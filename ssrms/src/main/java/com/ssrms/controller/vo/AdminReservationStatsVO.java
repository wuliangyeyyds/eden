package com.ssrms.controller.vo;

import lombok.Data;

/**
 * 管理员预约统计（后台预约管理页顶部统计卡片）
 */
@Data
public class AdminReservationStatsVO {

    /** 当前记录（与列表查询条件保持一致，忽略分页） */
    private Long total;

    /** 已预约（reserved） */
    private Long booked;

    /** 已签到（checked_in） */
    private Long signed;

    /** 已取消（cancelled + cancel_overdue） */
    private Long canceled;

    /** 违约（late + no_show） */
    private Long violation;
}
