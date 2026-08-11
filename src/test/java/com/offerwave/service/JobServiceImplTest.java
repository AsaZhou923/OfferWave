package com.offerwave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.entity.Job;
import com.offerwave.entity.User;
import com.offerwave.mapper.JobMapper;
import com.offerwave.mapper.UserJobStatusMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Job.class);
    }

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
    void shouldSelectNewestWindowBeforeApplyingCompanyDiversity() {
        mockAuthenticatedMember();
        List<Job> sortedWindow = List.of(
                buildJob(100, "Acme"),
                buildJob(99, "Acme"),
                buildJob(98, "Beta"),
                buildJob(97, "Acme"),
                buildJob(96, "Gamma"));

        when(jobMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(100L);
        when(jobMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(sortedWindow);

        Page<Job> result = jobService.searchJobs(new Page<>(1, 5), null, null, null,
                null, null, null, "newest");

        assertEquals(List.of(100L, 98L, 96L, 99L, 97L), extractIds(result, 5));
        assertEquals(5, result.getRecords().size());
        assertEquals(100L, result.getTotal());

        ArgumentCaptor<LambdaQueryWrapper<Job>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(jobMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("updated_at DESC"), sqlSegment);
        assertTrue(sqlSegment.contains("LIMIT 30"), sqlSegment);
    }

    @Test
    void shouldLimitAnonymousUsersToThirtyJobsFromRequestedSort() {
        List<Job> sortedWindow = buildJobs(60, 31);

        when(jobMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(100L);
        when(jobMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(sortedWindow);

        Page<Job> result = jobService.searchJobs(new Page<>(1, 30), null, null, null,
                null, null, null, "salary_desc");

        assertEquals(List.of(60L, 59L, 58L, 57L, 56L), extractIds(result, 5));
        assertEquals(30, result.getRecords().size());
        assertEquals(30L, result.getTotal());

        ArgumentCaptor<LambdaQueryWrapper<Job>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(jobMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("salary_max DESC"), sqlSegment);
    }

    private Job buildJob(long id, String companyName) {
        Job job = new Job();
        job.setId(id);
        job.setCompanyName(companyName);
        job.setAuditStatus(1);
        return job;
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
