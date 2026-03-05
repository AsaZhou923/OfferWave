package com.offernow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.dto.JobDetailDto;
import com.offernow.entity.Job;
import com.offernow.entity.User;
import com.offernow.entity.UserJobStatus;
import com.offernow.mapper.JobMapper;
import com.offernow.mapper.UserJobStatusMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Job query service implementation.
 */
@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private UserJobStatusMapper userJobStatusMapper;

    @Autowired
    private MembershipAccessService membershipAccessService;

    private static final int JOB_LIST_LIMIT = 30;

    private static final Map<Integer, String> DELIVERY_STATUS_MAP = Map.of(
            0, "未投递",
            1, "已投递",
            2, "笔试中",
            3, "面试中",
            4, "已录用(Offer)",
            5, "流程结束(挂)");

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }
        }
        return null;
    }

    private boolean shouldLimitJobList(User currentUser) {
        if (currentUser == null) {
            return true;
        }
        User effectiveUser = membershipAccessService.ensureMembershipActive(currentUser);
        return effectiveUser.getMembershipId() == null || effectiveUser.getMembershipId() <= 1;
    }

    @Override
    public Page<Job> searchJobs(Page<Job> page, String keyword, String city, String industry, String recruitType, Integer salaryMin,
                                String education, String sort) {

        User currentUser = getCurrentUser();
        boolean isLimitedUser = shouldLimitJobList(currentUser);

        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Job::getAuditStatus, 1);
        wrapper.and(StringUtils.isNotBlank(keyword),
                w -> w.like(Job::getCompanyName, keyword).or().like(Job::getJobTitle, keyword));
        wrapper.like(StringUtils.isNotBlank(city), Job::getCity, city);
        wrapper.like(StringUtils.isNotBlank(industry), Job::getCompanyBusiness, industry);
        wrapper.eq(StringUtils.isNotBlank(recruitType), Job::getRecruitType, recruitType);
        wrapper.ge(salaryMin != null, Job::getSalaryMin, salaryMin);
        wrapper.eq(StringUtils.isNotBlank(education), Job::getEducation, education);

        if ("deadline".equals(sort)) {
            wrapper.orderByAsc(Job::getDeadline);
        } else if ("salary_desc".equals(sort)) {
            wrapper.orderByDesc(Job::getSalaryMax);
        } else {
            wrapper.orderByDesc(Job::getUpdatedAt);
        }

        Page<Job> resultPage = jobMapper.selectPage(page, wrapper);

        if (isLimitedUser) {
            long offset = page.offset();
            resultPage.setTotal(Math.min(resultPage.getTotal(), JOB_LIST_LIMIT));

            if (offset >= JOB_LIST_LIMIT) {
                resultPage.setRecords(Collections.emptyList());
            } else {
                int remain = (int) (JOB_LIST_LIMIT - offset);
                if (resultPage.getRecords().size() > remain) {
                    resultPage.setRecords(resultPage.getRecords().subList(0, remain));
                }
            }
        }

        LocalDate today = LocalDate.now();
        resultPage.getRecords().forEach(job -> {
            job.setIndustry(job.getCompanyBusiness());
            job.setIsUrgent(false);
            job.setIsCollected(false);
            job.setDeliveryStatus(0);
            job.setDeliveryStatusStr(DELIVERY_STATUS_MAP.get(0));
            if (StringUtils.isNotBlank(job.getDeadline())) {
                try {
                    LocalDate deadlineDate = LocalDate.parse(job.getDeadline());
                    long daysUntil = ChronoUnit.DAYS.between(today, deadlineDate);
                    if (daysUntil >= 0 && daysUntil <= 7) {
                        job.setIsUrgent(true);
                    }
                } catch (DateTimeParseException ignored) {
                    // Ignore non-standard date format.
                }
            }
        });

        if (currentUser != null && !resultPage.getRecords().isEmpty()) {
            List<Long> jobIds = resultPage.getRecords().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<UserJobStatus> statusWrapper = new LambdaQueryWrapper<>();
            statusWrapper.eq(UserJobStatus::getUserId, currentUser.getId())
                    .in(UserJobStatus::getJobId, jobIds);

            Map<Long, UserJobStatus> statusByJobId = userJobStatusMapper.selectList(statusWrapper).stream()
                    .collect(Collectors.toMap(UserJobStatus::getJobId, Function.identity(), (a, b) -> b));

            resultPage.getRecords().forEach(job -> {
                UserJobStatus status = statusByJobId.get(job.getId());
                if (status != null) {
                    job.setIsCollected(Boolean.TRUE.equals(status.getIsCollected()));
                    Integer deliveryStatus = status.getDeliveryStatus() == null ? 0 : status.getDeliveryStatus();
                    job.setDeliveryStatus(deliveryStatus);
                    job.setDeliveryStatusStr(DELIVERY_STATUS_MAP.getOrDefault(deliveryStatus, "未知状态"));
                }
            });
        }

        return resultPage;
    }

    @Override
    public JobDetailDto getJobDetail(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null || job.getAuditStatus() != 1) {
            return null;
        }

        JobDetailDto jobDetailDto = new JobDetailDto();
        BeanUtils.copyProperties(job, jobDetailDto);

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            JobDetailDto.MyStatus myStatus = new JobDetailDto.MyStatus();
            myStatus.setCollected(false);
            myStatus.setDeliveryStatus(0);
            myStatus.setDeliveryStatusStr(DELIVERY_STATUS_MAP.get(0));

            Long userId = currentUser.getId();
            LambdaQueryWrapper<UserJobStatus> statusWrapper = new LambdaQueryWrapper<>();
            statusWrapper.eq(UserJobStatus::getUserId, userId)
                    .eq(UserJobStatus::getJobId, jobId);
            UserJobStatus status = userJobStatusMapper.selectOne(statusWrapper);

            if (status != null) {
                myStatus.setCollected(status.getIsCollected());
                myStatus.setDeliveryStatus(status.getDeliveryStatus());
                myStatus.setDeliveryStatusStr(
                        DELIVERY_STATUS_MAP.getOrDefault(status.getDeliveryStatus(), "未知状态"));
                myStatus.setUserNote(status.getUserNote());
            }
            jobDetailDto.setMyStatus(myStatus);
        }

        return jobDetailDto;
    }
}

