package com.ssrms.service.impl;

import com.ssrms.controller.vo.AdminDashboardHomeVO;
import com.ssrms.mapper.AdminDashboardMapper;
import com.ssrms.service.AdminDashboardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final String ST_RESERVED = "reserved";
    private static final String ST_CHECKED_IN = "checked_in";
    private static final String ST_LATE = "late";
    private static final String ST_NO_SHOW = "no_show";
    private static final String ST_CANCELLED = "cancelled";
    private static final String ST_CANCEL_OVERDUE = "cancel_overdue";

    @Resource
    private AdminDashboardMapper adminDashboardMapper;

    private static int rate(int numerator, int denominator) {
        if (denominator <= 0) return 0;
        double v = (numerator * 100.0) / denominator;
        return (int) Math.round(v);
    }

    private static String safe(String s) {
        return StringUtils.hasText(s) ? s.trim() : "";
    }

    @Override
    public AdminDashboardHomeVO buildHome(int recentLimit) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // ===== 今日概览 =====
        int todayTotal = adminDashboardMapper.countTodayReservations(today);
        int todayChecked = adminDashboardMapper.countTodayCheckedIn(today);

        int yesterdayTotal = adminDashboardMapper.countReservationsByDayExcludingCancelled(yesterday);

        int trend;
        if (yesterdayTotal <= 0) {
            trend = (todayTotal <= 0) ? 0 : 100;
        } else {
            trend = (int) Math.round(((todayTotal - yesterdayTotal) * 100.0) / yesterdayTotal);
        }

        int openRooms = adminDashboardMapper.countOpenRooms();
        Integer openSeatsSum = adminDashboardMapper.sumOpenRoomSeats();
        int totalSeats = openSeatsSum == null ? 0 : openSeatsSum;

        AdminDashboardHomeVO.Metrics metrics = new AdminDashboardHomeVO.Metrics();
        metrics.setTodayReservations(todayTotal);
        metrics.setTodayTrend(trend);
        metrics.setTodaySignRate(rate(todayChecked, todayTotal));
        metrics.setOpenRooms(openRooms);
        metrics.setTotalSeats(totalSeats);

        // ===== 本周概览（周一 ~ 下周一）=====
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);

        int weekTotal = adminDashboardMapper.countWeekTotal(weekStart, weekEnd);
        int weekCheckedIn = adminDashboardMapper.countWeekByStatus(weekStart, weekEnd, ST_CHECKED_IN);
        int weekLate = adminDashboardMapper.countWeekByStatus(weekStart, weekEnd, ST_LATE);
        int weekNoShow = adminDashboardMapper.countWeekByStatus(weekStart, weekEnd, ST_NO_SHOW);

        int weekCompleted = weekCheckedIn + weekLate;
        int weekViolation = weekLate + weekNoShow;

        AdminDashboardHomeVO.Weekly weekly = new AdminDashboardHomeVO.Weekly();
        weekly.setWeekReservationRate(rate(weekCompleted, weekTotal));
        weekly.setWeekAvgCheckinRate(rate(weekCheckedIn, weekTotal));
        weekly.setWeekViolationRate(rate(weekViolation, weekTotal));

        // ===== 最近动态 =====
        int limit = Math.max(1, recentLimit);
        List<AdminDashboardMapper.RecentRow> rows = adminDashboardMapper.selectRecentRows(limit);

        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        List<AdminDashboardHomeVO.Activity> acts = new ArrayList<>();

        for (AdminDashboardMapper.RecentRow r : rows) {
            AdminDashboardHomeVO.Activity a = new AdminDashboardHomeVO.Activity();
            a.setId(r.reservationId);

            LocalDateTime t = (r.updateTime != null) ? r.updateTime : r.createTime;
            a.setTime(t == null ? "--:--" : t.format(hm));

            String userName = safe(r.userName);
            if (!StringUtils.hasText(userName)) userName = "某同学";

            String roomText = buildRoomText(r.campus, r.building, r.roomName);
            String slot = "";
            if (r.startTime != null && r.endTime != null) {
                slot = r.startTime.format(hm) + "-" + r.endTime.format(hm);
            }

            String st = safe(r.status);

            if (ST_CANCELLED.equals(st) || ST_CANCEL_OVERDUE.equals(st)) {
                a.setType("cancel");
                a.setTypeLabel("已取消");
                a.setText(userName + " 取消了 " + roomText + (slot.isEmpty() ? "" : (" · " + slot)));
            } else if (ST_CHECKED_IN.equals(st) || ST_LATE.equals(st)) {
                a.setType("sign");
                a.setTypeLabel("已签到");
                a.setText(userName + " 在 " + roomText + " 完成签到");
            } else if (ST_NO_SHOW.equals(st)) {
                a.setType("violation");
                a.setTypeLabel("违约");
                a.setText(userName + " 未签到 · " + roomText + (slot.isEmpty() ? "" : (" · " + slot)));
            } else {
                // reserved 或其他：默认算“新预约”
                a.setType("book");
                a.setTypeLabel("新预约");
                a.setText(userName + " 预约了 " + roomText + (slot.isEmpty() ? "" : (" · " + slot)));
            }

            acts.add(a);
        }

        AdminDashboardHomeVO out = new AdminDashboardHomeVO();
        out.setMetrics(metrics);
        out.setWeekly(weekly);
        out.setLatestActivities(acts);
        return out;
    }

    private String buildRoomText(String campus, String building, String roomName) {
        String c = safe(campus);
        String b = safe(building);
        String r = safe(roomName);

        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(c)) sb.append(c);
        if (StringUtils.hasText(b)) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(b);
        }
        if (StringUtils.hasText(r)) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(r);
        }
        if (sb.length() == 0) return "自习室";
        return sb.toString();
    }
}
