package com.ssrms.controller.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理员端：用户分页结果
 */
@Data
public class AdminUserPageVO {
    private List<AdminUserRowVO> records;
    private AdminUserStatsVO stats;
}
