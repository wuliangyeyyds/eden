package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员端投诉/反馈列表展示 VO
 */
@Data
public class FeedbackAdminVO {
    private Long id;

    private Integer userId;
    private String userName;
    private String studentNo;

    private Integer roomId;
    private String roomLabel;

    private Long reservationId;
    private String reservationLabel;

    private String category;
    private Integer rating;
    private String content;

    private String status;
    private String reply;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
