package com.ssrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateAdminDTO;
import com.ssrms.controller.vo.AdminUserPageVO;
import com.ssrms.controller.vo.AdminUserRowVO;
import com.ssrms.controller.vo.AdminUserStatsVO;
import com.ssrms.entity.User;
import com.ssrms.entity.Violation;
import com.ssrms.mapper.ViolationMapper;
import com.ssrms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员端：用户管理接口
 *
 * 业务规则：
 * - status：0正常 1预警(系统自动：credit_score < 80 且非黑名单) 2黑名单(仅管理员手动)
 * - 黑名单（2）只允许管理员手动加入/解除；解除后按当前 credit_score 自动回落到 0/1
 * - 管理员账号（role_id=0）不允许加入/解除黑名单，也不提供重置密码入口
 */
@RestController
@RequestMapping("/user/admin")
public class UserAdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ViolationMapper violationMapper;

    /**
     * 分页 + 筛选 + 统计
     *
     * @param onlyAbnormal true 表示仅查看预警/黑名单（status in (1,2)），但若同时选择了 status 下拉则以 status 为准
     */
    @GetMapping("/page")
    public Result page(@RequestParam(defaultValue = "1") Long page,
                       @RequestParam(defaultValue = "8") Long size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer roleId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Boolean onlyAbnormal) {

        // 每次查询前同步“预警”状态（不会影响 status=2 的黑名单锁定）
        syncWarningStatus();

        LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery();
        applyFilters(qw, keyword, roleId, status, onlyAbnormal);
        qw.orderByDesc(User::getCreateTime);

        Page<User> p = userService.page(new Page<>(page, size), qw);
        List<User> records = p.getRecords() == null ? Collections.emptyList() : p.getRecords();

        Map<Integer, Integer> violationCountMap = buildViolationCountMap(
                records.stream().map(User::getId).filter(Objects::nonNull).collect(Collectors.toList())
        );

        List<AdminUserRowVO> rowVOS = records.stream().map(u -> {
            AdminUserRowVO vo = new AdminUserRowVO();
            vo.setId(u.getId());
            vo.setAccount(u.getAccount());
            vo.setName(u.getName());
            vo.setStudentNo(u.getStudentNo());
            vo.setRoleId(u.getRoleId());
            vo.setPhone(u.getPhone());
            vo.setEmail(u.getEmail());
            vo.setCollege(u.getCollege());
            vo.setGradeClass(u.getGradeClass());
            vo.setCreditScore(u.getCreditScore());
            vo.setStatus(u.getBlacklistFlag());
            vo.setViolationCount(violationCountMap.getOrDefault(u.getId(), 0));
            vo.setIsValid(u.getIsValid());
            vo.setLastLoginIp(u.getLastLoginIp());
            vo.setLastLoginTime(u.getLastLoginTime());
            vo.setCreateTime(u.getCreateTime());
            vo.setUpdateTime(u.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());

        AdminUserPageVO pageVO = new AdminUserPageVO();
        pageVO.setRecords(rowVOS);
        pageVO.setStats(buildStats(keyword, roleId, status, onlyAbnormal));

        return Result.success(pageVO, p.getTotal());
    }

    /** 用户详情：基本信息 + 违约次数 + 最近违约记录（默认 10 条） */
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Integer id) {
        if (id == null) return Result.fail("缺少用户ID");

        syncWarningStatus();

        User u = userService.getById(id);
        if (u == null) return Result.fail("用户不存在");

        int violationCount = violationMapper.selectCount(
                Wrappers.<Violation>lambdaQuery().eq(Violation::getUserId, id)
        ).intValue();

        List<Violation> recent = violationMapper.selectList(
                Wrappers.<Violation>lambdaQuery()
                        .eq(Violation::getUserId, id)
                        .orderByDesc(Violation::getCreateTime)
                        .last("LIMIT 10")
        );

        // 返回一份较完整的 user（前端抽屉展示用）
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", u.getId());
        userMap.put("account", u.getAccount());
        userMap.put("name", u.getName());
        userMap.put("studentNo", u.getStudentNo());
        userMap.put("roleId", u.getRoleId());
        userMap.put("phone", u.getPhone());
        userMap.put("email", u.getEmail());
        userMap.put("college", u.getCollege());
        userMap.put("gradeClass", u.getGradeClass());
        userMap.put("commonCampus", u.getCommonCampus());
        userMap.put("profileRemark", u.getProfileRemark());
        // 兼容前端可能使用 remark 字段名
        userMap.put("remark", u.getProfileRemark());
        userMap.put("creditScore", u.getCreditScore());
        userMap.put("status", u.getBlacklistFlag());
        userMap.put("isValid", u.getIsValid());
        userMap.put("lastLoginIp", u.getLastLoginIp());
        userMap.put("lastLoginTime", u.getLastLoginTime());
        userMap.put("createTime", u.getCreateTime());
        userMap.put("updateTime", u.getUpdateTime());

        Map<String, Object> resp = new HashMap<>();
        resp.put("user", userMap);
        resp.put("violationCount", violationCount);
        resp.put("recentViolations", recent);

        return Result.success(resp);
    }

    /** 新增管理员账号（role_id=0），需填写：account / name / password */
    @PostMapping("/createAdmin")
    public Result createAdmin(@RequestBody CreateAdminDTO dto) {
        if (dto == null) return Result.fail("参数错误");
        String account = dto.getAccount() == null ? null : dto.getAccount().trim();
        String name = dto.getName() == null ? null : dto.getName().trim();
        String password = dto.getPassword();

        if (!StringUtils.hasText(account)) return Result.fail("账号不能为空");
        if (!StringUtils.hasText(name)) return Result.fail("姓名不能为空");
        if (!StringUtils.hasText(password) || password.length() < 6) return Result.fail("密码至少 6 位");

        if (userService.existsByAccount(account)) return Result.fail("账号已存在");

        User u = new User();
        u.setAccount(account);
        u.setName(name);
        u.setPassword(password);
        u.setRoleId(0);

        // 默认值
        u.setIsValid("Y");
        u.setCreditScore(100);
        u.setBlacklistFlag(0);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());

        // 对管理员来说 student_no 可以为空；为了显示统一，也可以填一个和账号一致的“工号/账号”
        if (!StringUtils.hasText(u.getStudentNo())) {
            u.setStudentNo(account);
        }

        boolean ok = userService.save(u);
        return ok ? Result.success(true) : Result.fail("创建失败");
    }

    /** 单个加入黑名单（status=2） */
    @PostMapping("/blacklist/{id}")
    public Result blacklist(@PathVariable Integer id) {
        if (id == null) return Result.fail("缺少用户ID");
        User u = userService.getById(id);
        if (u == null) return Result.fail("用户不存在");
        if (u.getRoleId() != null && u.getRoleId() == 0) return Result.fail("管理员账号不允许加入黑名单");

        boolean ok = userService.update(
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, id)
                        .set(User::getBlacklistFlag, 2)
                        .set(User::getUpdateTime, LocalDateTime.now())
        );
        return ok ? Result.success(true) : Result.fail("操作失败");
    }

    /** 单个解除黑名单（按 credit_score 回落到 0/1） */
    @PostMapping("/unblacklist/{id}")
    public Result unblacklist(@PathVariable Integer id) {
        if (id == null) return Result.fail("缺少用户ID");
        User u = userService.getById(id);
        if (u == null) return Result.fail("用户不存在");
        if (u.getRoleId() != null && u.getRoleId() == 0) return Result.fail("管理员账号不允许解除黑名单");

        Integer st = u.getBlacklistFlag();
        if (st == null || st != 2) return Result.fail("该用户不在黑名单中");

        int score = u.getCreditScore() == null ? 100 : u.getCreditScore();
        int newStatus = (score < 80) ? 1 : 0;

        boolean ok = userService.update(
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, id)
                        .eq(User::getBlacklistFlag, 2)
                        .set(User::getBlacklistFlag, newStatus)
                        .set(User::getUpdateTime, LocalDateTime.now())
        );
        return ok ? Result.success(true) : Result.fail("操作失败");
    }

    /** 批量加入黑名单（status=2），自动忽略管理员账号 */
    @PostMapping("/batchBlacklist")
    public Result batchBlacklist(@RequestBody BatchIds body) {
        List<Integer> ids = body == null ? null : body.getIds();
        if (ids == null || ids.isEmpty()) return Result.fail("请选择用户");

        boolean ok = userService.update(
                Wrappers.<User>lambdaUpdate()
                        .in(User::getId, ids)
                        .ne(User::getRoleId, 0)
                        .set(User::getBlacklistFlag, 2)
                        .set(User::getUpdateTime, LocalDateTime.now())
        );
        return ok ? Result.success(true) : Result.fail("操作失败");
    }

    /** 批量解除黑名单（仅对 status=2 生效；解除后按 credit_score 回落到 0/1），自动忽略管理员账号 */
    @PostMapping("/batchUnblacklist")
    public Result batchUnblacklist(@RequestBody BatchIds body) {
        List<Integer> ids = body == null ? null : body.getIds();
        if (ids == null || ids.isEmpty()) return Result.fail("请选择用户");

        List<User> locked = userService.list(
                Wrappers.<User>lambdaQuery()
                        .in(User::getId, ids)
                        .ne(User::getRoleId, 0)
                        .eq(User::getBlacklistFlag, 2)
        );
        if (locked == null || locked.isEmpty()) return Result.success(true);

        LocalDateTime now = LocalDateTime.now();
        for (User u : locked) {
            int score = u.getCreditScore() == null ? 100 : u.getCreditScore();
            u.setBlacklistFlag(score < 80 ? 1 : 0);
            u.setUpdateTime(now);
        }

        boolean ok = userService.updateBatchById(locked);
        return ok ? Result.success(true) : Result.fail("操作失败");
    }

    /** 重置密码：默认 123456（管理员账号不允许） */
    @PostMapping("/resetPassword/{id}")
    public Result resetPassword(@PathVariable Integer id) {
        if (id == null) return Result.fail("缺少用户ID");
        User u = userService.getById(id);
        if (u == null) return Result.fail("用户不存在");
        if (u.getRoleId() != null && u.getRoleId() == 0) return Result.fail("管理员账号不允许重置密码");

        boolean ok = userService.update(
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, id)
                        .set(User::getPassword, "123456")
                        .set(User::getUpdateTime, LocalDateTime.now())
        );
        return ok ? Result.success(true) : Result.fail("重置失败");
    }

    // =========================
    // helpers
    // =========================

    /** 同步非黑名单用户的“预警”状态 */
    private void syncWarningStatus() {
        // status=2 保持不变；其余由 credit_score 决定 0/1
        userService.update(
                Wrappers.<User>lambdaUpdate()
                        .ne(User::getBlacklistFlag, 2)
                        .lt(User::getCreditScore, 80)
                        .set(User::getBlacklistFlag, 1)
        );
        userService.update(
                Wrappers.<User>lambdaUpdate()
                        .ne(User::getBlacklistFlag, 2)
                        .ge(User::getCreditScore, 80)
                        .set(User::getBlacklistFlag, 0)
        );
    }

    private void applyFilters(LambdaQueryWrapper<User> qw,
                              String keyword,
                              Integer roleId,
                              Integer status,
                              Boolean onlyAbnormal) {
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            qw.and(w -> w.like(User::getName, k)
                    .or().like(User::getStudentNo, k)
                    .or().like(User::getAccount, k));
        }
        if (roleId != null) {
            qw.eq(User::getRoleId, roleId);
        }

        if (status != null) {
            qw.eq(User::getBlacklistFlag, status);
        } else if (Boolean.TRUE.equals(onlyAbnormal)) {
            qw.in(User::getBlacklistFlag, 1, 2);
        }
    }

    private AdminUserStatsVO buildStats(String keyword, Integer roleId, Integer status, Boolean onlyAbnormal) {
        LambdaQueryWrapper<User> base = Wrappers.lambdaQuery();
        applyFilters(base, keyword, roleId, status, onlyAbnormal);

        AdminUserStatsVO vo = new AdminUserStatsVO();
        vo.setTotal(userService.count(base));

        LambdaQueryWrapper<User> q0 = Wrappers.lambdaQuery();
        applyFilters(q0, keyword, roleId, status, onlyAbnormal);
        q0.eq(User::getBlacklistFlag, 0);
        vo.setNormal(userService.count(q0));

        LambdaQueryWrapper<User> q1 = Wrappers.lambdaQuery();
        applyFilters(q1, keyword, roleId, status, onlyAbnormal);
        q1.eq(User::getBlacklistFlag, 1);
        vo.setWarning(userService.count(q1));

        LambdaQueryWrapper<User> q2 = Wrappers.lambdaQuery();
        applyFilters(q2, keyword, roleId, status, onlyAbnormal);
        q2.eq(User::getBlacklistFlag, 2);
        vo.setBlacklist(userService.count(q2));

        return vo;
    }

    private Map<Integer, Integer> buildViolationCountMap(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();

        List<Map<String, Object>> maps = violationMapper.selectMaps(
                Wrappers.<Violation>query()
                        .select("user_id AS userId", "COUNT(*) AS cnt")
                        .in("user_id", userIds)
                        .groupBy("user_id")
        );

        Map<Integer, Integer> out = new HashMap<>();
        for (Map<String, Object> m : maps) {
            Object uidObj = m.get("userId");
            Object cntObj = m.get("cnt");
            if (uidObj == null || cntObj == null) continue;
            Integer uid = (uidObj instanceof Number) ? ((Number) uidObj).intValue() : Integer.valueOf(uidObj.toString());
            Integer cnt = (cntObj instanceof Number) ? ((Number) cntObj).intValue() : Integer.valueOf(cntObj.toString());
            out.put(uid, cnt);
        }
        return out;
    }

    public static class BatchIds {
        private List<Integer> ids;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }
}