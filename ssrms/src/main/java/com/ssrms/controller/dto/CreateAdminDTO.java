package com.ssrms.controller.dto;

import lombok.Data;

/**
 * 新增管理员账号
 * 字段：account / name / password
 */
@Data
public class CreateAdminDTO {
    private String account;
    private String name;
    private String password;
}
