package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateReservationDTO;
import com.ssrms.controller.dto.SlotStatusDTO;
import com.ssrms.service.ReservationService;
import com.ssrms.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ssrms.controller.vo.MyReservationVO;
import com.ssrms.controller.vo.MyViolationVO;
import com.ssrms.entity.Reservation;
import com.ssrms.entity.Room;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomService roomService;

    /**
     * 查询某自习室某一天的时段占用情况
     * GET /reservation/slots?roomId=3&date=2025-12-13
     */
    @GetMapping("/slots")
    public Result getSlots(@RequestParam Integer roomId,
                           @RequestParam
                           @DateTimeFormat(pattern = "yyyy-MM-dd")
                           LocalDate date) {
        SlotStatusDTO dto = reservationService.getSlotStatus(roomId, date);
        return Result.success(dto);
    }

    /**
     * 创建预约
     * POST /reservation/create
     * body: { roomId, date, slotIds, userId }
     */
    @PostMapping("/create")
    public Result create(@RequestBody CreateReservationDTO req) {
        // 先简单一点，userId 从 body 里拿；以后可以改成从登录态中解析
        if (req.getUserId() == null) {
            return Result.fail("用户未登录或 userId 为空");
        }
        List<Integer> conflict = reservationService.createReservations(req);
        if (!conflict.isEmpty()) {
            return Result.fail("部分时段已被约满，请刷新后重新选择");
        }
        return Result.success("预约成功");
    }

    /**
     * 我的预约列表（按日期倒序，时间正序）
     */
    @GetMapping("/my")
    public Result myReservations(@RequestParam Integer userId) {
        // 1）查出这个用户的所有预约，按日期倒序、开始时间正序
        List<Reservation> list = reservationService.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .orderByDesc(Reservation::getDate)
                        .orderByAsc(Reservation::getStartTime)
        );

        if (list.isEmpty()) {
            // 没有记录也返回成功，data 是空列表，total 是 0
            return Result.success(Collections.emptyList(), 0L);
        }

        // 2）一次性把涉及到的房间都查出来，组一个 map
        Set<Integer> roomIds = list.stream()
                .map(Reservation::getRoomId)
                .collect(Collectors.toSet());

        Map<Integer, Room> roomMap = roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, r -> r));

        // 3）拼装 VO
        List<MyReservationVO> voList = list.stream().map(r -> {
            MyReservationVO vo = new MyReservationVO();
            vo.setId(r.getId());
            vo.setReservationNo(r.getReservationNo());
            vo.setDate(r.getDate());
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());
            vo.setStatus(r.getStatus());

            Room room = roomMap.get(r.getRoomId());
            if (room != null) {
                String roomName = room.getCampus() + " · "
                        + room.getBuilding() + " "
                        + room.getRoomName();
                vo.setRoomName(roomName);
            } else {
                vo.setRoomName("已删除的自习室(ID=" + r.getRoomId() + ")");
            }

            // 暂时座位号就直接用 seatId 转成字符串
            vo.setSeatLabel(r.getSeatId() == null ? null : String.valueOf(r.getSeatId()));

            return vo;
        }).collect(Collectors.toList());

        // 4）用你的 Result.success(Object data, Long total) 返回
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

        // 1）先刷新一下未签到（把已经结束还没签到的记录标记为 no_show）
        reservationService.markExpiredAsNoShow(userId);

        // 2）查出该用户所有 no_show / late 的记录
        List<Reservation> list = reservationService.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .in(Reservation::getStatus, Arrays.asList("no_show", "late"))
                        .orderByDesc(Reservation::getDate)
                        .orderByAsc(Reservation::getStartTime)
        );

        if (list.isEmpty()) {
            return Result.success(Collections.emptyList(), 0L);
        }

        // 3）一次性查出所有相关房间，组 map
        Set<Integer> roomIds = list.stream()
                .map(Reservation::getRoomId)
                .collect(Collectors.toSet());

        Map<Integer, Room> roomMap = roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, r -> r));

        // 4）组装 VO
        List<MyViolationVO> voList = list.stream().map(r -> {
            MyViolationVO vo = new MyViolationVO();

            vo.setReservationId(r.getId());
            vo.setDate(r.getDate());

            Room room = roomMap.get(r.getRoomId());
            if (room != null) {
                String roomFullName = room.getCampus() + " · "
                        + room.getBuilding() + " "
                        + room.getRoomName();
                vo.setRoomFullName(roomFullName);
            } else {
                vo.setRoomFullName("已删除的自习室(ID=" + r.getRoomId() + ")");
            }

            String status = r.getStatus();
            if ("no_show".equals(status)) {
                vo.setViolationType("未签到");
                vo.setPenaltyScore(-2);
                vo.setRemark("预约后未按时签到");
            } else if ("late".equals(status)) {
                vo.setViolationType("迟到");
                vo.setPenaltyScore(-1);
                vo.setRemark("超过开始时间较久后才签到");
            } else {
                vo.setViolationType(status);
                vo.setPenaltyScore(0);
                vo.setRemark("未知类型");
            }

            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList, (long) voList.size());
    }
}