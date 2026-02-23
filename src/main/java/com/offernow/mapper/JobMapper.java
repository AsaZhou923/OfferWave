package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位信息主表 Mapper 接口
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
