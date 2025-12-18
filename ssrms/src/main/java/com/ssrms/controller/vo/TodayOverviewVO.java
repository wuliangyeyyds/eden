package com.ssrms.controller.vo;

import lombok.Data;

@Data
public class TodayOverviewVO {
    private Integer totalSeats;      // 总座位数（固定 24000）
    private Integer reservedCount;   // 已预约（当前时刻）
    private Integer inUseCount;      // 正在使用（当前时刻）
    private Integer remainingCount;  // 剩余（当前时刻）

    public TodayOverviewVO(Integer totalSeats, Integer reservedCount, Integer inUseCount, Integer remainingCount) {
        this.totalSeats = totalSeats;
        this.reservedCount = reservedCount;
        this.inUseCount = inUseCount;
        this.remainingCount = remainingCount;
    }
}