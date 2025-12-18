package com.ssrms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateSeatReservationDTO;
import com.ssrms.controller.vo.MyViolationVO;
import com.ssrms.entity.*;
import com.ssrms.mapper.ReservationMapper;
import com.ssrms.mapper.UserMapper;
import com.ssrms.mapper.ViolationMapper;
import com.ssrms.service.ReservationService;
import com.ssrms.service.RoomService;
import com.ssrms.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl
        extends ServiceImpl<ReservationMapper, Reservation>
        implements ReservationService {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private ViolationMapper violationMapper;

    @Autowired
    private UserMapper userMapper;

    private static final int PENALTY_LATE = 1;
    private static final int PENALTY_NO_SHOW = 2;

    // =========================
    // 1) 签到：状态更新 +（必要时）违规落库/扣分
    // =========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result checkIn(Long reservationId) {
        Reservation r = this.getById(reservationId);
        if (r == null) return Result.fail("预约记录不存在");
        if (!"reserved".equals(r.getStatus())) return Result.fail("当前状态不允许签到");

        LocalDate date = r.getDate();
        LocalTime startTime = r.getStartTime();
        LocalTime endTime = r.getEndTime();
        if (date == null || startTime == null || endTime == null) return Result.fail("预约记录的时间信息不完整");

        LocalDateTime start = LocalDateTime.of(date, startTime);
        LocalDateTime end = LocalDateTime.of(date, endTime);
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime earliest = start.minusMinutes(10);
        LocalDateTime lateLine = start.plusMinutes(10);

        if (now.isBefore(earliest)) {
            return Result.fail("未到签到时间，请在开始前 10 分钟内再尝试");
        }

        // 超过结束时间：直接 no_show + 违规落库/扣分（防重复）
        if (now.isAfter(end)) {
            r.setStatus("no_show");
            r.setCheckinTime(null);
            r.setUpdateTime(now);
            this.updateById(r);

            recordViolationIfAbsent(r); // ✅统一入口

            Result res = Result.success(null, 0L);
            res.setMsg("已超过本次预约时间，系统已将本次记录标记为未签到");
            return res;
        }

        String newStatus = (!now.isAfter(lateLine)) ? "checked_in" : "late";

        r.setStatus(newStatus);
        r.setCheckinTime(now);
        r.setUpdateTime(now);
        this.updateById(r);

        if ("late".equals(newStatus)) {
            recordViolationIfAbsent(r); // ✅统一入口
            Result res = Result.success(null, 0L);
            res.setMsg("签到成功（已标记为迟到）");
            return res;
        }

        Result res = Result.success(null, 0L);
        res.setMsg("签到成功");
        return res;
    }

    // =========================
    // 2) 取消：保持你原逻辑
    // =========================
    @Override
    public Result cancel(Long reservationId) {
        Reservation r = this.getById(reservationId);
        if (r == null) return Result.fail("预约记录不存在");
        if (!"reserved".equals(r.getStatus())) return Result.fail("当前状态不能取消");

        LocalDate date = r.getDate();
        LocalTime startTime = r.getStartTime();
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime latestCancelTime = startDateTime.minusMinutes(10);
        if (now.isAfter(latestCancelTime)) {
            return Result.fail("距离开始时间不足 10 分钟，无法取消预约");
        }

        LocalDateTime normalCancelDeadline = startDateTime.minusDays(1);
        String newStatus = now.isBefore(normalCancelDeadline) ? "cancelled" : "cancel_overdue";

        r.setStatus(newStatus);
        r.setUpdateTime(LocalDateTime.now());
        this.updateById(r);

        Result res = Result.success(null, 0L);
        res.setMsg("cancelled".equals(newStatus) ? "取消成功" : "取消成功（已记录为逾期取消）");
        return res;
    }

    // =========================
    // 3) 刷新 no_show：只负责把 reserved->no_show，并复用统一违规入口
    // =========================
    @Override
    @Transactional
    public int markExpiredAsNoShow(Integer userId) {
        if (userId == null) return 0;

        LocalDateTime now = LocalDateTime.now();

        List<Reservation> allReserved = this.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .eq(Reservation::getStatus, "reserved")
        );
        if (allReserved.isEmpty()) return 0;

        List<Reservation> needUpdate = new ArrayList<>();
        for (Reservation r : allReserved) {
            LocalDate d = r.getDate();
            LocalTime end = r.getEndTime();
            if (d == null || end == null) continue;

            LocalDateTime endDateTime = LocalDateTime.of(d, end);
            if (!endDateTime.isAfter(now)) {
                r.setStatus("no_show");
                r.setCheckinTime(null);
                r.setUpdateTime(now);
                needUpdate.add(r);
            }
        }

        if (needUpdate.isEmpty()) return 0;

        this.updateBatchById(needUpdate);

        // ✅统一入口：对新变成 no_show 的记录补写 violation/扣分（防重复）
        for (Reservation r : needUpdate) {
            recordViolationIfAbsent(r);
        }

        return needUpdate.size();
    }

    // =========================
    // 4) 违规记录：整洁版（核心）
    // =========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result myViolations(Integer userId) {
        if (userId == null) return Result.fail("userId不能为空");

        // 1) 先刷新 no_show（本方法内部已统一落库/扣分）
        markExpiredAsNoShow(userId);

        // 2) 把该用户所有 late/no_show 的预约，补写到 violation（防重复 + 防重复扣分）
        syncViolationsFromReservations(userId);

        // 3) 以 violation 表为权威来源返回
        List<Violation> vList = violationMapper.selectList(
                Wrappers.<Violation>lambdaQuery()
                        .eq(Violation::getUserId, userId)
                        .orderByDesc(Violation::getVDate)
                        .orderByDesc(Violation::getCreateTime)
        );

        if (vList == null || vList.isEmpty()) {
            return Result.success(Collections.emptyList(), 0L);
        }

        // 4) 批量补齐 reservation / room / seat 信息
        Set<Long> reservationIds = vList.stream()
                .map(Violation::getReservationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Reservation> reservationMap = reservationIds.isEmpty()
                ? Collections.emptyMap()
                : this.listByIds(reservationIds).stream()
                .collect(Collectors.toMap(Reservation::getId, x -> x, (a, b) -> a));

        Set<Integer> roomIds = vList.stream()
                .map(Violation::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Room> roomMap = roomIds.isEmpty()
                ? Collections.emptyMap()
                : roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, x -> x, (a, b) -> a));

        Set<Integer> seatIds = reservationMap.values().stream()
                .map(Reservation::getSeatId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> seatNoMap = seatIds.isEmpty()
                ? Collections.emptyMap()
                : seatService.listByIds(seatIds).stream()
                .collect(Collectors.toMap(Seat::getId, Seat::getSeatNo, (a, b) -> a));

        List<MyViolationVO> voList = new ArrayList<>();
        for (Violation v : vList) {
            MyViolationVO vo = new MyViolationVO();

            Long rid = v.getReservationId();
            vo.setReservationId(rid != null ? rid : v.getId()); // 极端情况兜底
            vo.setDate(v.getVDate());

            Reservation r = (rid == null) ? null : reservationMap.get(rid);
            if (r != null) {
                vo.setStartTime(r.getStartTime());
                vo.setEndTime(r.getEndTime());
                Integer seatId = r.getSeatId();
                vo.setSeatNo(seatId == null ? "未指定" : seatNoMap.getOrDefault(seatId, "未指定"));
            } else {
                vo.setSeatNo("未指定");
            }

            Room room = v.getRoomId() == null ? null : roomMap.get(v.getRoomId());
            if (room != null) {
                vo.setCampus(room.getCampus());
                vo.setBuilding(room.getBuilding());
                vo.setRoomName(room.getRoomName());
                vo.setRoomLabel(room.getCampus() + " · " + room.getBuilding() + " " + room.getRoomName());
            } else {
                vo.setCampus("-");
                vo.setBuilding("-");
                vo.setRoomName("-");
                vo.setRoomLabel("未知场地");
            }

            ViolationMeta meta = metaFromVType(v.getVType());
            vo.setViolationType(meta.cnType);
            vo.setPenaltyScore(-Math.abs(v.getPenaltyScore() == null ? meta.penalty : v.getPenaltyScore()));
            vo.setRemark((v.getDescription() == null || v.getDescription().isBlank()) ? meta.remark : v.getDescription());

            voList.add(vo);
        }

        return Result.success(voList, (long) voList.size());
    }

    // 把 late/no_show 的预约补写 violation（历史补偿）
    private int syncViolationsFromReservations(Integer userId) {
        List<Reservation> list = this.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .in(Reservation::getStatus, Arrays.asList("late", "no_show"))
        );
        if (list == null || list.isEmpty()) return 0;

        int inserted = 0;
        for (Reservation r : list) {
            if (recordViolationIfAbsent(r)) inserted++;
        }
        return inserted;
    }

    /**
     * ✅统一入口：只对 late/no_show 生效
     * - violation 已存在：不重复插入，不重复扣分
     * - violation 不存在：插入 violation，并扣 credit_score
     */
    private boolean recordViolationIfAbsent(Reservation r) {
        if (r == null || r.getUserId() == null || r.getId() == null) return false;

        ViolationMeta meta = metaFromReservationStatus(r.getStatus());
        if (meta == null) return false;

        Long cnt = violationMapper.selectCount(
                Wrappers.<Violation>lambdaQuery()
                        .eq(Violation::getUserId, r.getUserId())
                        .eq(Violation::getReservationId, r.getId())
                        .eq(Violation::getVType, meta.vType)
        );
        if (cnt != null && cnt > 0) return false;

        Violation v = new Violation();
        v.setUserId(r.getUserId());
        v.setReservationId(r.getId());
        v.setVType(meta.vType);
        v.setVDate(r.getDate());
        v.setRoomId(r.getRoomId());
        v.setDescription(meta.remark);
        v.setPenaltyScore(meta.penalty); // ✅正数
        v.setHandled(0);
        v.setCreateTime(LocalDateTime.now());
        violationMapper.insert(v);

        // ✅只在“插入成功”时扣分（防重复扣）
        userMapper.update(
                null,
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, r.getUserId())
                        .setSql("credit_score = GREATEST(0, credit_score - " + meta.penalty + ")")
                        .set(User::getUpdateTime, LocalDateTime.now())
        );

        return true;
    }

    // --- 违规元信息（避免散落 if/else） ---
    private static class ViolationMeta {
        final String vType;
        final String cnType;
        final int penalty;
        final String remark;

        ViolationMeta(String vType, String cnType, int penalty, String remark) {
            this.vType = vType;
            this.cnType = cnType;
            this.penalty = penalty;
            this.remark = remark;
        }
    }

    private ViolationMeta metaFromReservationStatus(String status) {
        if ("no_show".equals(status)) {
            return new ViolationMeta("no_show", "未签到", PENALTY_NO_SHOW, "到结束时间仍未签到");
        }
        if ("late".equals(status)) {
            return new ViolationMeta("late", "迟到", PENALTY_LATE, "超过开始时间较久后才签到");
        }
        return null;
    }

    private ViolationMeta metaFromVType(String vType) {
        if ("no_show".equals(vType)) {
            return new ViolationMeta("no_show", "未签到", PENALTY_NO_SHOW, "到结束时间仍未签到");
        }
        if ("late".equals(vType)) {
            return new ViolationMeta("late", "迟到", PENALTY_LATE, "超过开始时间较久后才签到");
        }
        return new ViolationMeta(vType, vType, 0, "未知类型");
    }

    // =========================
    // 5) 创建预约：保持你原逻辑（不改）
    // =========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createSeatReservations(CreateSeatReservationDTO dto) {
        if (dto == null
                || dto.getUserId() == null
                || dto.getRoomId() == null
                || dto.getDate() == null
                || dto.getStartTime() == null
                || dto.getEndTime() == null
                || dto.getSeatNos() == null
                || dto.getSeatNos().isEmpty()) {
            return Result.fail("参数不完整");
        }

        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            return Result.fail("时间段不合法");
        }

        if (dto.getSeatNos().size() > 4) {
            return Result.fail("一次最多预约 4 个座位");
        }

        List<String> seatNos = dto.getSeatNos().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(this::normalizeSeatNo)
                .distinct()
                .collect(Collectors.toList());

        if (seatNos.size() != dto.getSeatNos().size()) {
            return Result.fail("座位号重复");
        }

        List<Seat> seats = seatService.list(
                Wrappers.<Seat>lambdaQuery()
                        .eq(Seat::getRoomId, dto.getRoomId())
                        .in(Seat::getSeatNo, seatNos)
                        .eq(Seat::getStatus, "enabled")
        );

        if (seats.size() != seatNos.size()) {
            Set<String> found = seats.stream().map(Seat::getSeatNo).collect(Collectors.toSet());
            List<String> missing = seatNos.stream().filter(s -> !found.contains(s)).toList();
            return Result.fail("座位不存在或不可用: " + String.join(",", missing));
        }

        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(dto.getStartTime(), dto.getEndTime());
        if (minutes <= 0) return Result.fail("时间段不合法");
        if (minutes > 240) return Result.fail("单次最多预约 4 小时");

        for (Seat seat : seats) {
            List<Reservation> conflicts = this.list(
                    Wrappers.<Reservation>lambdaQuery()
                            .eq(Reservation::getSeatId, seat.getId())
                            .eq(Reservation::getDate, dto.getDate())
                            .notIn(Reservation::getStatus, Arrays.asList("cancelled", "cancel_overdue"))
                            .lt(Reservation::getStartTime, dto.getEndTime())
                            .gt(Reservation::getEndTime, dto.getStartTime())
                            .orderByAsc(Reservation::getStartTime)
            );

            if (!conflicts.isEmpty()) {
                List<String> lines = conflicts.stream()
                        .map(r -> String.format("座位 %s：%s 至 %s 已存在用户预约",
                                seat.getSeatNo(),
                                r.getStartTime().toString(),
                                r.getEndTime().toString()))
                        .distinct()
                        .toList();
                return Result.fail(String.join("\n", lines));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> toSave = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);

            Reservation r = new Reservation();
            r.setReservationNo(genReservationNo(dto.getUserId(), i));
            r.setUserId(dto.getUserId());
            r.setRoomId(dto.getRoomId());
            r.setSeatId(seat.getId());
            r.setDate(dto.getDate());
            r.setStartTime(dto.getStartTime());
            r.setEndTime(dto.getEndTime());
            r.setStatus("reserved");
            r.setCreateTime(now);
            r.setUpdateTime(now);

            toSave.add(r);
        }

        this.saveBatch(toSave);

        List<String> reservationNos = toSave.stream()
                .map(Reservation::getReservationNo)
                .toList();

        return Result.success(reservationNos);
    }

    private String normalizeSeatNo(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.matches("\\d+")) {
            int n = Integer.parseInt(t);
            return String.format("%02d", n);
        }
        return t;
    }

    private String genReservationNo(Integer userId, int idx) {
        long ts = System.currentTimeMillis();
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "R" + userId + "-" + ts + "-" + idx + "-" + rnd;
    }

    @Override
    public Result seatConflicts(Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (roomId == null || date == null || startTime == null || endTime == null) {
            return Result.success(Collections.emptyMap());
        }

        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
        if (minutes <= 0) return Result.success(Collections.emptyMap());
        if (minutes > 240) return Result.success(Collections.emptyMap()); // 超过4小时，前端本来就不该让选

        // seatId -> seatNo
        Map<Integer, String> seatNoMap = seatService.list(
                Wrappers.<Seat>lambdaQuery().eq(Seat::getRoomId, roomId)
        ).stream().collect(Collectors.toMap(Seat::getId, Seat::getSeatNo, (a, b) -> a));

        // 查出该房间、该日期、与所选区间有重叠的预约
        List<Reservation> list = this.list(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getRoomId, roomId)
                        .eq(Reservation::getDate, date)
                        .notIn(Reservation::getStatus, Arrays.asList("cancelled", "cancel_overdue"))
                        .lt(Reservation::getStartTime, endTime)
                        .gt(Reservation::getEndTime, startTime)
                        .orderByAsc(Reservation::getSeatId)
                        .orderByAsc(Reservation::getStartTime)
        );

        // seatNo -> [{startTime,endTime}]
        Map<String, List<Map<String, String>>> map = new HashMap<>();
        for (Reservation r : list) {
            String seatNo = seatNoMap.get(r.getSeatId());
            if (seatNo == null) continue;

            Map<String, String> one = new HashMap<>();
            one.put("startTime", r.getStartTime().toString());
            one.put("endTime", r.getEndTime().toString());

            map.computeIfAbsent(seatNo, k -> new ArrayList<>()).add(one);
        }

        return Result.success(map);
    }
}