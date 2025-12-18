package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateSeatReservationDTO;
import com.ssrms.entity.Seat;
import com.ssrms.service.ReservationService;
import com.ssrms.service.RoomService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ssrms.controller.vo.MyReservationVO;
import com.ssrms.controller.vo.MyViolationVO;
import com.ssrms.entity.Reservation;
import com.ssrms.entity.Room;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private com.ssrms.service.SeatService seatService;

    /**
     * 我的预约列表（按日期倒序，时间正序）
     */
    @GetMapping("/my")
    public Result myReservations(@RequestParam Integer userId) {

        List<Reservation> list = reservationService.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .orderByDesc(Reservation::getDate)
                        .orderByAsc(Reservation::getStartTime)
        );

        if (list.isEmpty()) {
            return Result.success(Collections.emptyList(), 0L);
        }

        // roomMap（过滤 null，避免 IN (null)）
        Set<Integer> roomIds = list.stream()
                .map(Reservation::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Room> roomMap = roomIds.isEmpty()
                ? Collections.emptyMap()
                : roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, r -> r, (a, b) -> a));

        // seatId -> seatNo
        Set<Integer> seatIds = list.stream()
                .map(Reservation::getSeatId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> seatNoMap = seatIds.isEmpty()
                ? Collections.emptyMap()
                : seatService.listByIds(seatIds).stream()
                .collect(Collectors.toMap(Seat::getId, Seat::getSeatNo, (a, b) -> a));

        List<MyReservationVO> voList = list.stream().map(r -> {
            MyReservationVO vo = new MyReservationVO();
            vo.setId(r.getId());
            vo.setReservationNo(r.getReservationNo());
            vo.setDate(r.getDate());
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());
            vo.setStatus(r.getStatus());

            Room room = (r.getRoomId() == null) ? null : roomMap.get(r.getRoomId());
            if (room != null) {
                vo.setCampus(room.getCampus());
                vo.setBuilding(room.getBuilding());
                vo.setRoomName(room.getRoomName());
                vo.setRoomLabel(vo.getCampus() + " · " + vo.getBuilding() + " " + vo.getRoomName());
            } else {
                vo.setCampus("未知校区");
                vo.setBuilding("未知建筑");
                vo.setRoomName("未知自习室");
                vo.setRoomLabel("未知场地");
            }

            Integer seatId = r.getSeatId();
            String seatNo = (seatId == null) ? null : seatNoMap.get(seatId);
            if (seatNo == null || seatNo.isBlank()) seatNo = "未指定";
            vo.setSeatNo(seatNo);

            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList, (long) voList.size());
    }


    /**
     * 学生签到
     * 规则：
     *  - 可签到时间：开始时间前 10 分钟 ~ 结束时间
     *  - 正常签到：开始时间前 10 分钟 ~ 开始时间后 10 分钟
     *  - 迟到签到：开始时间后 10 分钟 ~ 结束时间
     *  - 超过结束时间未签到：标记为未签到(no_show)，不能再签到
     */
    @PostMapping("/checkin/{id}")
    public Result checkIn(@PathVariable Long id) {
        // 业务逻辑全部在 ReservationServiceImpl.checkIn 里
        return reservationService.checkIn(id);
    }

    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id) {
        return reservationService.cancel(id);
    }

    /**
     * 刷新当前用户的“未签到”状态：
     * 把所有已经结束、但还处于 reserved 的预约记录批量标记为 no_show
     * 返回本次一共更新了多少条
     */
    @PostMapping("/refreshNoShow")
    public Result refreshNoShow(@RequestParam Integer userId) {
        int updated = reservationService.markExpiredAsNoShow(userId);
        // data 里放一个整数（本次被标记为未签到的条数）
        return Result.success(updated);
    }

    /**
     * 我的违规记录：
     *  - 先把这个用户所有已经结束但还处于 reserved 的预约批量标记为 no_show
     *  - 然后查询状态为 no_show / late 的记录
     *  - 按日期倒序、开始时间正序
     */
    @GetMapping("/violations")
    public Result myViolations(@RequestParam Integer userId) {
        return reservationService.myViolations(userId);
    }

    @PostMapping("/create")
    public Result create(@RequestBody CreateSeatReservationDTO dto) {
        return reservationService.createSeatReservations(dto);
    }

    /**
     * 兼容前端：用于获取时段状态
     * GET /reservation/slots?roomId=1&date=2025-12-19
     * 先按“全部可用”返回，避免前端报错（你要做更精细的再升级）
     */

    @GetMapping("/slots")
    public Result slotStatus(@RequestParam Integer roomId,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<SlotStatusVO> list = new ArrayList<>();
        for (int h = 8; h < 22; h++) {
            SlotStatusVO vo = new SlotStatusVO();
            vo.setSlotId(h);
            vo.setStatus("available");
            list.add(vo);
        }
        return Result.success(list);
    }

    @Data
    public static class SlotStatusVO {
        private Integer slotId;
        private String status;
    }

    @GetMapping("/seatConflicts")
    public Result seatConflicts(@RequestParam Integer roomId,
                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
                                @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime) {
        return reservationService.seatConflicts(roomId, date, startTime, endTime);
    }
}