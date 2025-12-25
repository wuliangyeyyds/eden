package com.ssrms.controller.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理员端：座位布局 VO
 * - rows：按 rowNo 分组，每一行 seats 数量可变（用于“铺满不留空”）
 */
@Data
public class SeatLayoutVO {

    private RoomBrief room;
    private List<SeatRow> rows;
    private SeatStats stats;

    @Data
    public static class RoomBrief {
        private Integer id;
        private String campus;
        private String building;
        private String roomName;
        private Integer totalSeats;
        private Integer openSeats;
        private String status;
    }

    @Data
    public static class SeatStats {
        private Integer free;
        private Integer occupied;
        private Integer disabled;
    }

    @Data
    public static class SeatRow {
        private Integer rowNo;
        private List<SeatItem> seats;
    }

    @Data
    public static class SeatItem {
        private Integer id;
        /** seat 表 seat_no 原值 */
        private String seatNo;
        /** 展示值：row-col */
        private String displayNo;
        /** free / occupied / disabled */
        private String status;
    }
}
