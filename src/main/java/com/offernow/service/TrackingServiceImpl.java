package com.offernow.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.common.NotFoundException;
import com.offernow.common.PrivilegeException;
import com.offernow.dto.UpdateJobStatusDto;
import com.offernow.entity.Job;
import com.offernow.entity.Membership;
import com.offernow.entity.User;
import com.offernow.entity.UserJobStatus;
import com.offernow.mapper.JobMapper;
import com.offernow.mapper.MembershipMapper;
import com.offernow.mapper.UserJobStatusMapper;
import com.offernow.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void updateJobStatus(Long userId, Long jobId, UpdateJobStatusDto dto) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new NotFoundException("职位不存在");
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
                checkUserPrivileges(userId);
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
                checkUserPrivileges(userId);
            }
            userJobStatusMapper.updateById(status);
        }
    }

    @Override
    public Page<Job> getMyJobs(Long userId, String type, Page<Job> page) {
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

        Page<Job> jobPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (jobIds.isEmpty()) {
            return jobPage;
        }

        // 批量查询职位并按状态分页结果原顺序回填
        List<Job> jobs = jobMapper.selectBatchIds(jobIds);
        Map<Long, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j));
        List<Job> sortedJobs = jobIds.stream()
                .map(jobMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        jobPage.setRecords(sortedJobs);
        jobPage.setTotal(statusPage.getTotal());

        return jobPage;
    }


    private boolean isTracked(Boolean isCollected, Integer deliveryStatus) {
        return Boolean.TRUE.equals(isCollected) || (deliveryStatus != null && deliveryStatus > 0);
    }

    /**
     * 检查用户是否超过会员可追踪职位数量上限。
     */
    private void checkUserPrivileges(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new PrivilegeException("用户不存在，无法校验追踪权限");
        }
        user = membershipAccessService.ensureMembershipActive(user);

        Membership membership = membershipMapper.selectById(user.getMembershipId());
        if (membership == null) {
            return; // 没有会员信息时按不限制处理
        }

        if (user.getCustomTrackLimit() != null) {
            if (user.getCustomTrackLimit() < 0) {
                return;
            }
            LambdaQueryWrapper<UserJobStatus> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(UserJobStatus::getUserId, userId);
            countWrapper.and(w -> w.eq(UserJobStatus::getIsCollected, true).or().gt(UserJobStatus::getDeliveryStatus, 0));
            long currentCount = userJobStatusMapper.selectCount(countWrapper);
            if (currentCount >= user.getCustomTrackLimit()) {
                throw new PrivilegeException("当前账号追踪额度已达上限：" + user.getCustomTrackLimit());
            }
            return;
        }

        if (membership.getPrivileges() == null)
            return;

        JSONObject privileges = JSONUtil.parseObj(membership.getPrivileges());
        Integer maxTrack = privileges.getInt("max_job_track");

        if (maxTrack == null || maxTrack == -1 || maxTrack == 999)
            return;

        LambdaQueryWrapper<UserJobStatus> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserJobStatus::getUserId, userId);
        countWrapper.and(w -> w.eq(UserJobStatus::getIsCollected, true).or().gt(UserJobStatus::getDeliveryStatus, 0));
        long currentCount = userJobStatusMapper.selectCount(countWrapper);

        if (currentCount >= maxTrack) {
            throw new PrivilegeException("普通用户只能追踪" + maxTrack + "个职位，请升级 VIP 解锁无限权益");
        }
    }
}
