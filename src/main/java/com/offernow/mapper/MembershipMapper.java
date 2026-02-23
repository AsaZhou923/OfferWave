package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.Membership;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级表 Mapper。
 */
@Mapper
public interface MembershipMapper extends BaseMapper<Membership> {
}
