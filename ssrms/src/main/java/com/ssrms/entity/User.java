package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表 user
 *
 * 注意：数据库字段已从 blacklist_flag 升级为 status（0正常 1预警 2黑名单）。
 * 为了尽量减少全项目的改动，这里仍保留 Java 字段名 blacklistFlag，
 * 但它实际映射到数据库的 status 字段，取值范围为 0/1/2。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("account")
    private String account;

    @TableField("name")
    private String name;

    @TableField("student_no")
    private String studentNo;

    @TableField("password")
    private String password;

    @TableField("age")
    private Integer age;

    @TableField("sex")
    private Integer sex; // 0 女 1 男

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("role_id")
    private Integer roleId; // 0 管理员 1 学生

    @TableField("isValid")
    private String isValid; // Y / N

    @TableField("college")
    private String college;

    @TableField("grade_class")
    private String gradeClass;

    @TableField("common_campus")
    private String commonCampus;

    @TableField("profile_remark")
    private String profileRemark;

    @TableField("credit_score")
    private Integer creditScore;

    @TableField("status")
    private Integer blacklistFlag; // 0正常 1预警 2黑名单

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
}