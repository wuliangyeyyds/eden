package com.ssrms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateReservationDTO;
import com.ssrms.controller.dto.SlotStatusDTO;
import com.ssrms.entity.Reservation;
import com.ssrms.entity.Room;
import com.ssrms.mapper.ReservationMapper;
import com.ssrms.service.ReservationService;
import com.ssrms.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



@Service
public class ReservationServiceImpl
        extends ServiceImpl<ReservationMapper, Reservation>
        implements ReservationService {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReservationMapper reservationMapper;

    @Override
    public SlotStatusDTO getSlotStatus(Integer roomId, LocalDate date) {
        SlotStatusDTO dto = new SlotStatusDTO();
        dto.setRoomId(roomId);
        dto.setDate(date);

        Room room = roomService.getById(roomId);
        if (room == null) {
            // 找不到房间：全部不可预约
            dto.setDisabledSlotIds(
                    IntStream.range(0, 24).boxed().collect(Collectors.toList())
            );
            return dto;
        }

        // 房间不是 open，就视为整天不可预约
        if (!"open".equalsIgnoreCase(room.getStatus())) {
            dto.setDisabledSlotIds(
                    IntStream.range(0, 24).boxed().collect(Collectors.toList())
            );
            return dto;
        }

        // === 关键改动从这里开始 ===

        // 查当天该房间所有「有效」预约
        List<Reservation> list = this.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getRoomId, roomId)
                        .eq(Reservation::getDate, date)
                        .in(Reservation::getStatus,
                                Arrays.asList("reserved", "checked_in"))
        );

        // 只要这个小时存在任意一条记录，就认为该时段已被占用
        Set<Integer> disabledSet = list.stream()
                .map(r -> r.getStartTime().getHour())  // 0~23
                .collect(Collectors.toSet());

        List<Integer> disabled = disabledSet.stream()
                .sorted()
                .collect(Collectors.toList());

        dto.setDisabledSlotIds(disabled);
        return dto;
    }

    @Override
    @Transactional
    public List<Integer> createReservations(CreateReservationDTO req) {
        Integer userId = req.getUserId();
        Integer roomId = req.getRoomId();
        LocalDate date = req.getDate();
        List<Integer> slotIds = req.getSlotIds();

        if (userId == null || roomId == null || date == null
                || slotIds == null || slotIds.isEmpty()) {
            // 参数不合法时简单返回全部失败
            return new ArrayList<>(slotIds == null ? Collections.emptyList() : slotIds);
        }

        Room room = roomService.getById(roomId);
        if (room == null || !"open".equalsIgnoreCase(room.getStatus())) {
            // 房间不存在或关闭：全部失败
            return new ArrayList<>(slotIds);
        }

        // 查询当天该房间已有的「有效」预约
        List<Reservation> existing = this.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getRoomId, roomId)
                        .eq(Reservation::getDate, date)
                        .in(Reservation::getStatus,
                                Arrays.asList("reserved", "checked_in"))
        );

        // 按小时统计已有记录数
        Map<Integer, Long> countByHour = existing.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStartTime().getHour(),
                        Collectors.counting()
                ));

        // 只要某个小时已经有记录（>0），就视为冲突
        List<Integer> conflict = new ArrayList<>();
        for (Integer slotId : slotIds) {
            long current = countByHour.getOrDefault(slotId, 0L);
            if (current > 0) {
                conflict.add(slotId);
            }
        }

        if (!conflict.isEmpty()) {
            // 存在冲突，不插入，直接返回冲突列表
            return conflict;
        }

        // 没冲突，可以插入预约记录
        List<Reservation> toSave = new ArrayList<>();
        for (Integer slotId : slotIds) {
            LocalTime start = LocalTime.of(slotId, 0);
            LocalTime end = LocalTime.of((slotId + 1) % 24, 0);

            Reservation r = new Reservation();
            r.setReservationNo(generateNo());
            r.setUserId(userId);
            r.setRoomId(roomId);
            r.setSeatId(null);       // 目前不选座
            r.setDate(date);
            r.setStartTime(start);
            r.setEndTime(end);
            r.setStatus("reserved");

            toSave.add(r);
        }

        this.saveBatch(toSave);
        // 返回空列表表示全部成功
        return Collections.emptyList();
    }

    // 简单的预约编号生成器：Y + 日期 + 随机后缀
    private String generateNo() {
        String dateStr = java.time.LocalDate.now().toString().replace("-", "");
        String rand = String.valueOf((int) (Math.random() * 9000 + 1000));
        return "Y" + dateStr + rand;
    }

    @Override
    public Result checkIn(Long reservationId) {
        // 1. 查记录
        Reservation r = this.getById(reservationId);
        if (r == null) {
            Result res = Result.fail();
            res.setMsg("预约记录不存在");
            return res;
        }

        // 只能在 “待签到” 状态下签到
        if (!"reserved".equals(r.getStatus())) {
            Result res = Result.fail();
            res.setMsg("当前状态不允许签到");
            return res;
        }

        LocalDate date = r.getDate();          // 对应表里的 date 列
        LocalTime startTime = r.getStartTime();// 对应 start_time
        LocalTime endTime = r.getEndTime();    // 对应 end_time

        if (date == null || startTime == null || endTime == null) {
            Result res = Result.fail();
            res.setMsg("预约记录的时间信息不完整");
            return res;
        }

        // 2. 计算时间窗口
        LocalDateTime start = LocalDateTime.of(date, startTime); // 预约开始时间
        LocalDateTime end   = LocalDateTime.of(date, endTime);   // 预约结束时间
        LocalDateTime now   = LocalDateTime.now();

        // 可签到区间：开始前 10 分钟 ~ 结束
        LocalDateTime earliest = start.minusMinutes(10); // 最早签到
        LocalDateTime lateLine = start.plusMinutes(10);  // 正常签到截止（开始后10分钟）

        // 还没到开始前 10 分钟
        if (now.isBefore(earliest)) {
            Result res = Result.fail();
            res.setMsg("未到签到时间，请在开始前 10 分钟内再尝试");
            return res;
        }

        // 已经超过结束时间：认定为未签到
        if (now.isAfter(end)) {
            r.setStatus("no_show");
            r.setCheckinTime(null);  // 没有签到时间
            this.updateById(r);

            Result res = Result.fail();
            res.setMsg("已超过本次预约时间，系统已将本次记录标记为未签到");
            return res;
        }

        // 3. 在可签到时间内：区分正常 / 迟到
        String newStatus;
        if (!now.isAfter(lateLine)) {
            // now <= start+10min -> 正常
            newStatus = "checked_in";
        } else {
            // start+10min ~ end -> 迟到
            newStatus = "late";
        }

        r.setStatus(newStatus);
        r.setCheckinTime(now);
        this.updateById(r);

        String msg = "签到成功";
        if ("late".equals(newStatus)) {
            msg = "签到成功（已标记为迟到）";
        }

        // 你自己的 Result 不是泛型，这里用 success(Object, Long) 然后改 msg
        Result res = Result.success(null, 0L);
        res.setMsg(msg);
        return res;
    }

    @Override
    public Result cancel(Long reservationId) {
        // 1. 查询预约记录
        Reservation r = this.getById(reservationId);
        if (r == null) {
            Result res = Result.fail();
            res.setMsg("预约记录不存在");
            return res;
        }

        // 2. 只有待签到（reserved）的记录才允许取消
        if (!"reserved".equals(r.getStatus())) {
            Result res = Result.fail();
            res.setMsg("当前状态不能取消");
            return res;
        }

        // 3. 计算时间点
        LocalDate date = r.getDate();          // 预约日期
        LocalTime startTime = r.getStartTime();// 开始时间，例如 12:00:00

        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime now = LocalDateTime.now();

        // 3.1 距离开始时间 10 分钟以内，禁止取消
        LocalDateTime latestCancelTime = startDateTime.minusMinutes(10);
        if (!now.isBefore(latestCancelTime)) {
            Result res = Result.fail();
            res.setMsg("距离开始时间不足 10 分钟，无法取消预约");
            return res;
        }

        // 3.2 正常取消 vs 逾期取消
        LocalDateTime normalCancelDeadline = startDateTime.minusDays(1);
        String newStatus;
        if (now.isBefore(normalCancelDeadline)) {
            // 提前一天及以上取消 -> 正常取消
            newStatus = "cancelled";
        } else {
            // 提前不足一天，但还没到开始前 10 分钟 -> 逾期取消
            newStatus = "cancel_overdue";
        }

        // 4. 更新状态
        r.setStatus(newStatus);
        r.setUpdateTime(LocalDateTime.now());
        this.updateById(r);

        // 这里如果以后接了 seat 表，可以在这里把座位释放掉
        // TODO: 释放座位逻辑（seat表没接起来先不写，否则会编译错误）

        Result res = Result.success(null, 0L);
        if ("cancelled".equals(newStatus)) {
            res.setMsg("取消成功");
        } else {
            res.setMsg("取消成功（已记录为逾期取消）");
        }
        return res;
    }

    @Override
    @Transactional
    public int markExpiredAsNoShow(Integer userId) {
        if (userId == null) {
            return 0;
        }

        // 当前时间（用一次就够了）
        LocalDateTime now = LocalDateTime.now();

        // 先把这个用户所有“待签到”的预约都查出来
        List<Reservation> allReserved = this.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .eq(Reservation::getStatus, "reserved")
        );

        if (allReserved.isEmpty()) {
            return 0;
        }

        List<Reservation> needUpdate = new ArrayList<>();
        for (Reservation r : allReserved) {
            LocalDate d = r.getDate();
            LocalTime end = r.getEndTime();
            if (d == null || end == null) {
                continue;
            }

            // 该条预约的“结束时间点”
            LocalDateTime endDateTime = LocalDateTime.of(d, end);

            // 如果结束时间 <= 当前时间 ⇒ 已经过期，还没签到 ⇒ 标记为未签到
            if (!endDateTime.isAfter(now)) {
                r.setStatus("no_show");
                r.setCheckinTime(null);
                r.setUpdateTime(now);
                needUpdate.add(r);
            }
        }

        if (needUpdate.isEmpty()) {
            return 0;
        }

        this.updateBatchById(needUpdate);
        return needUpdate.size();
    }
}