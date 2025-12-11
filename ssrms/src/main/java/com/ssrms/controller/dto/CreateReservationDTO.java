package com.ssrms.controller.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateReservationDTO {

    private Integer roomId;

    private LocalDate date;

    /** 要预约的时段 id 列表，比如 [8, 9, 10] */
    private List<Integer> slotIds;

    /** 先简单点，让前端把 userId 也传过来；以后可以从登录态里拿 */
    private Integer userId;
}