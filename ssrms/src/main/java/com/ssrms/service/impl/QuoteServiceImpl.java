package com.ssrms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.entity.Quote;
import com.ssrms.mapper.QuoteMapper;
import com.ssrms.service.QuoteService;
import org.springframework.stereotype.Service;

@Service
public class QuoteServiceImpl extends ServiceImpl<QuoteMapper, Quote> implements QuoteService {

    @Override
    public Quote randomEnabled(Integer excludeId) {
        // MySQL 随机一条：ORDER BY RAND() LIMIT 1
        var qw = Wrappers.<Quote>lambdaQuery()
                .eq(Quote::getEnabled, 1);

        if (excludeId != null) {
            qw.ne(Quote::getId, excludeId);
        }

        qw.last("ORDER BY RAND() LIMIT 1");
        return this.getOne(qw, false);
    }
}