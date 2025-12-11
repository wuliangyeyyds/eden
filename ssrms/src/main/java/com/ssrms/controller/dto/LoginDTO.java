package com.ssrms.controller.dto;

import lombok.Data;

@Data
public class LoginDTO {

    private String account;
    private String password;

    /**
     * 0 管理员 1 学生
     */
    private Integer roleId;
}