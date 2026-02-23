package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.Membership;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级配置表 Mapper 接口
 */
@Mapper
public interface MembershipMapper extends BaseMapper<Membership> {
}
