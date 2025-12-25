package com.ssrms.controller.dto;

import lombok.Data;

/** 管理员新建自习室 */
@Data
public class CreateRoomDTO {
    private String campus;
    private String building;
    /** 房间号 / 自习室名，例如 301 / A101 */
    private String roomName;
    /** 座位总数 */
    private Integer totalSeats;
    /** 可选：备注 */
    private String remark;
    /** 可选：生成座位时每行列数（默认 8） */
    private Integer cols;
}
