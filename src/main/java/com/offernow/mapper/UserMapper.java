package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username);
}
