package com.ssrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ssrms.entity.Reservation;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    // 今日：当前时刻“已预约”（reserved 且时间段覆盖当前时刻）
    @Select("SELECT IFNULL(COUNT(DISTINCT seat_id), 0) " +
            "FROM reservation " +
            "WHERE date = #{date} " +
            "  AND seat_id IS NOT NULL " +
            "  AND start_time <= #{now} " +
            "  AND end_time > #{now} " +
            "  AND status = 'reserved'")
    Integer countReservedNow(@Param("date") LocalDate date, @Param("now") LocalTime now);

    // 今日：当前时刻“正在使用”（checked_in / late 且时间段覆盖当前时刻）
    @Select("SELECT IFNULL(COUNT(DISTINCT seat_id), 0) " +
            "FROM reservation " +
            "WHERE date = #{date} " +
            "  AND seat_id IS NOT NULL " +
            "  AND start_time <= #{now} " +
            "  AND end_time > #{now} " +
            "  AND status IN ('checked_in','late')")
    Integer countInUseNow(@Param("date") LocalDate date, @Param("now") LocalTime now);

    // 本月累计预约次数：只统计 reserved / checked_in / late（按你期望排除 no_show）
    @Select("SELECT IFNULL(COUNT(*), 0) " +
            "FROM reservation " +
            "WHERE user_id = #{userId} " +
            "  AND date >= #{startDate} AND date <= #{endDate} " +
            "  AND status IN ('reserved','checked_in','late')")
    Long countMonthReservations(@Param("userId") Integer userId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    // 本月累计自习时长：仅 checked_in / late，按 (end-start) 求和（分钟）
    @Select("SELECT IFNULL(SUM(TIME_TO_SEC(TIMEDIFF(end_time, start_time))), 0) DIV 60 " +
            "FROM reservation " +
            "WHERE user_id = #{userId} " +
            "  AND date >= #{startDate} AND date <= #{endDate} " +
            "  AND status IN ('checked_in','late')")
    Long sumMonthStudyMinutes(@Param("userId") Integer userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    // 本月最近一次到馆：成功签到或迟到的“最近预约开始时间”
    @Select("SELECT MAX(TIMESTAMP(date, start_time)) " +
            "FROM reservation " +
            "WHERE user_id = #{userId} " +
            "  AND date >= #{startDate} AND date <= #{endDate} " +
            "  AND status IN ('checked_in','late')")
    LocalDateTime selectLastVisitTime(@Param("userId") Integer userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
}