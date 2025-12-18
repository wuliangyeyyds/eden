package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("violation")
public class Violation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("reservation_id")
    private Long reservationId;

    @TableField("v_type")
    private String vType; // no_show / late / ...

    @TableField("v_date")
    private LocalDate vDate;

    @TableField("room_id")
    private Integer roomId;

    @TableField("description")
    private String description;

    @TableField("penalty_score")
    private Integer penaltyScore; // ✅存正数：1/2

    @TableField("handled")
    private Integer handled;

    @TableField("create_time")
    private LocalDateTime createTime;
}