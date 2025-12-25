package com.ssrms.controller.vo;

import lombok.Data;

/**
 * 管理员预约列表行数据（后台预约管理页使用）
 */
@Data
public class AdminReservationRowVO {

    /** 预约记录主键 */
    private Long id;

    /** 预约编号（reservation_no） */
    private String code;

    private Integer userId;
    private Integer roomId;
    private Integer seatId;

    /** 学生姓名 */
    private String studentName;

    /** 学号 */
    private String studentNo;

    /** 自习室展示名（如：主校区 · 3楼 301） */
    private String room;

    /** 座位号（如：A-15） */
    private String seatNo;

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 时间段（HH:mm-HH:mm） */
    private String timeRange;

    /** 状态：reserved / checked_in / late / no_show / cancelled / cancel_overdue */
    private String status;

    /** 备注（后端计算出来的展示字段，非数据库字段） */
    private String remark;

    /** 签到时间（yyyy-MM-dd HH:mm:ss） */
    private String checkinTime;
}
