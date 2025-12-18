package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String campus;
    private String building;

    @TableField("room_name")
    private String roomName;

    @TableField("total_seats")
    private Integer totalSeats;

    @TableField("open_seats")
    private Integer openSeats;

    /** open / closed / maintain */
    private String status;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}