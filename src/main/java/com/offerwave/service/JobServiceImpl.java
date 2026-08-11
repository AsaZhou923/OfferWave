package com.offerwave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.dto.JobDetailDto;
import com.offerwave.entity.Job;
import com.offerwave.entity.User;
import com.offerwave.entity.UserJobStatus;
import com.offerwave.mapper.JobMapper;
import com.offerwave.mapper.UserJobStatusMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private static final int DIVERSITY_WINDOW_SIZE = 30;

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

        long offset = page.offset();
        long total = jobMapper.selectCount(buildBaseJobQuery(keyword, city, industry, recruitType, salaryMin, education));
        List<Job> priorityWindowJobs = fetchPriorityWindowJobs(
                sort, keyword, city, industry, recruitType, salaryMin, education);
        List<Long> priorityWindowJobIds = priorityWindowJobs.stream().map(Job::getId).collect(Collectors.toList());
        int priorityWindowSize = priorityWindowJobs.size();

        Page<Job> resultPage;
        if (isLimitedUser) {
            resultPage = new Page<>(page.getCurrent(), page.getSize(), Math.min(total, JOB_LIST_LIMIT));
            resultPage.setRecords(slicePriorityWindow(priorityWindowJobs, offset, page.getSize()));
        } else {
            resultPage = new Page<>(page.getCurrent(), page.getSize(), total);
            resultPage.setRecords(fetchJobsWithPriorityWindow(page, sort, keyword, city, industry,
                    recruitType, salaryMin, education, priorityWindowJobs, priorityWindowJobIds, priorityWindowSize));
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

    private LambdaQueryWrapper<Job> buildBaseJobQuery(String keyword, String city, String industry,
                                                      String recruitType, Integer salaryMin, String education) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getAuditStatus, 1);
        wrapper.and(StringUtils.isNotBlank(keyword),
                w -> w.like(Job::getCompanyName, keyword).or().like(Job::getJobTitle, keyword));
        wrapper.like(StringUtils.isNotBlank(city), Job::getCity, city);
        wrapper.like(StringUtils.isNotBlank(industry), Job::getCompanyBusiness, industry);
        wrapper.eq(StringUtils.isNotBlank(recruitType), Job::getRecruitType, recruitType);
        wrapper.ge(salaryMin != null, Job::getSalaryMin, salaryMin);
        wrapper.eq(StringUtils.isNotBlank(education), Job::getEducation, education);
        return wrapper;
    }

    /**
     * 查询时先按用户要求选出排序窗口，再仅在窗口内部按公司轮询，
     * 避免“公司多样性”把窗口外的较旧职位提前。
     */
    private List<Job> fetchPriorityWindowJobs(String sort, String keyword, String city, String industry,
                                              String recruitType, Integer salaryMin, String education) {
        LambdaQueryWrapper<Job> wrapper = buildBaseJobQuery(keyword, city, industry, recruitType, salaryMin, education);
        applyRegularSort(wrapper, sort);
        wrapper.last("LIMIT " + DIVERSITY_WINDOW_SIZE);
        return diversifyByCompany(jobMapper.selectList(wrapper));
    }

    private List<Job> fetchJobsWithPriorityWindow(Page<Job> page, String sort, String keyword, String city,
                                                  String industry, String recruitType, Integer salaryMin,
                                                  String education, List<Job> priorityWindowJobs,
                                                  List<Long> priorityWindowJobIds, int priorityWindowSize) {
        long offset = page.offset();
        List<Job> result = new ArrayList<>();

        if (offset < priorityWindowSize) {
            result.addAll(slicePriorityWindow(priorityWindowJobs, offset, page.getSize()));
        }

        int remain = (int) page.getSize() - result.size();
        if (remain <= 0) {
            return result;
        }

        long regularOffset = Math.max(0, offset - priorityWindowSize);
        result.addAll(fetchRegularJobsExcludingPriorityWindow(regularOffset, remain, sort, keyword, city,
                industry, recruitType, salaryMin, education, priorityWindowJobIds));
        return result;
    }

    private List<Job> fetchRegularJobsExcludingPriorityWindow(long offset, int size, String sort, String keyword,
                                                              String city, String industry, String recruitType,
                                                              Integer salaryMin, String education,
                                                              List<Long> excludedJobIds) {
        if (size <= 0) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Job> wrapper = buildBaseJobQuery(keyword, city, industry, recruitType, salaryMin, education);
        wrapper.notIn(!excludedJobIds.isEmpty(), Job::getId, excludedJobIds);
        applyRegularSort(wrapper, sort);
        wrapper.last("LIMIT " + offset + ", " + size);
        return jobMapper.selectList(wrapper);
    }

    private void applyRegularSort(LambdaQueryWrapper<Job> wrapper, String sort) {
        if ("deadline".equals(sort)) {
            wrapper.orderByAsc(Job::getDeadline)
                    .orderByDesc(Job::getUpdatedAt)
                    .orderByDesc(Job::getId);
        } else if ("salary_desc".equals(sort)) {
            wrapper.orderByDesc(Job::getSalaryMax)
                    .orderByDesc(Job::getUpdatedAt)
                    .orderByDesc(Job::getId);
        } else {
            wrapper.orderByDesc(Job::getUpdatedAt)
                    .orderByDesc(Job::getId);
        }
    }

    private List<Job> diversifyByCompany(List<Job> sortedJobs) {
        if (sortedJobs == null || sortedJobs.size() < 2) {
            return sortedJobs == null ? Collections.emptyList() : new ArrayList<>(sortedJobs);
        }

        Map<String, Deque<Job>> jobsByCompany = new LinkedHashMap<>();
        for (Job job : sortedJobs) {
            String companyKey = job.getCompanyName() == null
                    ? "job:" + job.getId()
                    : job.getCompanyName().trim().toLowerCase(Locale.ROOT);
            jobsByCompany.computeIfAbsent(companyKey, ignored -> new ArrayDeque<>()).add(job);
        }

        List<Job> diversified = new ArrayList<>(sortedJobs.size());
        boolean added;
        do {
            added = false;
            for (Deque<Job> companyJobs : jobsByCompany.values()) {
                Job next = companyJobs.pollFirst();
                if (next != null) {
                    diversified.add(next);
                    added = true;
                }
            }
        } while (added);
        return diversified;
    }

    private List<Job> slicePriorityWindow(List<Job> priorityWindowJobs, long offset, long size) {
        if (offset >= priorityWindowJobs.size() || size <= 0) {
            return Collections.emptyList();
        }

        int start = (int) offset;
        int end = Math.min(start + (int) size, priorityWindowJobs.size());
        return new ArrayList<>(priorityWindowJobs.subList(start, end));
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

    @Override
    public long countPublicJobs() {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getAuditStatus, 1);
        return jobMapper.selectCount(wrapper);
    }
}
