package com.offernow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.dto.JobDetailDto;
import com.offernow.entity.Job;

/**
 * 职位查询服务接口
 */
public interface JobService {

    /**
     * 获取职位列表 (搜索/筛选)
     * @param page 分页对象
     * @param keyword 关键词
     * @param city 城市
     * @param recruitType 招聘类型
     * @param salaryMin 最低薪资
     * @param education 学历
     * @param sort 排序字段
     * @return 分页后的职位列表
     */
    Page<Job> searchJobs(Page<Job> page, String keyword, String city, String recruitType, Integer salaryMin, String education, String sort);

    /**
     * 获取职位详情
     * @param jobId 职位ID
     * @return 职位详情 DTO
     */
    JobDetailDto getJobDetail(Long jobId);

}
