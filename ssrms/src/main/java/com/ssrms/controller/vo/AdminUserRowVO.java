package com.ssrms.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员端：用户列表行 VO
 */
@Data
public class AdminUserRowVO {
    private Integer id;
    private String account;
    private String name;
    private String studentNo;

    /** 0 管理员 / 1 用户 */
    private Integer roleId;

    private String phone;
    private String email;
    private String college;
    private String gradeClass;

    private Integer creditScore;

    /** 0正常 1预警 2黑名单 */
    private Integer status;

    /** 违规次数（来自 violation 表聚合） */
    private Integer violationCount;

    private String isValid;

    private String lastLoginIp;
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
