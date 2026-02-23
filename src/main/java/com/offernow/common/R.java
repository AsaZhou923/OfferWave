package com.offernow.common;

import lombok.Data;
import java.io.Serializable;
import java.util.HashMap;

/**
 * 通用 API 响应封装类
 * @param <T>
 */
@Data
public class R<T> implements Serializable {

    private Integer code; // 编码：200表示成功，其他值表示失败
    private String message; // 错误信息
    private T data; // 数据
    private HashMap<String, Object> map = new HashMap<>(); // 动态数据

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
        r.code = 500; // 默认为 500
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
