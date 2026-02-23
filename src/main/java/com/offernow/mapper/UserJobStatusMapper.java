package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.UserJobStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-职位状态表 Mapper。
 */
@Mapper
public interface UserJobStatusMapper extends BaseMapper<UserJobStatus> {
}
