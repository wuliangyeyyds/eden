package com.ssrms.controller.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SlotStatusDTO {

    private Integer roomId;

    private LocalDate date;

    /** 被占满的时段 id 列表，例如 [8, 9, 14] 表示 8:00-9:00 等不可预约 */
    private List<Integer> disabledSlotIds;
}
