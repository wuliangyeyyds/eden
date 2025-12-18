package com.ssrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssrms.entity.Quote;

public interface QuoteService extends IService<Quote> {
    Quote randomEnabled(Integer excludeId);
}