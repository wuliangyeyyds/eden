package com.ssrms.controller.vo;

import lombok.Data;

/** 管理员房间列表行 */
@Data
public class AdminRoomRowVO {
    private Integer id;
    private String campus;
    private String building;
    private String roomName;

    /** 展示用名称：例如 3楼 301 自习室 */
    private String name;
    private Integer floor;

    /** 总座位数 */
    private Integer capacity;
    /** 开放座位数 */
    private Integer openSeats;
    /** open / closed / maintain */
    private String status;

    /** 当前时刻占用座位数（估算） */
    private Integer usedSeats;
}
