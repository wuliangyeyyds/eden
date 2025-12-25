package com.ssrms.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateSeatReservationDTO;
import com.ssrms.controller.dto.IdsDTO;
import com.ssrms.controller.vo.MyReservationVO;
import com.ssrms.entity.Reservation;
import com.ssrms.entity.Room;
import com.ssrms.entity.Seat;
import com.ssrms.service.ReservationService;
import com.ssrms.service.RoomService;
import com.ssrms.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预约相关接口（用户端 + 管理员端）
 * - Result 已回退为兼容版（支持 fail/error/successWithMsg 等旧方法）
 */
@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SeatService seatService;

    // =========================
    // 用户端
    // =========================

    /** 创建预约（支持一次提交多个 seatNos） */
    @PostMapping("/create")
    public Result create(@RequestBody CreateSeatReservationDTO dto) {
        return reservationService.createSeatReservations(dto);
    }

    /** 我的预约列表 */
    @GetMapping("/my")
    public Result myReservations(@RequestParam Integer userId) {

        List<Reservation> list = reservationService.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .orderByDesc(Reservation::getDate)
                        .orderByAsc(Reservation::getStartTime)
        );

        if (list == null || list.isEmpty()) {
            return Result.success(Collections.emptyList(), 0L);
        }

        // roomMap
        Set<Integer> roomIds = list.stream()
                .map(Reservation::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Room> roomMap = roomIds.isEmpty()
                ? Collections.emptyMap()
                : roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, Function.identity(), (a, b) -> a));

        // seatNoMap
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

            // room info
            Room room = (r.getRoomId() == null) ? null : roomMap.get(r.getRoomId());
            if (room != null) {
                // ✅ 用户端“我的预约”页面需要单独展示校区/建筑
                // 保持向后兼容：同时返回 campus/building 以及组合字段 roomLabel
                vo.setCampus(room.getCampus());
                vo.setBuilding(room.getBuilding());
                vo.setRoomName(room.getRoomName());
                String campus = safe(room.getCampus());
                String building = safe(room.getBuilding());
                String rn = safe(room.getRoomName());
                String label = (campus.isEmpty() ? "" : campus + " · ")
                        + (building.isEmpty() ? "" : building + " ")
                        + rn;
                vo.setRoomLabel(label.trim());
            } else {
                vo.setRoomName("未知自习室");
                vo.setRoomLabel("未知自习室");
            }

            // seat
            String seatNo = (r.getSeatId() == null) ? null : seatNoMap.get(r.getSeatId());
            if (seatNo == null || seatNo.isBlank()) seatNo = "未指定";
            vo.setSeatNo(seatNo);

            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList, (long) voList.size());
    }

    /** 学生签到（reserved -> checked_in 或 late；由 service 内部判定） */
    @PostMapping("/checkin/{id}")
    public Result checkIn(@PathVariable("id") Long reservationId) {
        return reservationService.checkIn(reservationId);
    }

    /** 学生取消预约（通常仅 reserved 可取消；由 service 内部判定） */
    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable("id") Long reservationId) {
        return reservationService.cancel(reservationId);
    }

    /** 查询时间段冲突座位 */
    @GetMapping("/seatConflicts")
    public Result seatConflicts(@RequestParam Integer roomId,
                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
                                @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime) {
        return reservationService.seatConflicts(roomId, date, startTime, endTime);
    }

    /** 我的违规记录 */
    @GetMapping("/violations")
    public Result myViolations(@RequestParam Integer userId) {
        return reservationService.myViolations(userId);
    }

    /** 将过期未签到的预约标记为 no_show（返回本次更新条数 int） */
    @PostMapping("/refreshNoShow")
    public Result refreshNoShow(@RequestParam Integer userId) {
        int updated = reservationService.markExpiredAsNoShow(userId);
        return Result.success(updated);
    }

    // =========================
    // 管理员端
    // =========================

    /** 管理员分页查询（keyword=姓名/学号/预约号；roomId/date/status） */
    @GetMapping("/admin/page")
    public Result adminPage(@RequestParam(defaultValue = "1") Long page,
                            @RequestParam(defaultValue = "8") Long size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Integer roomId,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                            @RequestParam(required = false) String status) {
        return reservationService.adminPage(keyword, roomId, date, status, page, size);
    }

    /** 管理员：补录签到 */
    @PostMapping("/admin/checkin/{id}")
    public Result adminForceCheckin(@PathVariable("id") Long id) {
        return reservationService.adminForceCheckin(id);
    }

    /** 管理员：取消预约 */
    @PostMapping("/admin/cancel/{id}")
    public Result adminForceCancel(@PathVariable("id") Long id) {
        return reservationService.adminForceCancel(id);
    }

    /** 管理员：标记违约 */
    @PostMapping("/admin/markViolation/{id}")
    public Result adminMarkViolation(@PathVariable("id") Long id) {
        return reservationService.adminMarkViolation(id);
    }

    /** 管理员：批量补录签到 */
    @PostMapping("/admin/batchCheckin")
    public Result adminBatchCheckin(@RequestBody IdsDTO dto) {
        return reservationService.adminBatchCheckin(dto == null ? null : dto.getIds());
    }

    /** 管理员：批量取消预约 */
    @PostMapping("/admin/batchCancel")
    public Result adminBatchCancel(@RequestBody IdsDTO dto) {
        return reservationService.adminBatchCancel(dto == null ? null : dto.getIds());
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}