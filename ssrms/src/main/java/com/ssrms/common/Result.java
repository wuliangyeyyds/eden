package com.ssrms.common;

import lombok.Data;

@Data
public class Result {
    private int code;   // 编码 200/400
    private String msg; // 成功/失败
    private Long total; // 总记录数
    private Object data; // 数据

    public static Result fail(){
        return result(400, "失败", 0L, null);
    }

    // 新增：带自定义错误信息的 fail
    public static Result fail(String msg){
        return result(400, msg, 0L, null);
    }

    public static Result success(){
        return result(200, "成功", 0L, null);
    }

    public static Result success(Object data){
        return result(200, "成功", 0L, data);
    }

    public static Result success(Object data, Long total){
        return result(200, "成功", total, data);
    }

    private static Result result(int code, String msg, Long total, Object data) {
        Result res = new Result();
        res.setCode(code);
        res.setMsg(msg);
        res.setTotal(total);
        res.setData(data);
        return res;
    }

    public static Result successWithMsg(String msg) {
        return result(200, msg, 0L, null);
    }

    public static Result failWithMsg(String msg) {
        return result(400, msg, 0L, null);
    }

    public static Result errorWithMsg(String msg) {
        return result(500, msg, 0L, null);
    }
}