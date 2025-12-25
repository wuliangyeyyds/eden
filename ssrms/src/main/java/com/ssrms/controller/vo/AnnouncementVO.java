package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementVO {
    private Integer id;
    private String title;
    private String content;
    private String type;
    private String level;
    private Integer targetRole;
    private String targetText;
    private Integer isTop;
    private LocalDateTime publishTime;
}
