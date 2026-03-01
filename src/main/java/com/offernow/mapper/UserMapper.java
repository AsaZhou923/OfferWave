package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户。
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(String email);
}
