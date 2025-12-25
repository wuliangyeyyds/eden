package com.ssrms.controller.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理员预约分页返回体（records + stats，total 放在 Result.total 里）
 */
@Data
public class AdminReservationPageVO {

    private List<AdminReservationRowVO> records;

    private AdminReservationStatsVO stats;
}
