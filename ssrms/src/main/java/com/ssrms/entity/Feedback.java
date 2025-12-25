package com.ssrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户评价/投诉/建议/申诉（feedback 表）
 */
@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交人 */
    private Long userId;

    /** 可选：关联自习室（room.id），允许为空 */
    private Long roomId;

    /** 可选：关联预约（reservation.id），允许为空 */
    private Long reservationId;

    /** 类型：env / service / suggestion / complaint / appeal / other */
    private String category;

    /** 评分 1~5，可为空（非评价类可不打分） */
    private Integer rating;

    /** 内容 */
    private String content;

    /** 状态：pending / processing / resolved */
    private String status;

    /** 管理员回复 */
    private String reply;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
