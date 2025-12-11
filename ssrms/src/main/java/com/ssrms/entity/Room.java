package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private String roomName;

    private Integer totalSeats;
    private Integer openSeats;

    /** open / closed / maintain */
    private String status;

    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}