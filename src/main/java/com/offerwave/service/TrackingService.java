package com.offerwave.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.dto.MyJobDto;
import com.offerwave.dto.UpdateJobStatusDto;

/**
 * 职位追踪服务接口。
 */
public interface TrackingService {

    /**
     * 更新用户对职位的追踪状态。
     *
     * @param userId 用户 ID
     * @param jobId 职位 ID
     * @param dto 状态更新参数
     */
    void updateJobStatus(Long userId, Long jobId, UpdateJobStatusDto dto);

    /**
     * 获取用户“收藏”或“已投递”职位列表。
     *
     * @param userId 用户 ID
     * @param type 类型：collected 或 delivered
     * @param page 分页对象
     * @return 分页后的职位列表
     */
    Page<MyJobDto> getMyJobs(Long userId, String type, Page<?> page);
}
