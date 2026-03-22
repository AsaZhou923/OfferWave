package com.offerwave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offerwave.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位表 Mapper。
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
