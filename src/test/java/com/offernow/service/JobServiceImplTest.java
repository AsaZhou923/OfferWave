package com.offernow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.entity.Job;
import com.offernow.mapper.JobMapper;
import com.offernow.mapper.UserJobStatusMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private UserJobStatusMapper userJobStatusMapper;

    @Mock
    private MembershipAccessService membershipAccessService;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void shouldDiversifyCompaniesInFirstPageWithinTop30Window() {
        List<Job> records = new ArrayList<>();
        records.addAll(buildJobs("A公司", 1, 10));
        records.addAll(buildJobs("B公司", 11, 10));
        records.addAll(buildJobs("C公司", 21, 10));
        records.addAll(buildJobs("D公司", 31, 5));

        Page<Job> preloadResult = new Page<>(1, 50, records.size());
        preloadResult.setRecords(records);

        when(jobMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(preloadResult);

        Page<Job> result = jobService.searchJobs(new Page<>(1, 20), null, null, null,
                null, null, null, "newest");

        List<String> firstSixCompanies = result.getRecords().subList(0, 6).stream()
                .map(Job::getCompanyName)
                .collect(Collectors.toList());

        assertEquals(List.of("A公司", "B公司", "C公司", "A公司", "B公司", "C公司"), firstSixCompanies);
        assertEquals(20, result.getRecords().size());
        assertTrue(result.getRecords().stream().map(Job::getCompanyName).distinct().count() >= 3);
    }

    private List<Job> buildJobs(String companyName, long startId, int size) {
        return IntStream.range(0, size).mapToObj(i -> {
            Job job = new Job();
            job.setId(startId + i);
            job.setCompanyName(companyName);
            job.setAuditStatus(1);
            return job;
        }).collect(Collectors.toList());
    }
}
