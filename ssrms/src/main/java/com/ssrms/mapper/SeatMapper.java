package com.ssrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ssrms.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    @Select("SELECT id FROM `seat` WHERE id = #{id} FOR UPDATE")
    Integer lockSeat(@Param("id") Integer id);
}