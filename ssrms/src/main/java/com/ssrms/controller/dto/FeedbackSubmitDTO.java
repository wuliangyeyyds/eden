package com.ssrms.controller.dto;

import lombok.Data;

/**
 * 学生端提交反馈 DTO
 * 与前端 UserHome.vue submitFeedback() payload 对齐：
 * {
 *   userId,
 *   category,
 *   rating,
 *   content,
 *   reservationId
 * }
 */
@Data
public class FeedbackSubmitDTO {
    private Long userId;
    private String category;
    private Integer rating;
    private String content;
    private Long reservationId;
}
