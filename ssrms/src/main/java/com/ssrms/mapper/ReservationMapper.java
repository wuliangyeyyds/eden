package com.ssrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ssrms.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 今日：当前时刻“已预约”（reserved 且时间段覆盖当前时刻）
     * 注意：MyBatis-Plus 默认使用 XMLLanguageDriver 解析注解 SQL。
     * SQL 中出现 <、>、<=、>= 时必须用 &lt; / &gt; 转义或使用 CDATA，否则会触发 XML 解析错误。
     */
    @Select({
            "<script>",
            "SELECT IFNULL(COUNT(DISTINCT seat_id), 0) ",
            "FROM reservation ",
            "WHERE date = #{date} ",
            "  AND seat_id IS NOT NULL ",
            "  AND start_time &lt;= #{now} ",
            "  AND end_time &gt; #{now} ",
            "  AND status = 'reserved'",
            "</script>"
    })
    Integer countReservedNow(@Param("date") LocalDate date, @Param("now") LocalTime now);

    /**
     * 今日：当前时刻“正在使用”（checked_in / late 且时间段覆盖当前时刻）
     */
    @Select({
            "<script>",
            "SELECT IFNULL(COUNT(DISTINCT seat_id), 0) ",
            "FROM reservation ",
            "WHERE date = #{date} ",
            "  AND seat_id IS NOT NULL ",
            "  AND start_time &lt;= #{now} ",
            "  AND end_time &gt; #{now} ",
            "  AND status IN ('checked_in','late')",
            "</script>"
    })
    Integer countInUseNow(@Param("date") LocalDate date, @Param("now") LocalTime now);

    /**
     * 本月累计预约次数：只统计 reserved / checked_in / late（排除 no_show 等）
     */
    @Select({
            "<script>",
            "SELECT IFNULL(COUNT(*), 0) ",
            "FROM reservation ",
            "WHERE user_id = #{userId} ",
            "  AND date &gt;= #{startDate} AND date &lt;= #{endDate} ",
            "  AND status IN ('reserved','checked_in','late')",
            "</script>"
    })
    Long countMonthReservations(@Param("userId") Integer userId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    /**
     * 本月累计自习时长：仅 checked_in / late，按 (end-start) 求和（分钟）
     */
    @Select({
            "<script>",
            "SELECT IFNULL(SUM(TIME_TO_SEC(TIMEDIFF(end_time, start_time))), 0) DIV 60 ",
            "FROM reservation ",
            "WHERE user_id = #{userId} ",
            "  AND date &gt;= #{startDate} AND date &lt;= #{endDate} ",
            "  AND status IN ('checked_in','late')",
            "</script>"
    })
    Long sumMonthStudyMinutes(@Param("userId") Integer userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    /**
     * 本月最近一次到馆：成功签到或迟到的“最近预约开始时间”
     */
    @Select({
            "<script>",
            "SELECT MAX(TIMESTAMP(date, start_time)) ",
            "FROM reservation ",
            "WHERE user_id = #{userId} ",
            "  AND date &gt;= #{startDate} AND date &lt;= #{endDate} ",
            "  AND status IN ('checked_in','late')",
            "</script>"
    })
    LocalDateTime selectLastVisitTime(@Param("userId") Integer userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    // ==========================================================
    // 座位管理页需要的查询（SeatAdminController 会调用）
    // ==========================================================

    /**
     * 批量查询：按 room_id 统计“当前占用”的座位数（distinct seat_id）
     * occupied = 今天 + 当前时刻 + 状态 reserved/checked_in/late
     * 返回字段：
     * - roomId
     * - cnt
     */
    @Select({
            "<script>",
            "SELECT s.room_id AS roomId, IFNULL(COUNT(DISTINCT r.seat_id), 0) AS cnt ",
            "FROM reservation r ",
            "JOIN seat s ON s.id = r.seat_id ",
            "WHERE r.date = #{date} ",
            "  AND r.seat_id IS NOT NULL ",
            "  AND r.start_time &lt;= #{now} ",
            "  AND r.end_time &gt; #{now} ",
            "  AND r.status IN ('reserved','checked_in','late') ",
            "  AND s.room_id IN ",
            "  <foreach collection='roomIds' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "  </foreach> ",
            "GROUP BY s.room_id",
            "</script>"
    })
    List<Map<String, Object>> selectOccupiedSeatCountByRoomIds(@Param("roomIds") List<Integer> roomIds,
                                                               @Param("date") LocalDate date,
                                                               @Param("now") LocalTime now);

    /**
     * 查询某一房间：当前时刻占用的 seat_id 列表（distinct）
     */
    @Select({
            "<script>",
            "SELECT DISTINCT r.seat_id ",
            "FROM reservation r ",
            "JOIN seat s ON s.id = r.seat_id ",
            "WHERE s.room_id = #{roomId} ",
            "  AND r.date = #{date} ",
            "  AND r.seat_id IS NOT NULL ",
            "  AND r.start_time &lt;= #{now} ",
            "  AND r.end_time &gt; #{now} ",
            "  AND r.status IN ('reserved','checked_in','late')",
            "</script>"
    })
    List<Integer> selectOccupiedSeatIdsNow(@Param("roomId") Integer roomId,
                                           @Param("date") LocalDate date,
                                           @Param("now") LocalTime now);
}
