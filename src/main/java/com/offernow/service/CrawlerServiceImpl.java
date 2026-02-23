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

@Service
public class CrawlerServiceImpl implements CrawlerService {

    @Autowired
    private JobMapper jobMapper;

    @Override
    @Transactional // 保证整个同步操作的原子性
    public Map<String, Integer> syncJobs(CrawlerSyncDto syncDto) {
        int receivedCount = syncDto.getItems().size();
        int insertedCount = 0;
        int updatedCount = 0;

        // 1. 一次性查询出所有已存在的 jobs
        List<String> hashes = syncDto.getItems().stream()
                                     .map(CrawlerJobItemDto::getUniqueHash)
                                     .collect(Collectors.toList());
        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Job::getUniqueHash, hashes);
        List<Job> existingJobs = jobMapper.selectList(queryWrapper);
        Map<String, Job> existingJobsMap = existingJobs.stream()
                                                       .collect(Collectors.toMap(Job::getUniqueHash, job -> job));

        // 2. 遍历上报数据，进行更新或插入
        for (CrawlerJobItemDto itemDto : syncDto.getItems()) {
            Job existingJob = existingJobsMap.get(itemDto.getUniqueHash());
            if (existingJob != null) {
                // 更新
                Job jobToUpdate = new Job();
                jobToUpdate.setId(existingJob.getId());
                // 只更新可能变化的字段
                jobToUpdate.setProcessStage(itemDto.getProcessStage());
                jobToUpdate.setDeadline(itemDto.getDeadline());
                jobToUpdate.setSalaryRange(itemDto.getSalaryRange());
                jobToUpdate.setSalaryMin(itemDto.getSalaryMin());
                jobToUpdate.setSalaryMax(itemDto.getSalaryMax());
                // 其他字段...
                jobMapper.updateById(jobToUpdate);
                updatedCount++;
            } else {
                // 插入
                Job newJob = new Job();
                BeanUtils.copyProperties(itemDto, newJob);
                newJob.setAuditStatus(0); // 新增数据默认为待审核
                jobMapper.insert(newJob);
                insertedCount++;
            }
        }

        // 3. 组装返回结果
        Map<String, Integer> result = new HashMap<>();
        result.put("received_count", receivedCount);
        result.put("inserted_count", insertedCount);
        result.put("updated_count", updatedCount);

        return result;
    }
}
