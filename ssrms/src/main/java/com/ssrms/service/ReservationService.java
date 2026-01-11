package com.ssrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateSeatReservationDTO;
import com.ssrms.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationService extends IService<Reservation> {

    // =========================
    // 用户侧：签到/取消/违规
    // =========================

    /** 签到（reserved -> checked_in / late / no_show） */
    Result checkIn(Long reservationId);

    /** 取消预约（reserved -> cancelled / cancel_overdue） */
    Result cancel(Long reservationId);

    /** 将已过期的 reserved 记录标记为 no_show，并进行违规落库/扣分（内部会防重复） */
    int markExpiredAsNoShow(Integer userId);

    /** 我的违规记录（会先刷新 no_show，并同步 late/no_show 到 violation 表） */
    Result myViolations(Integer userId);

    // =========================
    // 用户侧：创建预约/冲突查询
    // =========================

    /** 创建座位预约（支持一次多个座位） */
    Result createSeatReservations(CreateSeatReservationDTO dto);

    /** 查询座位冲突：返回 seatNo -> 冲突时间段列表 */
    Result seatConflicts(Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime);

    // =========================
    // 管理员侧：预约管理
    // =========================

    /**
     * 管理员分页查询预约
     * keyword：支持 姓名/学号/预约编号
     * roomId/date/status：筛选条件
     */
    Result adminPage(String keyword, Integer roomId, LocalDate date, String status, long page, long size);

    /** 管理员补录签到（通常对 late/no_show） */
    Result adminForceCheckin(Long reservationId);

    /** 管理员批量补录签到 */
    Result adminBatchCheckin(List<Long> ids);

    /** 管理员强制取消预约（通常对 reserved） */
    Result adminForceCancel(Long reservationId);

    /** 管理员批量取消预约 */
    Result adminBatchCancel(List<Long> ids);

    /** 管理员标记违约（由实现类决定允许哪些状态） */
    Result adminMarkViolation(Long reservationId);
}
