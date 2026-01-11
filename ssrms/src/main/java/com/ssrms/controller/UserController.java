package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.controller.dto.LoginDTO;
import com.ssrms.controller.dto.RegisterDTO;
import com.ssrms.entity.User;
import com.ssrms.mapper.ViolationMapper;
import com.ssrms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    private String getClientIp(HttpServletRequest request) {
        String[] keys = new String[] {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        for (String k : keys) {
            String v = request.getHeader(k);
            if (v != null && v.length() > 0 && !"unknown".equalsIgnoreCase(v)) {
                // X-Forwarded-For 可能是 "client, proxy1, proxy2"
                return v.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO dto, HttpServletRequest request) {
        if (dto.getAccount() == null || dto.getPassword() == null || dto.getRoleId() == null) {
            return Result.fail("账号、密码和登录身份不能为空");
        }

        User user = userService.login(dto.getAccount(), dto.getPassword(), dto.getRoleId());
        if (user == null) {
            return Result.fail("账号或密码错误，或账号状态异常");
        }

        // 1) 获取客户端IP
        String ip = getClientIp(request);
        LocalDateTime now = LocalDateTime.now();

        // 2) 写回数据库（只更新需要的字段）
        User upd = new User();
        upd.setId(user.getId());
        upd.setLastLoginIp(ip);
        upd.setLastLoginTime(now);
        userService.updateById(upd);

        // 3) 同步到返回对象（避免前端再查一次）
        user.setLastLoginIp(ip);
        user.setLastLoginTime(now);

        return Result.success(toVO(user));
    }

    /**
     * 查询个人信息
     * GET /user/profile?userId=1
     */
    @GetMapping("/profile")
    public Result getProfile(@RequestParam Integer userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        Integer sum = violationMapper.sumUnhandledPenalty(userId);
        int penalty = (sum == null ? 0 : sum);
        int credit = Math.max(0, 100 - penalty);

        // ✅不改数据库，只把返回值里的 creditScore 变成动态计算结果
        user.setCreditScore(credit);

        return Result.success(toVO(user));
    }


    /**
     * 更新个人信息
     * POST /user/profile
     * body: { id, name, college, gradeClass, phone, email, commonCampus, profileRemark }
     */
    @PostMapping("/profile")
    public Result updateProfile(@RequestBody User dto) {
        if (dto.getId() == null) {
            return Result.fail("用户ID不能为空");
        }

        User user = userService.getById(dto.getId());
        if (user == null) {
            return Result.fail("用户不存在");
        }

        user.setName(dto.getName());
        user.setCollege(dto.getCollege());
        user.setGradeClass(dto.getGradeClass());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setCommonCampus(dto.getCommonCampus());
        user.setProfileRemark(dto.getProfileRemark());
        user.setStudentNo(dto.getStudentNo());

        boolean ok = userService.updateById(user);
        if (!ok) {
            return Result.fail("保存失败，请稍后重试");
        }

        // 返回最新的用户信息
        return Result.success(toVO(user));
    }

    // ✅ 账号查重：给前端实时提示用
    @GetMapping("/check-account")
    public Result checkAccount(@RequestParam("account") String account) {
        boolean exists = userService.existsByAccount(account);
        return Result.success(exists);
    }

    // ✅ 注册：真正写入数据库
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO dto) {
        if (dto == null
                || !StringUtils.hasText(dto.getAccount())
                || !StringUtils.hasText(dto.getName())
                || !StringUtils.hasText(dto.getStudentNo())
                || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getConfirmPassword())) {
            return Result.fail("注册信息不完整");
        }

        // 1) 密码一致性校验
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("密码不同");
        }

        // 2) 账号不能重复
        if (userService.existsByAccount(dto.getAccount())) {
            return Result.fail("该账号已存在");
        }

        // 3) 角色：注册一律学生，管理员账号建议由后台初始化/数据库预置
        int roleId = 1;

        User created = userService.register(
                dto.getAccount(),
                dto.getName(),
                dto.getStudentNo(),
                dto.getPassword(),
                roleId
        );

        return Result.success(toVO(created));

    }

    private com.ssrms.controller.vo.UserVO toVO(User u) {
        com.ssrms.controller.vo.UserVO vo = new com.ssrms.controller.vo.UserVO();
        vo.setId(u.getId());
        vo.setAccount(u.getAccount());
        vo.setName(u.getName());
        vo.setStudentNo(u.getStudentNo());
        vo.setRoleId(u.getRoleId());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setCollege(u.getCollege());
        vo.setGradeClass(u.getGradeClass());
        vo.setCommonCampus(u.getCommonCampus());
        vo.setProfileRemark(u.getProfileRemark());
        vo.setCreditScore(u.getCreditScore());
        vo.setBlacklistFlag(u.getBlacklistFlag());
        vo.setIsValid(u.getIsValid());
        vo.setCreateTime(u.getCreateTime());
        vo.setUpdateTime(u.getUpdateTime());
        vo.setLastLoginIp(u.getLastLoginIp());
        vo.setLastLoginTime(u.getLastLoginTime());
        return vo;
    }

    @Autowired
    private ViolationMapper violationMapper;

}