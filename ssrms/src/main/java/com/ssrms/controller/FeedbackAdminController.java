package com.ssrms.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.FeedbackHandleDTO;
import com.ssrms.controller.vo.FeedbackAdminVO;
import com.ssrms.entity.Feedback;
import com.ssrms.entity.Reservation;
import com.ssrms.entity.Room;
import com.ssrms.entity.Seat;
import com.ssrms.entity.User;
import com.ssrms.service.FeedbackService;
import com.ssrms.service.ReservationService;
import com.ssrms.service.RoomService;
import com.ssrms.service.SeatService;
import com.ssrms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员端：投诉/反馈处理
 *
 * 关键修复：
 * 1) 兼容 Feedback.userId 在实体里可能是 Long / Integer（你项目里 Feedback::getUserId 返回 Long）
 * 2) 修复“lambda 引用的本地变量必须是 final 或 effectively final”的编译问题：
 *    - 不在 lambda 外对 Long 变量做二次赋值，全部在进入 wrapper 之前计算出 final 变量
 */
@RestController
@RequestMapping("/feedback/admin")
public class FeedbackAdminController {

    @Autowired private FeedbackService feedbackService;
    @Autowired private UserService userService;
    @Autowired private ReservationService reservationService;
    @Autowired private RoomService roomService;
    @Autowired private SeatService seatService;

