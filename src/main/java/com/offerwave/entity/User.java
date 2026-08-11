package com.offerwave.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表实体。
 */
@TableName(value = "users")
@Data
public class User implements Serializable {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 独立邮箱（用于验证码登录和找回密码） */
    private String email;

    /** BCrypt 密码哈希 */
    @JsonIgnore
    @ToString.Exclude
    private String passwordHash;

    private Integer role;

    /** 账号状态：1-正常，0-封禁 */
    private Integer accountStatus;

    /** 微信 openid（预留） */
    private String wechatOpenid;

    /** 昵称 */
    private String nickname;

    /** 会员等级 ID */
    private Integer membershipId;

    /** 会员到期时间（null 表示永久） */
    private LocalDateTime membershipExpireAt;

    /** 偏好行业 */
    private String prefIndustry;

    /** 偏好城市 */
    private String prefCity;

    /** 偏好岗位 */
    private String prefJob;

    /** 学历背景 */
    private String educationBackground;

    /** 期望薪资 */
    private Integer salary;

    /** 手工配置追踪额度上限（null 表示按会员权益） */
    private Integer customTrackLimit;

    /** 最后登录时间 */
    private LocalDateTime lastLogin;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
