package com.ssrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ssrms.controller.dto.AiChatRequestDTO;
import com.ssrms.controller.vo.AiChatVO;
import com.ssrms.entity.Reservation;
import com.ssrms.entity.Room;
import com.ssrms.entity.Seat;
import com.ssrms.entity.User;
import com.ssrms.entity.Violation;
import com.ssrms.mapper.ReservationMapper;
import com.ssrms.mapper.RoomMapper;
import com.ssrms.mapper.SeatMapper;
import com.ssrms.mapper.UserMapper;
import com.ssrms.mapper.ViolationMapper;
import com.ssrms.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final CozeClient cozeClient;

    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final ViolationMapper violationMapper;
    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;

    @Override
    public AiChatVO chat(AiChatRequestDTO req) {
        if (req == null || req.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (!StringUtils.hasText(req.getMessage())) {
            throw new IllegalArgumentException("message 不能为空");
        }

        Integer userId = req.getUserId();
        User u = userMapper.selectById(userId);
        if (u == null) {
            return new AiChatVO("未找到该用户，请重新登录后再试。", req.getConversationId());
        }

        String prompt = buildPrompt(u, req.getMessage());

        CozeClient.CozeChatResult r = cozeClient.chatStream(String.valueOf(userId), req.getConversationId(), prompt);
        return new AiChatVO(r.getReply(), r.getConversationId());
    }

    private String buildPrompt(User u, String userQuestion) {
        LocalDate today = LocalDate.now();

        // 今日预约（最多 3 条）
        List<Reservation> todays = reservationMapper.selectList(
                new QueryWrapper<Reservation>()
                        .eq("user_id", u.getId())
                        .eq("date", today)
                        .orderByAsc("start_time")
                        .last("LIMIT 3")
        );

        // 最近违规（最多 3 条）
        List<Violation> vios = violationMapper.selectList(
                new QueryWrapper<Violation>()
                        .eq("user_id", u.getId())
                        .orderByDesc("create_time")
                        .last("LIMIT 3")
        );

        String creditState = calcCreditState(u);

        String todayResStr = todays.isEmpty() ? "（无）" : todays.stream().map(r -> {
            String roomName = "";
            String seatNo = "";
            if (r.getRoomId() != null) {
                Room room = roomMapper.selectById(r.getRoomId());
                if (room != null) {
                    roomName = String.format("%s-%s-%s",
                            safe(room.getCampus()), safe(room.getBuilding()), safe(room.getRoomName()));
                }
            }
            if (r.getSeatId() != null) {
                Seat seat = seatMapper.selectById(r.getSeatId());
                if (seat != null) seatNo = safe(seat.getSeatNo());
            }
            String slot = String.format("%s-%s", safe(r.getStartTime()), safe(r.getEndTime()));
            String where = (roomName.isEmpty() ? ("roomId=" + r.getRoomId()) : roomName);
            if (!seatNo.isEmpty()) where = where + " 座位" + seatNo;
            return String.format("- %s %s %s 状态=%s", today, slot, where, safe(r.getStatus()));
        }).collect(Collectors.joining("\n"));

        String vioStr = vios.isEmpty() ? "（无）" : vios.stream().map(v -> {
            String where = v.getRoomId() == null ? "" : ("roomId=" + v.getRoomId());
            String score = v.getPenaltyScore() == null ? "" : ("扣" + v.getPenaltyScore() + "分");
            return String.format("- %s %s %s %s %s",
                    safe(v.getVDate()),
                    safe(v.getVType()),
                    score,
                    where,
                    safe(v.getDescription())
            ).trim();
        }).collect(Collectors.joining("\n"));

        // 轻量版：把上下文直接塞给智能体（不做工具调用）
        return ""
                + "你是 SSRMS（自习室预约系统）的学生端智能助手。\n"
                + "请严格基于【系统上下文】回答【用户问题】。如果上下文没有相关信息，请明确说明并给出可执行的建议，但不要编造数据。\n"
                + "回答用中文，尽量简洁、可操作。\n\n"
                + "【系统上下文】\n"
                + "用户：" + safe(u.getName()) + "（学号：" + safe(u.getStudentNo()) + "）\n"
                + "信用分：" + safe(u.getCreditScore()) + "；信用状态：" + creditState + "\n"
                + "今日预约（最多3条）：\n" + todayResStr + "\n"
                + "最近违规（最多3条）：\n" + vioStr + "\n\n"
                + "【用户问题】\n"
                + userQuestion + "\n";
    }

    private String calcCreditState(User u) {
        Integer bl = u.getBlacklistFlag();
        Integer cs = u.getCreditScore();
        if (bl != null && bl == 1) return "黑名单";
        if (cs != null && cs < 80) return "预警";
        return "正常";
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
