package com.offerwave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.entity.Job;
import com.offerwave.entity.User;
import com.offerwave.mapper.JobMapper;
import com.offerwave.mapper.UserJobStatusMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUseJobIdsForFirstThirtyJobs() {
        mockAuthenticatedMember();
        List<Job> priorityWindowJobs = buildAscendingJobs(1, 30);

        when(jobMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(100L);
        when(jobMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(priorityWindowJobs);

        Page<Job> result = jobService.searchJobs(new Page<>(1, 20), null, null, null,
                null, null, null, "newest");

        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), extractIds(result, 5));
        assertEquals(20, result.getRecords().size());
        assertEquals(100L, result.getTotal());
    }

    @Test
    void shouldFillBeyondFirstThirtyWithRegularSortedJobsWithoutDuplicates() {
        mockAuthenticatedMember();
        List<Job> priorityWindowJobs = buildAscendingJobs(1, 30);
        List<Job> regularJobs = buildJobs(60, 51);

        when(jobMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(60L);
        when(jobMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(priorityWindowJobs)
                .thenReturn(regularJobs);

        Page<Job> result = jobService.searchJobs(new Page<>(2, 20), null, null, null,
                null, null, null, "newest");

        assertEquals(List.of(21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L), extractIds(result, 10));
        assertEquals(List.of(60L, 59L, 58L, 57L, 56L, 55L, 54L, 53L, 52L, 51L), extractIds(result, 10, 20));
        assertEquals(20, result.getRecords().size());
        assertEquals(60L, result.getTotal());
    }

    private List<Job> buildJobs(long startInclusive, long endInclusive) {
        return LongStream.iterate(startInclusive, id -> id - 1)
                .limit(startInclusive - endInclusive + 1)
                .mapToObj(id -> {
                    Job job = new Job();
                    job.setId(id);
                    job.setCompanyName("Company-" + id);
                    job.setAuditStatus(1);
                    return job;
                })
                .collect(Collectors.toList());
    }

    private List<Job> buildAscendingJobs(long startInclusive, long endInclusive) {
        return LongStream.rangeClosed(startInclusive, endInclusive)
                .mapToObj(id -> {
                    Job job = new Job();
                    job.setId(id);
                    job.setCompanyName("Company-" + id);
                    job.setAuditStatus(1);
                    return job;
                })
                .collect(Collectors.toList());
    }

    private List<Long> extractIds(Page<Job> page, int endExclusive) {
        return extractIds(page, 0, endExclusive);
    }

    private List<Long> extractIds(Page<Job> page, int startInclusive, int endExclusive) {
        return page.getRecords().subList(startInclusive, endExclusive).stream()
                .map(Job::getId)
                .collect(Collectors.toList());
    }

    private void mockAuthenticatedMember() {
        User user = new User();
        user.setId(1L);
        user.setMembershipId(2);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(membershipAccessService.ensureMembershipActive(any(User.class))).thenReturn(user);
    }
}
