package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("reservation_no")
    private String reservationNo;

    @TableField("user_id")
    private Integer userId;

    @TableField("room_id")
    private Integer roomId;

    @TableField("seat_id")
    private Integer seatId;   // 可为空（不精确到座位时）

    @TableField("date")
    private LocalDate date;

    @TableField("start_time")
    private LocalTime startTime;

    @TableField("end_time")
    private LocalTime endTime;

    /**
     * reserved / checked_in / late / no_show / cancelled / cancel_overdue
     */
    @TableField("status")
    private String status;

    @TableField("checkin_time")
    private LocalDateTime checkinTime;

    @TableField("checkout_time")
    private LocalDateTime checkoutTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}