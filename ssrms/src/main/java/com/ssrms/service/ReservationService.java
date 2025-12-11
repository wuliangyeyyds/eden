package com.ssrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateReservationDTO;
import com.ssrms.controller.dto.SlotStatusDTO;
import com.ssrms.entity.Reservation;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService extends IService<Reservation> {

    /**
     * 查询某个自习室在某一天的时段占用情况
     */
    SlotStatusDTO getSlotStatus(Integer roomId, LocalDate date);

    /**
     * 创建预约，返回冲突的时段列表；为空表示全部成功
     */
    List<Integer> createReservations(CreateReservationDTO req);

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
}