package com.offernow.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offernow.dto.RegisterDto;
import com.offernow.dto.UsernamePasswordLoginDto;
import com.offernow.entity.User;
import com.offernow.mapper.UserMapper;
import com.offernow.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> login(UsernamePasswordLoginDto loginDto) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectByUsername(loginDto.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 更新最后登录时间
        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);

        // 4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId().toString());

        // 5. 组装返回结果
        Map<String, Object> responseData = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", null);
        userInfo.put("membership_level", user.getMembershipId());
        userInfo.put("is_vip", user.getMembershipId() > 1);

        responseData.put("token", token);
        responseData.put("user", userInfo);
        responseData.put("is_new_user", false); // 对于登录而言，永远不是新用户

        return responseData;
    }

    @Override
    public void register(RegisterDto registerDto) {
        // 1. 检查用户名是否已存在
        if (userMapper.selectByUsername(registerDto.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 创建新用户
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        user.setNickname("User_" + RandomUtil.randomString(6));
        user.setMembershipId(1); // 默认为普通用户
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());

        // 3. 插入数据库
        userMapper.insert(user);
    }
}
