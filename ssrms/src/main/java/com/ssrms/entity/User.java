package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String account;

    private String name;

    @TableField("student_no")
    private String studentNo;

    private String password;

    private Integer age;

    private Integer sex;

    private String phone;

    private String email;

    @TableField("role_id")
    private Integer roleId;

    @TableField("isValid")
    private String isValid;

    private String college;

    @TableField("grade_class")
    private String gradeClass;

    @TableField("common_campus")
    private String commonCampus;

    @TableField("profile_remark")
    private String profileRemark;

    @TableField("credit_score")
    private Integer creditScore;

    @TableField("blacklist_flag")
    private Integer blacklistFlag;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}