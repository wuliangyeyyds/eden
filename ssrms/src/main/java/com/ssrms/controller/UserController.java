package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.controller.dto.LoginDTO;
import com.ssrms.entity.User;
import com.ssrms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO dto) {
        if (dto.getAccount() == null || dto.getPassword() == null || dto.getRoleId() == null) {
            return Result.fail("账号、密码和登录身份不能为空");
        }

        User user = userService.login(dto.getAccount(), dto.getPassword(), dto.getRoleId());
        if (user == null) {
            return Result.fail("账号或密码错误，或账号状态异常");
        }

        return Result.success(user);
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
        return Result.success(user);
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
        return Result.success(user);
    }
}