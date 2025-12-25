package com.ssrms.controller.vo;

import lombok.Data;

/**
 * 管理员端：用户统计
 */
@Data
public class AdminUserStatsVO {
    private Long total;
    private Long normal;
    private Long warning;
    private Long blacklist;
}
