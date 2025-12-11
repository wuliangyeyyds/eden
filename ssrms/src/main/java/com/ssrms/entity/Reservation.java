package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reservationNo;

    private Integer userId;
    private Integer roomId;
    private Integer seatId;   // 目前可以先为 null

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    /** reserved / cancelled / checked_in / finished / no_show */
    private String status;

    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}