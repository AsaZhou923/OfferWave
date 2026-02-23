package com.offernow.service;

import com.offernow.dto.UserPreferenceDto;
import com.offernow.entity.Membership;

import java.util.List;
import java.util.Map;

/**
 * 用户与会员服务接口
 */
public interface UserService {

    /**
     * 获取当前登录用户的详细信息
     * @param userId 当前用户ID
     * @return 包含用户所有信息的 Map
     */
    Map<String, Object> getUserInfo(Long userId);

    /**
     * 更新用户的求职偏好
     * @param userId 当前用户ID
     * @param preferenceDto 偏好数据
     * @return 更新是否成功
     */
    boolean updatePreferences(Long userId, UserPreferenceDto preferenceDto);

    /**
     * 获取所有可用的会员等级列表
     * @return 会员等级列表
     */
    List<Membership> listMemberships();

    /**
     * 模拟升级会员
     * @param userId 当前用户ID
     * @param targetLevelId 目标等级ID
     * @return 包含新等级信息的 Map
     */
    Map<String, Object> upgradeMembership(Long userId, Integer targetLevelId);

}
