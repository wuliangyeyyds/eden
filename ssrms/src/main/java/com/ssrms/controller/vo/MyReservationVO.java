package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MyReservationVO {

    private Long id;

    /** 预约编号 */
    private String reservationNo;

    /** 校区 */
    private String campus;

    /** 建筑 */
    private String building;

    /** 自习室名称（room_name） */
    private String roomName;

    /** 兼容字段：校区 · 建筑 自习室 */
    private String roomLabel;

    /** 座位号（seat_no），没有则“未指定” */
    private String seatNo;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    /** reserved / checked_in / late / no_show / cancelled / cancel_overdue ... */
    private String status;
}