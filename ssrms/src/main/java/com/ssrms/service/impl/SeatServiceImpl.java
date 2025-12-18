package com.ssrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.entity.Seat;
import com.ssrms.mapper.SeatMapper;
import com.ssrms.service.SeatService;
import org.springframework.stereotype.Service;

@Service
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {
}