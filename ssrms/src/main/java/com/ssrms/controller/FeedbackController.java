package com.ssrms.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.FeedbackSubmitDTO;
import com.ssrms.entity.Feedback;
import com.ssrms.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 学生端：评价/投诉（反馈）
 *
 * 对应前端 UserHome.vue：
 * - POST /feedback/submit
 * - GET  /feedback/my-page?userId=...&status=...&pageNum=...&pageSize=...
 *
 * 关键点：
 * 1) status 作为可选参数：不传=不过滤，才能在“全部”里看到 processing/resolved。
 * 2) Result 返回结构要匹配前端：data 是列表，total 是总数。
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/submit")
    public Result submit(@RequestBody FeedbackSubmitDTO dto) {
        if (dto == null) return Result.fail("参数为空");
        if (dto.getUserId() == null) return Result.fail("缺少 userId");
        if (!StringUtils.hasText(dto.getCategory())) return Result.fail("缺少 category");
        if (!StringUtils.hasText(dto.getContent())) return Result.fail("缺少 content");

        Feedback fb = new Feedback();
        // 你项目里 Feedback.getUserId() 返回 Long，这里统一用 Long
        fb.setUserId(dto.getUserId());
        fb.setReservationId(dto.getReservationId());
        fb.setCategory(dto.getCategory());
        fb.setRating(dto.getRating());
        fb.setContent(dto.getContent());

        fb.setStatus("pending");
        fb.setReply(null);

        LocalDateTime now = LocalDateTime.now();
        fb.setCreateTime(now);
        fb.setUpdateTime(now);

        feedbackService.save(fb);
        return Result.successWithMsg("提交成功");
    }

    @GetMapping("/my-page")
    public Result myPage(@RequestParam Long userId,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "6") Integer pageSize) {

        int pn = Math.max(1, pageNum == null ? 1 : pageNum);
        int ps = Math.max(1, Math.min(50, pageSize == null ? 6 : pageSize));

        Page<Feedback> page = feedbackService.page(
                new Page<>(pn, ps),
                Wrappers.<Feedback>lambdaQuery()
                        .eq(Feedback::getUserId, userId)
                        // ✅ 不传 status 就不过滤（保证“全部”能看到 processing/resolved）
                        .eq(StringUtils.hasText(status), Feedback::getStatus, status)
                        .orderByDesc(Feedback::getUpdateTime)
                        .orderByDesc(Feedback::getCreateTime)
        );

        return Result.success(page.getRecords(), page.getTotal());
    }
}
