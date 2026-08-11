package com.offerwave.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerwave.dto.UserPreferenceDto;
import com.offerwave.entity.Membership;
import com.offerwave.entity.User;
import com.offerwave.entity.UserJobStatus;
import com.offerwave.mapper.MembershipMapper;
import com.offerwave.mapper.UserJobStatusMapper;
import com.offerwave.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户与会员服务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    /** 用户数据访问 */
    @Autowired
    private UserMapper userMapper;

    /** 会员数据访问 */
    @Autowired
    private MembershipMapper membershipMapper;

    /** 用户职位状态数据访问 */
    @Autowired
    private UserJobStatusMapper userJobStatusMapper;

    @Autowired
    private MembershipAccessService membershipAccessService;

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user = membershipAccessService.ensureMembershipActive(user);

        Membership membership = membershipMapper.selectById(user.getMembershipId());
        if (membership == null) {
            throw new RuntimeException("会员配置不存在");
        }

        // 1) 组装基础用户信息与偏好
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("nickname", user.getNickname());
        response.put("avatar", null);
        response.put("role", user.getRole());
        response.put("is_admin", Integer.valueOf(1).equals(user.getRole()));

        Map<String, Object> preferences = new HashMap<>();
        if (user.getPrefIndustry() != null && !user.getPrefIndustry().isEmpty()) {
            preferences.put("industry", Arrays.asList(user.getPrefIndustry().split(",")));
        } else {
            preferences.put("industry", List.of());
        }
        preferences.put("city", user.getPrefCity());
        preferences.put("job", user.getPrefJob());
        response.put("preferences", preferences);

        // 2) 组装会员与权益信息
        Map<String, Object> membershipInfo = new HashMap<>();
        membershipInfo.put("id", membership.getId());
        membershipInfo.put("level_name", membership.getLevelName());
        membershipInfo.put("expire_date", user.getMembershipExpireAt() == null
            ? null
            : user.getMembershipExpireAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (membership.getPrivileges() != null) {
            JSONObject privilegesJson = JSONUtil.parseObj(membership.getPrivileges());
            membershipInfo.put("privileges", privilegesJson);
        }
        response.put("membership", membershipInfo);

        // 3) 组装统计信息
        Map<String, Object> stats = new HashMap<>();
        LambdaQueryWrapper<UserJobStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserJobStatus::getUserId, userId);

        queryWrapper.and(wrapper -> wrapper.eq(UserJobStatus::getIsCollected, true)
                .or()
                .gt(UserJobStatus::getDeliveryStatus, 0));
        stats.put("tracked_count", userJobStatusMapper.selectCount(queryWrapper));

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
        // 出于安全考虑，不直接对前端透出 privileges 原始字段
        List<Membership> memberships = membershipMapper.selectList(null);
        memberships.forEach(m -> m.setPrivileges(null));
        return memberships;
    }

}
