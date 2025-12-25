package com.ssrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ssrms.common.Result;
import com.ssrms.controller.vo.AnnouncementVO;
import com.ssrms.entity.Announcement;
import com.ssrms.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公告相关接口（学生端 + 管理端）
 *
 * 数据表结构以 ssrms.sql 中的 announcement 表为准：
 *  - id, title, content, type, level, target_role, target_text, is_top, is_published,
 *    publish_time, expire_time, created_by, create_time, update_time
 */
@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 首页公告（学生端使用：拉取候选池，前端再做 NEW 优先 + 时间倒序）
     * GET /announcement/home?roleId=1&limit=50
     */
    @GetMapping("/home")
    public Result home(@RequestParam(defaultValue = "1") Integer roleId,
                       @RequestParam(defaultValue = "6") Integer limit) {

        int safeLimit = Math.max(1, Math.min(50, limit));
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Announcement> qw = Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getIsPublished, 1)
                // 允许“全部(2)”或当前角色
                .and(w -> w.eq(Announcement::getTargetRole, 2).or().eq(Announcement::getTargetRole, roleId))
                // 发布时间 <= 当前时间
                .le(Announcement::getPublishTime, now)
                // 未过期：expire_time is null OR expire_time >= now
                .and(w -> w.isNull(Announcement::getExpireTime).or().ge(Announcement::getExpireTime, now))
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishTime)
                .last("limit " + safeLimit);

        List<Announcement> list = announcementService.list(qw);
        List<AnnouncementVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 学生端公告分页（当前生效 + 按角色过滤）
     * GET /announcement/page?roleId=1&pageNum=1&pageSize=8&type=RULE
     */
    @GetMapping("/page")
    public Result page(@RequestParam(defaultValue = "1") Integer roleId,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "8") Integer pageSize,
                       @RequestParam(required = false) String type) {

        int pn = Math.max(1, pageNum);
        int ps = Math.max(1, Math.min(50, pageSize));
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Announcement> qw = Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getIsPublished, 1)
                .and(w -> w.eq(Announcement::getTargetRole, 2).or().eq(Announcement::getTargetRole, roleId))
                .le(Announcement::getPublishTime, now)
                .and(w -> w.isNull(Announcement::getExpireTime).or().ge(Announcement::getExpireTime, now))
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishTime);

        if (StringUtils.hasText(type)) {
            qw.eq(Announcement::getType, type.trim());
        }

        Page<Announcement> page = announcementService.page(new Page<>(pn, ps), qw);
        List<AnnouncementVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(voList, page.getTotal());
    }

    /* ==========================================================
     * ✅ 管理端：公告管理（列表/详情/新增/修改=重新发布/删除）
     * ========================================================== */

    /**
     * 管理端分页：默认只展示“当前生效”的公告（与学生端一致）
     * includeExpired=1 则包含已过期/未生效（便于清理或查看历史）
     *
     * GET /announcement/admin/page?pageNum=1&pageSize=10&type=&level=&targetRole=&keyword=&includeExpired=0
     */
    @GetMapping("/admin/page")
    public Result adminPage(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String level,
                            @RequestParam(required = false) Integer targetRole,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "0") Integer includeExpired) {

        int pn = Math.max(1, pageNum);
        int ps = Math.max(1, Math.min(50, pageSize));
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Announcement> qw = Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getIsPublished, 1);

        if (includeExpired == null || includeExpired == 0) {
            qw.le(Announcement::getPublishTime, now)
                    .and(w -> w.isNull(Announcement::getExpireTime).or().ge(Announcement::getExpireTime, now));
        }

        if (StringUtils.hasText(type)) {
            qw.eq(Announcement::getType, type.trim());
        }
        if (StringUtils.hasText(level)) {
            qw.eq(Announcement::getLevel, level.trim());
        }
        if (targetRole != null) {
            qw.eq(Announcement::getTargetRole, targetRole);
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            qw.and(w -> w.like(Announcement::getTitle, k).or().like(Announcement::getContent, k));
        }

        qw.orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishTime);

        Page<Announcement> page = announcementService.page(new Page<>(pn, ps), qw);
        List<AnnouncementVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(voList, page.getTotal());
    }

    /**
     * 管理端详情
     * GET /announcement/admin/detail?id=123
     */
    @GetMapping("/admin/detail")
    public Result adminDetail(@RequestParam Integer id) {
        if (id == null) return Result.fail("公告ID不能为空");
        Announcement a = announcementService.getById(id);
        if (a == null) return Result.fail("公告不存在");
        return Result.success(toVO(a));
    }

    /**
     * 管理端新增（默认立即发布）
     * POST /announcement/admin/create
     */
    @PostMapping("/admin/create")
    public Result adminCreate(@RequestBody Announcement dto) {
        if (dto == null) return Result.fail("参数不能为空");
        if (!StringUtils.hasText(dto.getTitle())) return Result.fail("公告标题不能为空");
        if (!StringUtils.hasText(dto.getContent())) return Result.fail("公告内容不能为空");
        if (!StringUtils.hasText(dto.getType())) return Result.fail("公告类型不能为空");
        if (!StringUtils.hasText(dto.getLevel())) return Result.fail("重要程度不能为空");
        if (dto.getTargetRole() == null) return Result.fail("面向对象不能为空");

        LocalDateTime now = LocalDateTime.now();

        Announcement a = new Announcement();
        // 只拷贝业务字段，时间字段统一在后端写入
        a.setTitle(dto.getTitle().trim());
        a.setContent(dto.getContent().trim());
        a.setType(dto.getType().trim());
        a.setLevel(dto.getLevel().trim());
        a.setTargetRole(dto.getTargetRole());
        a.setTargetText(StringUtils.hasText(dto.getTargetText()) ? dto.getTargetText().trim() : defaultTargetText(dto.getTargetRole()));
        a.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        a.setIsPublished(1);

        a.setPublishTime(now);
        a.setExpireTime(dto.getExpireTime()); // 可选
        a.setCreatedBy(dto.getCreatedBy());   // 可选
        a.setCreateTime(now);
        a.setUpdateTime(now);

        announcementService.save(a);
        return Result.success(toVO(a));
    }

    /**
     * 管理端修改（✅ 重新发布：插入新公告 + 删除旧公告）
     * 前端传 dto.id = 旧公告ID
     * POST /announcement/admin/republish
     */
    @PostMapping("/admin/republish")
    @Transactional
    public Result adminRepublish(@RequestBody Announcement dto) {
        if (dto == null) return Result.fail("参数不能为空");
        if (dto.getId() == null) return Result.fail("公告ID不能为空");

        Integer oldId = dto.getId();
        Announcement old = announcementService.getById(oldId);
        if (old == null) return Result.fail("公告不存在");

        if (!StringUtils.hasText(dto.getTitle())) return Result.fail("公告标题不能为空");
        if (!StringUtils.hasText(dto.getContent())) return Result.fail("公告内容不能为空");
        if (!StringUtils.hasText(dto.getType())) return Result.fail("公告类型不能为空");
        if (!StringUtils.hasText(dto.getLevel())) return Result.fail("重要程度不能为空");
        if (dto.getTargetRole() == null) return Result.fail("面向对象不能为空");

        LocalDateTime now = LocalDateTime.now();

        Announcement a = new Announcement();
        a.setTitle(dto.getTitle().trim());
        a.setContent(dto.getContent().trim());
        a.setType(dto.getType().trim());
        a.setLevel(dto.getLevel().trim());
        a.setTargetRole(dto.getTargetRole());
        a.setTargetText(StringUtils.hasText(dto.getTargetText()) ? dto.getTargetText().trim() : defaultTargetText(dto.getTargetRole()));
        a.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        a.setIsPublished(1);

        // ✅ 重新发布：发布时间更新为当前时间，id 为新生成
        a.setPublishTime(now);
        a.setExpireTime(dto.getExpireTime());
        a.setCreatedBy(dto.getCreatedBy());
        a.setCreateTime(now);
        a.setUpdateTime(now);

        announcementService.save(a);
        announcementService.removeById(oldId);

        return Result.success(toVO(a));
    }

    /**
     * 管理端删除
     * DELETE /announcement/admin/delete?id=123
     */
    @DeleteMapping("/admin/delete")
    public Result adminDelete(@RequestParam Integer id) {
        if (id == null) return Result.fail("公告ID不能为空");
        boolean ok = announcementService.removeById(id);
        return ok ? Result.success() : Result.fail("删除失败");
    }

    /* ===================== helpers ===================== */

    private String defaultTargetText(Integer targetRole) {
        if (targetRole == null) return "全部用户";
        if (targetRole == 0) return "全体管理员";
        if (targetRole == 1) return "全体学生";
        return "全部用户";
    }

    private AnnouncementVO toVO(Announcement a) {
        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setContent(a.getContent());
        vo.setType(a.getType());
        vo.setLevel(a.getLevel());
        vo.setTargetRole(a.getTargetRole());
        vo.setTargetText(a.getTargetText());
        vo.setIsTop(a.getIsTop());
        vo.setPublishTime(a.getPublishTime());
        return vo;
    }
}