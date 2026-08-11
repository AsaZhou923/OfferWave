package com.offerwave.service;

import com.offerwave.dto.UserPreferenceDto;
import com.offerwave.entity.Membership;

import java.util.List;
import java.util.Map;

/**
 * 用户与会员服务接口。
 */
public interface UserService {

    /**
     * 获取当前登录用户的详细信息。
     *
     * @param userId 当前用户 ID
     * @return 包含用户信息、偏好、会员与统计的结果
     */
    Map<String, Object> getUserInfo(Long userId);

    /**
     * 更新用户求职偏好。
     *
     * @param userId 当前用户 ID
     * @param preferenceDto 偏好参数
     * @return 是否更新成功
     */
    boolean updatePreferences(Long userId, UserPreferenceDto preferenceDto);

    /**
     * 获取可用会员等级列表。
     *
     * @return 会员等级列表
     */
    List<Membership> listMemberships();

}
