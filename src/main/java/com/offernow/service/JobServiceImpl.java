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

/**
 * 职位查询服务实现。
 */
@Service
public class JobServiceImpl implements JobService {

    /** 职位数据访问 */
    @Autowired
    private JobMapper jobMapper;

    /** 用户职位状态数据访问 */
    @Autowired
    private UserJobStatusMapper userJobStatusMapper;

    /** 游客/普通用户在职位列表可见的最大条数 */
    private static final int JOB_LIST_LIMIT = 30;

    /** 投递状态码映射（用于详情页展示） */
    private static final Map<Integer, String> DELIVERY_STATUS_MAP = Map.of(
            0, "未投递",
            1, "已投递",
            2, "笔试中",
            3, "面试中",
            4, "已录用(Offer)",
            5, "流程结束(挂)");

    /**
     * 获取当前登录用户。
     */
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
     * 是否需要对职位列表应用 30 条上限。
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
        boolean isLimitedUser = shouldLimitJobList(currentUser);

        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();

        // 基础过滤：只返回审核通过数据
        wrapper.eq(Job::getAuditStatus, 1);
        wrapper.and(StringUtils.isNotBlank(keyword),
                w -> w.like(Job::getCompanyName, keyword).or().like(Job::getJobTitle, keyword));
        wrapper.like(StringUtils.isNotBlank(city), Job::getCity, city);
        wrapper.eq(StringUtils.isNotBlank(recruitType), Job::getRecruitType, recruitType);
        wrapper.ge(salaryMin != null, Job::getSalaryMin, salaryMin);
        wrapper.eq(StringUtils.isNotBlank(education), Job::getEducation, education);

        // 排序规则
        if ("deadline".equals(sort)) {
            wrapper.orderByAsc(Job::getDeadline);
        } else if ("salary_desc".equals(sort)) {
            wrapper.orderByDesc(Job::getSalaryMax);
        } else {
            wrapper.orderByDesc(Job::getUpdatedAt);
        }

        Page<Job> resultPage = jobMapper.selectPage(page, wrapper);

        // 游客/普通用户应用 30 条总量限制
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

        // 计算是否为“即将截止”岗位（7 天内）
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
                    // 忽略非标准日期格式
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

        // 已登录用户附带 my_status
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
                        DELIVERY_STATUS_MAP.getOrDefault(status.getDeliveryStatus(), "未知状态"));
                myStatus.setUserNote(status.getUserNote());
                jobDetailDto.setMyStatus(myStatus);
            }
        }

        return jobDetailDto;
    }
}
