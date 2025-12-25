package com.ssrms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface AdminDashboardMapper {

    // ===== 今日统计 =====

    @Select("SELECT IFNULL(COUNT(1), 0) " +
            "FROM reservation " +
            "WHERE date = #{day} " +
            "  AND status NOT IN ('cancelled','cancel_overdue')")
    int countTodayReservations(@Param("day") LocalDate day);

    @Select("SELECT IFNULL(COUNT(1), 0) " +
            "FROM reservation " +
            "WHERE date = #{day} " +
            "  AND status IN ('checked_in','late')")
    int countTodayCheckedIn(@Param("day") LocalDate day);

    @Select("SELECT IFNULL(COUNT(1), 0) " +
            "FROM reservation " +
            "WHERE date = #{day} " +
            "  AND status NOT IN ('cancelled','cancel_overdue')")
    int countReservationsByDayExcludingCancelled(@Param("day") LocalDate day);

    // ===== 本周统计（start 为周一，end 为下周一，左闭右开）=====

    @Select("SELECT IFNULL(COUNT(1), 0) " +
            "FROM reservation " +
            "WHERE date >= #{start} AND date < #{end} " +
            "  AND status NOT IN ('cancelled','cancel_overdue')")
    int countWeekTotal(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT IFNULL(COUNT(1), 0) " +
            "FROM reservation " +
            "WHERE date >= #{start} AND date < #{end} " +
            "  AND status = #{status}")
    int countWeekByStatus(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("status") String status);

    // ===== 开放自习室 =====

    @Select("SELECT IFNULL(COUNT(1), 0) FROM room WHERE status = 'open'")
    int countOpenRooms();

    @Select("SELECT IFNULL(SUM(open_seats), 0) FROM room WHERE status = 'open'")
    Integer sumOpenRoomSeats();

    // ===== 最近预约动态 =====
    @Select("SELECT " +
            " r.id AS reservationId, " +
            " r.status AS status, " +
            " r.start_time AS startTime, " +
            " r.end_time AS endTime, " +
            " r.create_time AS createTime, " +
            " r.update_time AS updateTime, " +
            " u.name AS userName, " +
            " rm.campus AS campus, " +
            " rm.building AS building, " +
            " rm.room_name AS roomName " +
            "FROM reservation r " +
            "LEFT JOIN user u ON u.id = r.user_id " +
            "LEFT JOIN room rm ON rm.id = r.room_id " +
            "ORDER BY COALESCE(r.update_time, r.create_time) DESC " +
            "LIMIT #{limit}")
    List<RecentRow> selectRecentRows(@Param("limit") int limit);

    class RecentRow {
        public Long reservationId;
        public String status;
        public LocalTime startTime;
        public LocalTime endTime;
        public LocalDateTime createTime;
        public LocalDateTime updateTime;
        public String userName;
        public String campus;
        public String building;
        public String roomName;
    }
}
