package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MyViolationVO {

    private Long reservationId;

    /** 校区/楼/自习室：前端违规表格需要 */
    private String campus;
    private String building;
    private String roomName;

    /** 兼容字段：校区 · 建筑 自习室 */
    private String roomLabel;

    private String seatNo;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    /** 迟到 / 未签到 */
    private String violationType;

    /** 扣分（例如 -1 / -2） */
    private Integer penaltyScore;

    private String remark;
}