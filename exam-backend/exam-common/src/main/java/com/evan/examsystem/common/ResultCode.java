package com.evan.examsystem.common;

import lombok.Getter;

/**
 * 枚举错误码
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或 token 失效"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USERNAME_EXISTS(1002, "用户名已存在"),

    EXAM_NOT_STARTED(2001, "考试未开始"),
    EXAM_ENDED(2002, "考试已结束"),
    EXAM_ALREADY_SUBMITTED(2003, "已交卷，不能重复交");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
