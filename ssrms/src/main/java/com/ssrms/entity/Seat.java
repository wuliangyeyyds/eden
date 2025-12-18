package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("room_id")
    private Integer roomId;

    /** seat_no */
    @TableField("seat_no")
    private String seatNo;

    /** enabled / disabled */
    private String status;

    private String remark;
}