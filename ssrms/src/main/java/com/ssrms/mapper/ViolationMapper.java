package com.ssrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ssrms.entity.Violation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ViolationMapper extends BaseMapper<Violation> {

    @Select("SELECT COALESCE(SUM(penalty_score), 0) FROM violation WHERE user_id = #{userId} AND handled = 0")
    Integer sumUnhandledPenalty(@Param("userId") Integer userId);
}