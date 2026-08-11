package com.offerwave.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.common.NotFoundException;
import com.offerwave.common.PrivilegeException;
import com.offerwave.dto.MyJobDto;
import com.offerwave.dto.UpdateJobStatusDto;
import com.offerwave.entity.Job;
import com.offerwave.entity.Membership;
import com.offerwave.entity.User;
import com.offerwave.entity.UserJobStatus;
import com.offerwave.mapper.JobMapper;
import com.offerwave.mapper.MembershipMapper;
import com.offerwave.mapper.UserJobStatusMapper;
import com.offerwave.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 职位追踪服务实现。
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    /** 用户-职位状态数据访问 */
    @Autowired
    private UserJobStatusMapper userJobStatusMapper;

    /** 用户数据访问 */
    @Autowired
    private UserMapper userMapper;

    /** 会员数据访问 */
    @Autowired
    private MembershipMapper membershipMapper;

    /** 职位数据访问 */
    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private ContentModerationService contentModerationService;

    @Autowired
    private MembershipAccessService membershipAccessService;

    @Override
    @Transactional
    public void updateJobStatus(Long userId, Long jobId, UpdateJobStatusDto dto) {
        validateUpdate(dto);

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new NotFoundException("职位不存在");
        }

        User lockedUser = userMapper.selectByIdForUpdate(userId);
        if (lockedUser == null) {
            throw new PrivilegeException("用户不存在，无法校验追踪权限");
        }

        LambdaQueryWrapper<UserJobStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserJobStatus::getUserId, userId).eq(UserJobStatus::getJobId, jobId);
        UserJobStatus status = userJobStatusMapper.selectOne(queryWrapper);

        // 不存在则创建状态，存在则更新状态
        if (status == null) {
            status = new UserJobStatus();
            status.setUserId(userId);
            status.setJobId(jobId);
            status.setIsCollected(dto.getIsCollected() != null && dto.getIsCollected());
            status.setDeliveryStatus(dto.getDeliveryStatus() != null ? dto.getDeliveryStatus() : 0);
            status.setUserNote(contentModerationService.sanitizeUserNote(userId, dto.getUserNote()));

            if (isTracked(status.getIsCollected(), status.getDeliveryStatus())) {
                checkUserPrivileges(lockedUser);
            }
            userJobStatusMapper.insert(status);
        } else {
            boolean wasTracked = isTracked(status.getIsCollected(), status.getDeliveryStatus());

            if (dto.getIsCollected() != null)
                status.setIsCollected(dto.getIsCollected());
            if (dto.getDeliveryStatus() != null)
                status.setDeliveryStatus(dto.getDeliveryStatus());
            if (dto.getUserNote() != null)
                status.setUserNote(contentModerationService.sanitizeUserNote(userId, dto.getUserNote()));

            boolean isTrackedNow = isTracked(status.getIsCollected(), status.getDeliveryStatus());
            if (!wasTracked && isTrackedNow) {
                checkUserPrivileges(lockedUser);
            }
            userJobStatusMapper.updateById(status);
        }
    }

    @Override
    public Page<MyJobDto> getMyJobs(Long userId, String type, Page<?> page) {
        LambdaQueryWrapper<UserJobStatus> statusWrapper = new LambdaQueryWrapper<>();
        statusWrapper.eq(UserJobStatus::getUserId, userId);

        if ("collected".equals(type)) {
            statusWrapper.eq(UserJobStatus::getIsCollected, true);
        } else if ("delivered".equals(type)) {
            statusWrapper.gt(UserJobStatus::getDeliveryStatus, 0);
        } else {
            // type 非法，返回空分页
            return new Page<>();
        }
        statusWrapper.orderByDesc(UserJobStatus::getUpdatedAt);

        Page<UserJobStatus> statusQueryPage = new Page<>(page.getCurrent(), page.getSize());
        Page<UserJobStatus> statusPage = userJobStatusMapper.selectPage(statusQueryPage, statusWrapper);
        List<Long> jobIds = statusPage.getRecords().stream().map(UserJobStatus::getJobId).collect(Collectors.toList());

        Map<Long, UserJobStatus> statusMap = statusPage.getRecords().stream()
                .collect(Collectors.toMap(UserJobStatus::getJobId, s -> s, (oldVal, newVal) -> oldVal));

        Page<MyJobDto> jobPage = new Page<>(page.getCurrent(), page.getSize(), statusPage.getTotal());
        if (jobIds.isEmpty()) {
            return jobPage;
        }

        // 批量查询职位并按状态分页结果原顺序回填
        List<Job> jobs = jobMapper.selectBatchIds(jobIds);
        Map<Long, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j));
        List<MyJobDto> sortedJobs = jobIds.stream()
                .map(jobMap::get)
                .filter(Objects::nonNull)
                .map(job -> {
                    MyJobDto dto = new MyJobDto();
                    BeanUtils.copyProperties(job, dto);
                    UserJobStatus status = statusMap.get(job.getId());
                    dto.setDeliveryStatus(status == null || status.getDeliveryStatus() == null ? 0 : status.getDeliveryStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        jobPage.setRecords(sortedJobs);
        jobPage.setTotal(statusPage.getTotal());

        return jobPage;
    }


    private boolean isTracked(Boolean isCollected, Integer deliveryStatus) {
        return Boolean.TRUE.equals(isCollected) || (deliveryStatus != null && deliveryStatus > 0);
    }

    private void validateUpdate(UpdateJobStatusDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("职位状态参数不能为空");
        }
        Integer deliveryStatus = dto.getDeliveryStatus();
        if (deliveryStatus != null && (deliveryStatus < 0 || deliveryStatus > 5)) {
            throw new IllegalArgumentException("投递状态码必须在 0 到 5 之间");
        }
        if (dto.getUserNote() != null && dto.getUserNote().length() > 200) {
            throw new IllegalArgumentException("用户备注最多 200 字");
        }
    }

    /**
     * 检查用户是否超过会员可追踪职位数量上限。
     */
    private void checkUserPrivileges(User lockedUser) {
        User user = membershipAccessService.ensureMembershipActive(lockedUser);
        if (user == null || user.getId() == null || user.getMembershipId() == null) {
            throw unavailableQuotaConfiguration();
        }

        Membership membership = membershipMapper.selectById(user.getMembershipId());
        if (membership == null) {
            throw unavailableQuotaConfiguration();
        }

        if (user.getCustomTrackLimit() != null) {
            if (user.getCustomTrackLimit() == -1) {
                return;
            }
            if (user.getCustomTrackLimit() < 0) {
                throw unavailableQuotaConfiguration();
            }
            long currentCount = countTrackedJobs(user.getId());
            if (currentCount >= user.getCustomTrackLimit()) {
                throw new PrivilegeException("当前账号追踪额度已达上限：" + user.getCustomTrackLimit());
            }
            return;
        }

        if (!StringUtils.hasText(membership.getPrivileges())) {
            throw unavailableQuotaConfiguration();
        }

        final Integer maxTrack;
        try {
            JSONObject privileges = JSONUtil.parseObj(membership.getPrivileges());
            maxTrack = privileges.getInt("max_job_track");
        } catch (RuntimeException ex) {
            throw unavailableQuotaConfiguration();
        }

        if (maxTrack == null) {
            throw unavailableQuotaConfiguration();
        }

        if (maxTrack == -1 || maxTrack == 999)
            return;

        if (maxTrack < 0) {
            throw unavailableQuotaConfiguration();
        }

        long currentCount = countTrackedJobs(user.getId());

        if (currentCount >= maxTrack) {
            throw new PrivilegeException("普通用户只能追踪" + maxTrack + "个职位，请升级 VIP 解锁无限权益");
        }
    }

    private long countTrackedJobs(Long userId) {
        LambdaQueryWrapper<UserJobStatus> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserJobStatus::getUserId, userId);
        countWrapper.and(w -> w.eq(UserJobStatus::getIsCollected, true).or().gt(UserJobStatus::getDeliveryStatus, 0));
        return userJobStatusMapper.selectCount(countWrapper);
    }

    private PrivilegeException unavailableQuotaConfiguration() {
        return new PrivilegeException("会员追踪额度配置不可用，请联系管理员");
    }
}
