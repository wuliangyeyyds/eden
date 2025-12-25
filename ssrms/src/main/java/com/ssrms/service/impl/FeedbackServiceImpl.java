package com.ssrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.entity.Feedback;
import com.ssrms.mapper.FeedbackMapper;
import com.ssrms.service.FeedbackService;
import org.springframework.stereotype.Service;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {
}
