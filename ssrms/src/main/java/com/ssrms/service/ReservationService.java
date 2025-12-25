package com.ssrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateSeatReservationDTO;
import com.ssrms.controller.vo.MyReservationVO;
import com.ssrms.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationService extends IService<Reservation> {

    Result checkIn(Long reservationId);

    Result cancel(Long reservationId);

    int markExpiredAsNoShow(Integer userId);

    Result myViolations(Integer userId);

    Result createSeatReservations(CreateSeatReservationDTO dto);

    Result seatConflicts(Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime);

    /* ===================== 管理员预约管理 ===================== */

    /**
     * 管理员分页查询预约（支持 keyword: 姓名/学号/预约编号，roomId，date，status）
     */
    Result adminPage(String keyword, Integer roomId, LocalDate date, String status, long page, long size);

    /** 管理员补录签到 */
    Result adminForceCheckin(Long reservationId);

    /** 管理员取消预约 */
    Result adminForceCancel(Long reservationId);

    /** 管理员标记违约（未签到） */
    Result adminMarkViolation(Long reservationId);

    /** 批量补录签到 */
    Result adminBatchCheckin(List<Long> ids);

    /** 批量取消预约 */
    Result adminBatchCancel(List<Long> ids);

    List<MyReservationVO> listMyReservations(Long userId, Boolean onlyPending, Boolean onlyViolation);
}
