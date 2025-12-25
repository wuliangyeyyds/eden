package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO（用于登录返回、个人信息页等）
 * 数据库已升级为 status：0正常 1预警 2黑名单
 */
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
    private Integer status;
    private Integer blacklistFlag;
    private String isValid;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
}
