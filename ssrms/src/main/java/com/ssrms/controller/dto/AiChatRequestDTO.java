package com.ssrms.controller.dto;

import lombok.Data;

@Data
public class AiChatRequestDTO {
    private Integer userId;
    private String message;
    private String conversationId;
}