    private static Long parseLongOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return null; }
    }

    @GetMapping("/page")
    public Result page(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String keyword) {

        int pn = Math.max(1, pageNum);
        int ps = Math.max(1, Math.min(50, pageSize));

        final String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        final Long kwLong = parseLongOrNull(kw);

        // keyword 命中 user(name/studentNo/account) -> 取出这些用户的 id（转 Long，兼容 feedback.userId 为 Long）
        final Set<Long> hitUserIdsForFb = new HashSet<>();
        if (StringUtils.hasText(kw)) {
            List<User> users = userService.list(
                    Wrappers.<User>lambdaQuery()
                            .and(w -> w.like(User::getName, kw)
                                    .or().like(User::getStudentNo, kw)
                                    .or().like(User::getAccount, kw))
            );
            if (users != null && !users.isEmpty()) {
                hitUserIdsForFb.addAll(
                        users.stream()
                                .map(User::getId)
                                .filter(Objects::nonNull)
                                .map(id -> ((Number) id).longValue())
                                .collect(Collectors.toSet())
                );
            }
        }

        Page<Feedback> page = feedbackService.page(
                new Page<>(pn, ps),
                Wrappers.<Feedback>lambdaQuery()
                        .eq(StringUtils.hasText(status), Feedback::getStatus, status)
                        .eq(StringUtils.hasText(category), Feedback::getCategory, category)
                        .and(StringUtils.hasText(kw), w -> {
                            // 内容模糊匹配
                            w.like(Feedback::getContent, kw);

                            // 数字关键字：id/reservationId/userId 精确匹配（用 Long 更稳）
                            if (kwLong != null) {
                                w.or().eq(Feedback::getId, kwLong)
                                        .or().eq(Feedback::getReservationId, kwLong)
                                        .or().eq(Feedback::getUserId, kwLong);
                            }

                            // 命中用户列表：userId in (...)
                            if (!hitUserIdsForFb.isEmpty()) {
                                w.or().in(Feedback::getUserId, hitUserIdsForFb);
                            }
                        })
                        .orderByDesc(Feedback::getCreateTime)
        );

        List<Feedback> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return Result.success(Collections.emptyList(), 0L);
        }

        // feedback.userId -> Long 集合（兼容 Long/Integer）
        Set<Long> fbUserIdsLong = records.stream()
                .map(Feedback::getUserId)
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toSet());

        // user 表主键是 Integer：再转成 Integer 批量查 user
        Set<Integer> userIdsInt = fbUserIdsLong.stream().map(Long::intValue).collect(Collectors.toSet());
        Map<Integer, User> userMap = userIdsInt.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIdsInt).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // reservations
        Set<Long> reservationIds = records.stream()
                .map(Feedback::getReservationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Reservation> resMap = reservationIds.isEmpty()
                ? Collections.emptyMap()
                : reservationService.listByIds(reservationIds).stream()
                .collect(Collectors.toMap(Reservation::getId, r -> r, (a, b) -> a));

        // rooms（来自 feedback.roomId 或 reservation.roomId）
        Set<Integer> roomIds = new HashSet<>();
        records.stream()
                .map(Feedback::getRoomId)
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).intValue())
                .forEach(roomIds::add);

        resMap.values().stream()
                .map(Reservation::getRoomId)
                .filter(Objects::nonNull)
                .forEach(roomIds::add);

        Map<Integer, Room> roomMap = roomIds.isEmpty()
                ? Collections.emptyMap()
                : roomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, r -> r, (a, b) -> a));

        // seats（用于 seatNo）
        Set<Integer> seatIds = resMap.values().stream()
                .map(Reservation::getSeatId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Seat> seatMap = seatIds.isEmpty()
                ? Collections.emptyMap()
                : seatService.listByIds(seatIds).stream()
                .collect(Collectors.toMap(Seat::getId, s -> s, (a, b) -> a));

        List<FeedbackAdminVO> voList = records.stream().map(fb -> {
            FeedbackAdminVO vo = new FeedbackAdminVO();
            vo.setId(fb.getId());

            Integer uidInt = fb.getUserId() == null ? null : ((Number) fb.getUserId()).intValue();
            vo.setUserId(uidInt);

            Integer roomIdInt = fb.getRoomId() == null ? null : ((Number) fb.getRoomId()).intValue();
            vo.setRoomId(roomIdInt);

            vo.setReservationId(fb.getReservationId());
            vo.setCategory(fb.getCategory());
            vo.setRating(fb.getRating());
            vo.setContent(fb.getContent());
            vo.setStatus(fb.getStatus());
            vo.setReply(fb.getReply());
            vo.setCreateTime(fb.getCreateTime());
            vo.setUpdateTime(fb.getUpdateTime());

            User u = (uidInt == null) ? null : userMap.get(uidInt);
            if (u != null) {
                vo.setUserName(u.getName());
                vo.setStudentNo(u.getStudentNo());
            }

            // roomLabel：优先 feedback.roomId，否则用 reservation.roomId
            Integer rid = roomIdInt;
            Reservation rr = (fb.getReservationId() == null) ? null : resMap.get(fb.getReservationId());
            if (rid == null && rr != null) rid = rr.getRoomId();
            if (rid != null) {
                Room room = roomMap.get(rid);
                if (room != null) {
                    vo.setRoomLabel(room.getCampus() + " · " + room.getBuilding() + " " + room.getRoomName());
                }
            }

            // reservationLabel
            if (rr != null) {
                String date = rr.getDate() == null ? "" : rr.getDate().toString();
                String st = rr.getStartTime() == null ? "" : rr.getStartTime().toString().substring(0, 5);
                String et = rr.getEndTime() == null ? "" : rr.getEndTime().toString().substring(0, 5);

                String roomLabel = vo.getRoomLabel();
                if (!StringUtils.hasText(roomLabel) && rr.getRoomId() != null) {
                    Room room = roomMap.get(rr.getRoomId());
                    if (room != null) roomLabel = room.getCampus() + " · " + room.getBuilding() + " " + room.getRoomName();
                }

                String seatNo = null;
                if (rr.getSeatId() != null) {
                    Seat seat = seatMap.get(rr.getSeatId());
                    if (seat != null) seatNo = seat.getSeatNo();
                }
                String seatHint = StringUtils.hasText(seatNo) ? (" · 座位 " + seatNo) : "";
                vo.setReservationLabel(date + " " + st + "-" + et + " · " + (StringUtils.hasText(roomLabel) ? roomLabel : "未知场地") + seatHint);
            }

            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList, page.getTotal());
    }

    @GetMapping("/stats")
    public Result stats() {
        long pending = feedbackService.count(Wrappers.<Feedback>lambdaQuery().eq(Feedback::getStatus, "pending"));
        long processing = feedbackService.count(Wrappers.<Feedback>lambdaQuery().eq(Feedback::getStatus, "processing"));
        long resolved = feedbackService.count(Wrappers.<Feedback>lambdaQuery().eq(Feedback::getStatus, "resolved"));

        Map<String, Object> map = new HashMap<>();
        map.put("pending", pending);
        map.put("processing", processing);
        map.put("resolved", resolved);
        return Result.success(map);
    }

    @PostMapping("/handle")
    @Transactional
    public Result handle(@RequestBody FeedbackHandleDTO dto) {
        if (dto == null || dto.getId() == null) return Result.fail("缺少 id");

        Feedback fb = feedbackService.getById(dto.getId());
        if (fb == null) return Result.fail("反馈不存在");

        // 取消关联预约：管理员强制取消（不走学生端 10 分钟规则）
        if (Boolean.TRUE.equals(dto.getCancelReservation())) {
            Long rid = fb.getReservationId();
            if (rid == null) return Result.fail("该反馈无关联预约，无法取消");

            Reservation r = reservationService.getById(rid);
            if (r == null) return Result.fail("关联预约不存在");
            if (!"reserved".equals(r.getStatus())) {
                return Result.fail("关联预约当前状态不能取消：" + r.getStatus());
            }
            r.setStatus("cancelled");
            r.setUpdateTime(LocalDateTime.now());
            reservationService.updateById(r);
        }

        if (dto.getStatus() != null) fb.setStatus(dto.getStatus());
        if (dto.getReply() != null) fb.setReply(dto.getReply());

        feedbackService.updateById(fb);
        return Result.successWithMsg("处理成功");
    }
}
