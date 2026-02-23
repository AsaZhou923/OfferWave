package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位表 Mapper。
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
