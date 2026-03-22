package com.offerwave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerwave.dto.CrawlerJobItemDto;
import com.offerwave.dto.CrawlerSyncDto;
import com.offerwave.entity.CrawlerSyncError;
import com.offerwave.entity.CrawlerSyncLog;
import com.offerwave.entity.Job;
import com.offerwave.entity.User;
import com.offerwave.mapper.CrawlerSyncErrorMapper;
import com.offerwave.mapper.CrawlerSyncLogMapper;
import com.offerwave.mapper.JobMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 爬虫数据同步服务实现。
 */
@Service
public class CrawlerServiceImpl implements CrawlerService {

    /** 职位数据访问 */
    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private CrawlerSyncLogMapper crawlerSyncLogMapper;

    @Autowired
    private CrawlerSyncErrorMapper crawlerSyncErrorMapper;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Integer> syncJobs(CrawlerSyncDto syncDto) {
        int receivedCount = syncDto.getItems().size();
        int insertedCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        CrawlerSyncLog syncLog = new CrawlerSyncLog();
        syncLog.setBatchId(syncDto.getBatchId());
        syncLog.setReceivedCount(receivedCount);
        syncLog.setInsertedCount(0);
        syncLog.setUpdatedCount(0);
        syncLog.setFailedCount(0);
        syncLog.setStatus(1);
        syncLog.setOperatorUserId(getCurrentUserId());
        syncLog.setCreatedAt(LocalDateTime.now());

        try {
            List<CrawlerJobItemDto> validItems = new ArrayList<>();
            for (CrawlerJobItemDto item : syncDto.getItems()) {
                if (!StringUtils.hasText(item.getCompanyName())
                        || !StringUtils.hasText(item.getCompanyType())
                        || !StringUtils.hasText(item.getJobTitle())
                        || !StringUtils.hasText(item.getCity())
                        || !StringUtils.hasText(item.getRecruitType())) {
                    failedCount++;
                    CrawlerSyncError error = new CrawlerSyncError();
                    error.setBatchId(syncDto.getBatchId());
                    error.setUniqueHash(item.getUniqueHash());
                    error.setPayload(item.toString());
                    error.setErrorMessage("missing required fields");
                    error.setCreatedAt(LocalDateTime.now());
                    crawlerSyncErrorMapper.insert(error);
                    continue;
                }
                if (!StringUtils.hasText(item.getUniqueHash())) {
                    String raw = item.getCompanyName() + "_" + item.getJobTitle() + "_" + item.getCity();
                    item.setUniqueHash(DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)));
                }
                validItems.add(item);
            }

            // 0) 预处理：去除批次内的重复数据（保留最后一条），防止插入时主键冲突
            Map<String, CrawlerJobItemDto> distinctItems = validItems.stream()
                    .collect(Collectors.toMap(CrawlerJobItemDto::getUniqueHash, item -> item, (oldVal, newVal) -> newVal));

            if (!distinctItems.isEmpty()) {
                // 1) 批量查询已存在职位（按 unique_hash）
                List<String> hashes = distinctItems.values().stream()
                        .map(CrawlerJobItemDto::getUniqueHash)
                        .collect(Collectors.toList());
                LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.in(Job::getUniqueHash, hashes);
                List<Job> existingJobs = jobMapper.selectList(queryWrapper);
                Map<String, Job> existingJobsMap = existingJobs.stream()
                        .collect(Collectors.toMap(Job::getUniqueHash, job -> job));

                // 2) 逐条同步：存在则更新，不存在则插入
                for (CrawlerJobItemDto itemDto : distinctItems.values()) {
                    Job existingJob = existingJobsMap.get(itemDto.getUniqueHash());
                    if (existingJob != null) {
                        Job jobToUpdate = new Job();
                        jobToUpdate.setId(existingJob.getId());
                        // 仅更新可能变化字段
                        jobToUpdate.setProcessStage(itemDto.getProcessStage());
                        jobToUpdate.setTargetAudience(itemDto.getTargetAudience());
                        jobToUpdate.setDeadline(itemDto.getDeadline());
                        jobToUpdate.setSalaryRange(itemDto.getSalaryRange());
                        jobToUpdate.setSalaryMin(itemDto.getSalaryMin());
                        jobToUpdate.setSalaryMax(itemDto.getSalaryMax());
                        jobMapper.updateById(jobToUpdate);
                        updatedCount++;
                    } else {
                        Job newJob = new Job();
                        BeanUtils.copyProperties(itemDto, newJob);
                        newJob.setAuditStatus(0); // 新数据默认待审核
                        jobMapper.insert(newJob);
                        insertedCount++;
                    }
                }
            }

            syncLog.setInsertedCount(insertedCount);
            syncLog.setUpdatedCount(updatedCount);
            syncLog.setFailedCount(failedCount);
            syncLog.setStatus(1);
            crawlerSyncLogMapper.insert(syncLog);

            // 3) 返回同步统计
            Map<String, Integer> result = new HashMap<>();
            result.put("received_count", receivedCount);
            result.put("inserted_count", insertedCount);
            result.put("updated_count", updatedCount);
            result.put("failed_count", failedCount);

            return result;
        } catch (Exception ex) {
            syncLog.setInsertedCount(insertedCount);
            syncLog.setUpdatedCount(updatedCount);
            syncLog.setFailedCount(failedCount);
            syncLog.setStatus(0);
            syncLog.setErrorMessage(ex.getMessage());
            crawlerSyncLogMapper.insert(syncLog);
            throw ex;
        }
    }
}
