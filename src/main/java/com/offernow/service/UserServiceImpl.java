package com.offernow.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offernow.dto.UserPreferenceDto;
import com.offernow.entity.Membership;
import com.offernow.entity.User;
import com.offernow.entity.UserJobStatus;
import com.offernow.mapper.MembershipMapper;
import com.offernow.mapper.UserJobStatusMapper;
import com.offernow.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MembershipMapper membershipMapper;

    @Autowired
    private UserJobStatusMapper userJobStatusMapper;

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Membership membership = membershipMapper.selectById(user.getMembershipId());
        if (membership == null) {
            throw new RuntimeException("会员信息配置错误");
        }

        // 1. 组装用户信息和偏好
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("nickname", user.getNickname());
        response.put("avatar", null); // 暂时为 null

        Map<String, Object> preferences = new HashMap<>();
        // 将逗号分隔的字符串转为列表
        if (user.getPrefIndustry() != null && !user.getPrefIndustry().isEmpty()) {
            preferences.put("industry", Arrays.asList(user.getPrefIndustry().split(",")));
        } else {
            preferences.put("industry", List.of());
        }
        preferences.put("city", user.getPrefCity());
        preferences.put("job", user.getPrefJob());
        response.put("preferences", preferences);

        // 2. 组装会员信息和权益
        Map<String, Object> membershipInfo = new HashMap<>();
        membershipInfo.put("id", membership.getId());
        membershipInfo.put("level_name", membership.getLevelName());
        membershipInfo.put("expire_date", null); // 永久有效
        // 解析JSON权益
        if (membership.getPrivileges() != null) {
            JSONObject privilegesJson = JSONUtil.parseObj(membership.getPrivileges());
            membershipInfo.put("privileges", privilegesJson);
        }
        response.put("membership", membershipInfo);

        // 3. 组装统计数据
        Map<String, Object> stats = new HashMap<>();
        LambdaQueryWrapper<UserJobStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserJobStatus::getUserId, userId);

        // 统计已追踪（收藏或投递）
        queryWrapper.and(wrapper -> wrapper.eq(UserJobStatus::getIsCollected, true)
                .or()
                .gt(UserJobStatus::getDeliveryStatus, 0));
        stats.put("tracked_count", userJobStatusMapper.selectCount(queryWrapper));

        // 统计已投递
        LambdaQueryWrapper<UserJobStatus> deliveredWrapper = new LambdaQueryWrapper<>();
        deliveredWrapper.eq(UserJobStatus::getUserId, userId).gt(UserJobStatus::getDeliveryStatus, 0);
        stats.put("delivered_count", userJobStatusMapper.selectCount(deliveredWrapper));
        response.put("stats", stats);

        return response;
    }

    @Override
    public boolean updatePreferences(Long userId, UserPreferenceDto preferenceDto) {
        User user = new User();
        user.setId(userId);
        user.setPrefIndustry(preferenceDto.getPrefIndustry());
        user.setPrefCity(preferenceDto.getPrefCity());
        user.setPrefJob(preferenceDto.getPrefJob());
        return userMapper.updateById(user) > 0;
    }

    @Override
    public List<Membership> listMemberships() {
        // 为了安全，不返回 privileges 字段给前端
        List<Membership> memberships = membershipMapper.selectList(null);
        memberships.forEach(m -> m.setPrivileges(null));
        return memberships;
    }

    @Override
    public Map<String, Object> upgradeMembership(Long userId, Integer targetLevelId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        Membership targetMembership = membershipMapper.selectById(targetLevelId);
        if (targetMembership == null) {
            throw new RuntimeException("目标会员等级不存在");
        }

        user.setMembershipId(targetLevelId);
        userMapper.updateById(user);

        Map<String, Object> response = new HashMap<>();
        response.put("current_level", targetMembership.getLevelName());

        String expireDate = "永久";
        if (targetMembership.getDurationDays() != -1) {
            expireDate = LocalDate.now().plusDays(targetMembership.getDurationDays())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        response.put("expire_date", expireDate);

        return response;
    }
}
