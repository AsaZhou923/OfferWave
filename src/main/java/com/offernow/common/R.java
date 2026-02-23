package com.offernow.common;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 通用 API 响应封装。
 *
 * @param <T> 业务数据类型
 */
@Data
public class R<T> implements Serializable {

    /** 业务状态码，200 表示成功 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 扩展字段（按需补充） */
    private HashMap<String, Object> map = new HashMap<>();

    public static <T> R<T> success(T object) {
        R<T> r = new R<>();
        r.data = object;
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.message = msg;
        r.code = 500;
        return r;
    }

    public static <T> R<T> error(int code, String msg) {
        R<T> r = new R<>();
        r.message = msg;
        r.code = code;
        return r;
    }

    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }
}
