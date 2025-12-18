package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.entity.Quote;
import com.ssrms.service.QuoteService;
import com.ssrms.vo.QuoteVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quote")
@CrossOrigin(origins = "http://localhost:8080") // 先按你本地开发环境放开
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    // 同时兼容 GET/POST，避免你前端改动后还踩坑
    @RequestMapping(value = "/random", method = {RequestMethod.GET, RequestMethod.POST})
    public Result random(@RequestParam(value = "excludeId", required = false) Integer excludeId) {

        Quote q = quoteService.randomEnabled(excludeId);

        QuoteVO vo;
        if (q == null || q.getContent() == null || q.getContent().isBlank()) {
            vo = new QuoteVO(null, "今天也要稳住节奏，先做一小步。");
        } else {
            vo = new QuoteVO(q.getId(), q.getContent());
        }

        return Result.success(vo);
    }
}