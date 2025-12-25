package com.ssrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.entity.Announcement;
import com.ssrms.mapper.AnnouncementMapper;
import com.ssrms.service.AnnouncementService;
import org.springframework.stereotype.Service;

/**
 * 公告 Service 实现
 */
@Service
public class AnnouncementServiceImpl
        extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {
}
