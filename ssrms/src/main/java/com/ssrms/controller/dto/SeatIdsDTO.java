package com.ssrms.controller.dto;

import lombok.Data;

import java.util.List;

/** 批量更新座位状态 */
@Data
public class SeatIdsDTO {
    private List<Integer> seatIds;
    /** enabled / disabled */
    private String status;
}
