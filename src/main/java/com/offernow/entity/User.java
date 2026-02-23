package com.offernow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户基础信息表
 */
@TableName(value ="users")
@Data
public class User implements Serializable {
    /**
     * 主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号(可以是手机号/邮箱/自定义字母)
     */
    private String username;

    /**
     * BCrypt密码哈希值
     */
    private String passwordHash;

    /**
     * 微信唯一标识(暂留空)
     */
    private String wechatOpenid;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 会员等级 (关联等级表 ID)
     */
    private Integer membershipId;

    /**
     * 偏好行业 (建议存储 JSON 数组或逗号分隔)
     */
    private String prefIndustry;

    /**
     * 偏好城市
     */
    private String prefCity;

    /**
     * 偏好岗位
     */
    private String prefJob;
    
    /**
     * 学历 (本科/硕士等)
     */
    private String educationBackground;

    /**
     * 期望薪资 (月薪)
     */
    private Integer salary;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLogin;

    /**
     * 注册时间
     */
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
