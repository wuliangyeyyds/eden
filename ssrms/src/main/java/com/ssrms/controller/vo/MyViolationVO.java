package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MyViolationVO {

    private Long reservationId;

    /** 日期 */
    private LocalDate date;

    /** 自习室全称：本部 · 图书馆 3 楼 301 */
    private String roomFullName;

    /** 违规类型：未签到 / 迟到 */
    private String violationType;

    /** 扣分：例如 -2 / -1 */
    private Integer penaltyScore;

    /** 备注 */
    private String remark;
}