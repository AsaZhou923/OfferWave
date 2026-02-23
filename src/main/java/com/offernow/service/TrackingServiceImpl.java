package com.offernow.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.common.PrivilegeException;
import com.offernow.dto.UpdateJobStatusDto;
import com.offernow.entity.Job;
import com.offernow.entity.Membership;
import com.offernow.entity.User;
import com.offernow.entity.UserJobStatus;
import com.offernow.mapper.JobMapper;
import com.offernow.mapper.MembershipMapper;
import com.offernow.mapper.UserMapper;
import com.offernow.mapper.UserJobStatusMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrackingServiceImpl implements TrackingService {

    @Autowired
    private UserJobStatusMapper userJobStatusMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MembershipMapper membershipMapper;

    @Autowired
    private JobMapper jobMapper;

    @Override
    public void updateJobStatus(Long userId, Long jobId, UpdateJobStatusDto dto) {
        LambdaQueryWrapper<UserJobStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserJobStatus::getUserId, userId).eq(UserJobStatus::getJobId, jobId);
        UserJobStatus status = userJobStatusMapper.selectOne(queryWrapper);

        // 如果是新追踪一个职位
        if (status == null) {
            checkUserPrivileges(userId); // 检查权益
            status = new UserJobStatus();
            status.setUserId(userId);
            status.setJobId(jobId);
            status.setIsCollected(dto.getIsCollected() != null && dto.getIsCollected());
            status.setDeliveryStatus(dto.getDeliveryStatus() != null ? dto.getDeliveryStatus() : 0);
            status.setUserNote(dto.getUserNote());
            userJobStatusMapper.insert(status);
        } else {
            // 更新现有状态
            if (dto.getIsCollected() != null)
                status.setIsCollected(dto.getIsCollected());
            if (dto.getDeliveryStatus() != null)
                status.setDeliveryStatus(dto.getDeliveryStatus());
            if (dto.getUserNote() != null)
                status.setUserNote(dto.getUserNote());
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
            // 如果类型无效，返回空分页
            return new Page<>();
        }
        statusWrapper.orderByDesc(UserJobStatus::getUpdatedAt);

        Page<UserJobStatus> statusQueryPage = new Page<>(page.getCurrent(), page.getSize());
        Page<UserJobStatus> statusPage = userJobStatusMapper.selectPage(statusQueryPage, statusWrapper);
        List<Long> jobIds = statusPage.getRecords().stream().map(UserJobStatus::getJobId).collect(Collectors.toList());

        Page<Job> jobPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (jobIds.isEmpty()) {
            return jobPage; // 返回空的职位分页
        }

        List<Job> jobs = jobMapper.selectBatchIds(jobIds);
        // 按原 jobIds 的顺序排序
        Map<Long, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j));
        List<Job> sortedJobs = jobIds.stream().map(jobMap::get).collect(Collectors.toList());

        jobPage.setRecords(sortedJobs);
        jobPage.setTotal(statusPage.getTotal());

        return jobPage;
    }

    private void checkUserPrivileges(Long userId) {
        User user = userMapper.selectById(userId);
        Membership membership = membershipMapper.selectById(user.getMembershipId());

        if (membership.getPrivileges() == null)
            return; // 无权益配置，不限制

        JSONObject privileges = JSONUtil.parseObj(membership.getPrivileges());
        Integer maxTrack = privileges.getInt("max_job_track");

        if (maxTrack == null || maxTrack == -1 || maxTrack == 999)
            return; // -1 或 999 代表无限

        LambdaQueryWrapper<UserJobStatus> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserJobStatus::getUserId, userId);
        countWrapper.and(w -> w.eq(UserJobStatus::getIsCollected, true).or().gt(UserJobStatus::getDeliveryStatus, 0));
        long currentCount = userJobStatusMapper.selectCount(countWrapper);

        if (currentCount >= maxTrack) {
            throw new PrivilegeException("普通用户只能追踪 " + maxTrack + " 个职位，请升级 VIP 解锁无限权益");
        }
    }
}
