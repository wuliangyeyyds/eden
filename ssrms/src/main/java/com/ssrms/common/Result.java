package com.ssrms.common;

import lombok.Data;

/**
 * 统一返回体（兼容旧版调用：success / fail / error / successWithMsg / failWithMsg 等）。
 *
 * 约定：
 *  - code=200：成功
 *  - code=400：业务失败（参数/状态不允许等）
 *  - code=500：系统异常
 */
@Data
public class Result {

    private int code;
    private String msg;
    private Long total;
    private Object data;

    // =========================
    // 基础构造
    // =========================
    public static Result of(int code, String msg, Long total, Object data) {
        Result r = new Result();
        r.setCode(code);
        r.setMsg(msg);
        r.setTotal(total == null ? 0L : total);
        r.setData(data);
        return r;
    }

    // =========================
    // success
    // =========================
    public static Result success() {
        return of(200, "成功", 0L, null);
    }

    public static Result success(Object data) {
        return of(200, "成功", 0L, data);
    }

    public static Result success(Object data, Long total) {
        return of(200, "成功", total, data);
    }

    /** 兼容：成功 + 自定义 msg */
    public static Result success(Object data, String msg) {
        return of(200, msg == null ? "成功" : msg, 0L, data);
    }

    public static Result successWithMsg(String msg) {
        return of(200, msg == null ? "成功" : msg, 0L, null);
    }

    // =========================
    // fail（业务失败）
    // =========================
    public static Result fail() {
        return of(400, "失败", 0L, null);
    }

    /** 兼容旧项目常用写法：Result.fail("xxx") */
    public static Result fail(String msg) {
        return of(400, msg == null ? "失败" : msg, 0L, null);
    }

    public static Result failWithMsg(String msg) {
        return fail(msg);
    }

    // =========================
    // error（系统异常）
    // =========================
    /** 兼容旧项目常用写法：Result.error("xxx") */
    public static Result error(String msg) {
        return of(500, msg == null ? "系统异常" : msg, 0L, null);
    }

    public static Result errorWithMsg(String msg) {
        return error(msg);
    }
}