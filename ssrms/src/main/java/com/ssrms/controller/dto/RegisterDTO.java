package com.ssrms.controller.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String account;
    private String name;
    private String studentNo;
    private String password;
    private String confirmPassword;  // 仅用于校验，不入库
}