package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private String content;

    /**
     * RULE / ADJUSTMENT / EMERGENCY / MAINTENANCE / EXAM / OTHER
     */
    private String type;

    /**
     * IMPORTANT / WARNING / INFO
     */
    private String level;

    /**
     * 0 管理员 / 1 学生 / 2 全部
     */
    @TableField("target_role")
    private Integer targetRole;

    @TableField("target_text")
    private String targetText;

    @TableField("is_top")
    private Integer isTop;

    @TableField("is_published")
    private Integer isPublished;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("created_by")
    private Integer createdBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
