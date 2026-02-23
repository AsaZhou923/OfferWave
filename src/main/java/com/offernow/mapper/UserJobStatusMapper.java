package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.UserJobStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-职位交互状态表 Mapper 接口
 */
@Mapper
public interface UserJobStatusMapper extends BaseMapper<UserJobStatus> {
}
