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
import java.util.Map;

@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private UserJobStatusMapper userJobStatusMapper;

    // Guest + normal users can view at most this many jobs in list APIs.
    private static final int JOB_LIST_LIMIT = 30;

    private static final Map<Integer, String> DELIVERY_STATUS_MAP = Map.of(
            0, "\u672a\u6295\u9012",
            1, "\u5df2\u6295\u9012",
            2, "\u7b14\u8bd5\u4e2d",
            3, "\u9762\u8bd5\u4e2d",
            4, "\u5df2\u5f55\u7528(Offer)",
            5, "\u6d41\u7a0b\u7ed3\u675f(\u6302)");

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

    /**
     * Whether this user should be limited by the 30-item list cap.
     * Rule:
     * 1) guest user => limited
     * 2) membershipId null/1 => limited (normal user)
     * 3) membershipId > 1 => unlimited (member/VIP)
     */
    private boolean shouldLimitJobList(User currentUser) {
        return currentUser == null
                || currentUser.getMembershipId() == null
                || currentUser.getMembershipId() <= 1;
    }

    @Override
    public Page<Job> searchJobs(Page<Job> page, String keyword, String city, String recruitType, Integer salaryMin,
                                String education, String sort) {

        User currentUser = getCurrentUser();
        // Guests and normal users are capped; members are unlimited.
        boolean isLimitedUser = shouldLimitJobList(currentUser);

        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Job::getAuditStatus, 1);
        wrapper.and(StringUtils.isNotBlank(keyword),
                w -> w.like(Job::getCompanyName, keyword).or().like(Job::getJobTitle, keyword));
        wrapper.like(StringUtils.isNotBlank(city), Job::getCity, city);
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
            // Keep total stable for frontend pagination.
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
            job.setIsUrgent(false);
            if (StringUtils.isNotBlank(job.getDeadline())) {
                try {
                    LocalDate deadlineDate = LocalDate.parse(job.getDeadline());
                    long daysUntil = ChronoUnit.DAYS.between(today, deadlineDate);
                    if (daysUntil >= 0 && daysUntil <= 7) {
                        job.setIsUrgent(true);
                    }
                } catch (DateTimeParseException ignored) {
                    // Ignore invalid date format in source data.
                }
            }
        });

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
            Long userId = currentUser.getId();
            LambdaQueryWrapper<UserJobStatus> statusWrapper = new LambdaQueryWrapper<>();
            statusWrapper.eq(UserJobStatus::getUserId, userId)
                    .eq(UserJobStatus::getJobId, jobId);
            UserJobStatus status = userJobStatusMapper.selectOne(statusWrapper);

            if (status != null) {
                JobDetailDto.MyStatus myStatus = new JobDetailDto.MyStatus();
                myStatus.setCollected(status.getIsCollected());
                myStatus.setDeliveryStatus(status.getDeliveryStatus());
                myStatus.setDeliveryStatusStr(
                        DELIVERY_STATUS_MAP.getOrDefault(status.getDeliveryStatus(), "\u672a\u77e5\u72b6\u6001"));
                myStatus.setUserNote(status.getUserNote());
                jobDetailDto.setMyStatus(myStatus);
            }
        }

        return jobDetailDto;
    }
}
