package com.ssrms.controller.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class CreateSeatReservationDTO {

    private Integer userId;
    private Integer roomId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 多个座位号：["01","32"] */
    private List<String> seatNos;
}