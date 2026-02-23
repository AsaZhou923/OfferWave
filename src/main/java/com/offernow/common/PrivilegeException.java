package com.offernow.common;

/**
 * 自定义权限异常。
 * 用于表示因会员权限不足导致的业务失败。
 */
public class PrivilegeException extends RuntimeException {

    public PrivilegeException(String message) {
        super(message);
    }
}
