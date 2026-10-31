package com.bishe.teach.common;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String  message;
    private T       data;

    private Result() {}

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code    = 200;
        r.message = "操作成功";
        r.data    = data;
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.code    = 200;
        r.message = message;
        r.data    = data;
        return r;
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.code    = 500;
        r.message = message;
        return r;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.code    = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> unauthorized() {
        return error(401, "未登录或登录已过期，请重新登录");
    }

    public static <T> Result<T> forbidden() {
        return error(403, "无权限访问此资源");
    }
}
