package com.offernow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offernow.dto.CrawlerJobItemDto;
import com.offernow.dto.CrawlerSyncDto;
import com.offernow.entity.Job;
import com.offernow.mapper.JobMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public Map<String, Integer> syncJobs(CrawlerSyncDto syncDto) {
        int receivedCount = syncDto.getItems().size();
        int insertedCount = 0;
        int updatedCount = 0;

        // 1) 批量查询已存在职位（按 unique_hash）
        List<String> hashes = syncDto.getItems().stream()
                .map(CrawlerJobItemDto::getUniqueHash)
                .collect(Collectors.toList());
        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Job::getUniqueHash, hashes);
        List<Job> existingJobs = jobMapper.selectList(queryWrapper);
        Map<String, Job> existingJobsMap = existingJobs.stream()
                .collect(Collectors.toMap(Job::getUniqueHash, job -> job));

        // 2) 逐条同步：存在则更新，不存在则插入
        for (CrawlerJobItemDto itemDto : syncDto.getItems()) {
            Job existingJob = existingJobsMap.get(itemDto.getUniqueHash());
            if (existingJob != null) {
                Job jobToUpdate = new Job();
                jobToUpdate.setId(existingJob.getId());
                // 仅更新可能变化字段
                jobToUpdate.setProcessStage(itemDto.getProcessStage());
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

        // 3) 返回同步统计
        Map<String, Integer> result = new HashMap<>();
        result.put("received_count", receivedCount);
        result.put("inserted_count", insertedCount);
        result.put("updated_count", updatedCount);

        return result;
    }
}
