package com.evan.examsystem.common;

import lombok.Data;

/**
 * 统一返回类型
 * @param <T>
 */
@Data
public class Result<T> {
    public int code;
    public String message;
    public T data;

    //成功，带参数
    public static <T> Result<T> success(T data) {
        Result<T>result=new Result<T>();
        result.code=ResultCode.SUCCESS.getCode();
        result.message=ResultCode.SUCCESS.getMessage();
        result.data=data;
        return result;
    }

    //成功，无参数
    public static <T> Result<T> success() {
        return success(null);
    }

    //失败，带参数
    public static <T>Result<T>fail(ResultCode resultCode) {
        Result<T> result=new Result<T>();
        result.code=resultCode.getCode();
        result.message=resultCode.getMessage();
        return result;
    }

    //失败，自定义参数
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result=new Result<T>();
        result.code=code;
        result.message=message;
        return result;
    }


}
