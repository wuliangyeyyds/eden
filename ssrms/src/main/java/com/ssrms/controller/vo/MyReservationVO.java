package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * “我的预约”列表 VO
 */
@Data
public class MyReservationVO {

    private Long id;

    /** 预约编号 */
    private String reservationNo;

    /** 自习室名称，如：本部 · 图书馆 3 楼 301 */
    private String roomName;

    /** 日期 */
    private LocalDate date;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 座位号（暂时简单用 seatId 字符串表示） */
    private String seatLabel;

    /** 状态：reserved / finished / cancelled ... */
    private String status;
}