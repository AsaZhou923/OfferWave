package com.offerwave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerwave.dto.CrawlerJobItemDto;
import com.offerwave.entity.Job;
import com.offerwave.mapper.JobMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Owns the all-or-nothing transaction for crawler job writes.
 */
@Service
public class CrawlerJobBatchWriter {

    @Autowired
    private JobMapper jobMapper;

    @Transactional
    public Result write(List<CrawlerJobItemDto> validItems) {
        if (validItems == null || validItems.isEmpty()) {
            return new Result(0, 0);
        }

        Map<String, CrawlerJobItemDto> distinctItems = validItems.stream()
                .collect(Collectors.toMap(
                        CrawlerJobItemDto::getUniqueHash,
                        Function.identity(),
                        (oldValue, newValue) -> newValue,
                        LinkedHashMap::new));

        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Job::getUniqueHash, distinctItems.keySet());
        Map<String, Job> existingJobs = jobMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(
                        Job::getUniqueHash,
                        Function.identity(),
                        (first, ignored) -> first));

        int insertedCount = 0;
        int updatedCount = 0;
        for (CrawlerJobItemDto item : distinctItems.values()) {
            Job existingJob = existingJobs.get(item.getUniqueHash());
            if (existingJob == null) {
                Job newJob = new Job();
                BeanUtils.copyProperties(item, newJob);
                newJob.setAuditStatus(0);
                insertedCount += jobMapper.insert(newJob);
                continue;
            }

            Job jobToUpdate = new Job();
            BeanUtils.copyProperties(item, jobToUpdate);
            jobToUpdate.setId(existingJob.getId());
            jobToUpdate.setUniqueHash(item.getUniqueHash());
            updatedCount += jobMapper.updateById(jobToUpdate);
        }
        return new Result(insertedCount, updatedCount);
    }

    public record Result(int insertedCount, int updatedCount) {
    }
}
