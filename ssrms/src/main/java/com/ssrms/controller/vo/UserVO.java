package com.ssrms.controller.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Integer id;
    private String account;
    private String name;
    private String studentNo;
    private Integer roleId;
    private String phone;
    private String email;
    private String college;
    private String gradeClass;
    private String commonCampus;
    private String profileRemark;
    private Integer creditScore;
    private Integer blacklistFlag;
    private String isValid;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}