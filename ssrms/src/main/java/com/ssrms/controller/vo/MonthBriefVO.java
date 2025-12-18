package com.ssrms.controller.vo;

import lombok.Data;

@Data
public class MonthBriefVO {
    private Long monthReserveCount;  // 本月累计预约（已扣除取消）
    private Long studyMinutes;       // 本月累计自习时长（分钟）
    private String lastVisitTime;    // 最近一次到馆时间（yyyy-MM-dd HH:mm）

    public MonthBriefVO(Long monthReserveCount, Long studyMinutes, String lastVisitTime) {
        this.monthReserveCount = monthReserveCount;
        this.studyMinutes = studyMinutes;
        this.lastVisitTime = lastVisitTime;
    }
}