package com.ssrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateSeatReservationDTO;
import com.ssrms.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationService extends IService<Reservation> {

    /**
     * 学生签到
     * @param reservationId 预约记录主键
     */
    Result checkIn(Long reservationId);

    /**
     * 学生取消预约
     */
    Result cancel(Long reservationId);

    /**
     * 把某个用户所有「已经结束但是仍是待签到」的预约批量标记为未签到
     * @param userId 学生用户 id
     * @return 本次一共更新了多少条
     */
    int markExpiredAsNoShow(Integer userId);

    Result createSeatReservations(CreateSeatReservationDTO dto);

    Result myViolations(Integer userId);

    Result seatConflicts(Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime);
}