package com.offernow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.dto.UpdateJobStatusDto;
import com.offernow.entity.Job;

/**
 * 投递追踪服务接口
 */
public interface TrackingService {

    /**
     * 更新用户手动标记的某个职位的状态
     * @param userId 用户ID
     * @param jobId 职位ID
     * @param dto 状态更新数据
     */
    void updateJobStatus(Long userId, Long jobId, UpdateJobStatusDto dto);


    /**
     * 获取用户“收藏夹”或“投递进度表”的数据
     * @param userId 用户ID
     * @param type "collected" 或 "delivered"
     * @param page 分页对象
     * @return 分页后的职位列表
     */
    Page<Job> getMyJobs(Long userId, String type, Page<Job> page);

}
