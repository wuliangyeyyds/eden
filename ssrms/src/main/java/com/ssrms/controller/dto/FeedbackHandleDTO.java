package com.ssrms.controller.dto;

import lombok.Data;

/**
 * 管理员处理反馈/投诉
 *
 * status: pending / processing / resolved
 * cancelReservation: true 时（且该反馈有关联 reservationId），会尝试管理员强制取消该预约
 */
@Data
public class FeedbackHandleDTO {
    private Long id;
    private String status;
    private String reply;
    private Boolean cancelReservation;
}
