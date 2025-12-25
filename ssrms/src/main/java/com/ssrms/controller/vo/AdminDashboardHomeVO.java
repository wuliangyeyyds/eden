package com.ssrms.controller.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理员首页看板数据
 * 适配 AdminHome.vue 的 metrics / trendItems / latestActivities 使用方式
 */
@Data
public class AdminDashboardHomeVO {

    /** 顶部四张卡片中的三张（待处理事项暂不接后端） */
    private Metrics metrics;

    /** 本周运行概览 */
    private Weekly weekly;

    /** 最近预约动态 */
    private List<Activity> latestActivities;

    @Data
    public static class Metrics {
        /** 今日预约总数（排除取消类状态） */
        private Integer todayReservations;

        /** 与昨日相比的变化百分比（可正可负，单位：%） */
        private Integer todayTrend;

        /** 今日签到率（0-100）：(checked_in + late) / 今日预约总数 */
        private Integer todaySignRate;

        /** 当前开放自习室数 */
        private Integer openRooms;

        /** 覆盖座位数：默认统计开放自习室的 open_seats 之和 */
        private Integer totalSeats;
    }

    @Data
    public static class Weekly {
        /** 本周预约完成率（0-100）：(checked_in + late) / 本周预约总数 */
        private Integer weekReservationRate;

        /** 本周平均签到率（0-100）：checked_in / 本周预约总数 */
        private Integer weekAvgCheckinRate;

        /** 本周违约率（0-100）：(late + no_show) / 本周预约总数 */
        private Integer weekViolationRate;
    }

    @Data
    public static class Activity {
        private Long id;
        /** 展示文案 */
        private String text;
        /** book / sign / cancel / violation（用于页面 class） */
        private String type;
        /** 新预约 / 已签到 / 已取消 / 违约（用于页面 tag 文案） */
        private String typeLabel;
        /** 时间 HH:mm */
        private String time;
    }
}
