package com.offernow.common;

/**
 * 自定义权限异常
 * 用于处理因用户会员等级权益不足而导致的操作失败
 */
public class PrivilegeException extends RuntimeException {
    public PrivilegeException(String message) {
        super(message);
    }
}
