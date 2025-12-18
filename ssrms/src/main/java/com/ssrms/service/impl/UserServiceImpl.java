package com.ssrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.entity.User;
import com.ssrms.mapper.UserMapper;
import com.ssrms.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String account, String password, Integer roleId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getAccount, account)
                .eq(User::getPassword, password)
                .eq(User::getRoleId, roleId)
                .eq(User::getIsValid, "Y")
                .eq(User::getBlacklistFlag, 0)
                .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }

    @Override
    public boolean existsByAccount(String account) {
        if (!StringUtils.hasText(account)) return false;
        return lambdaQuery().eq(User::getAccount, account).count() > 0;
    }

    @Override
    public User register(String account, String name, String studentNo, String password, Integer roleId) {
        User u = new User();
        u.setAccount(account);
        u.setName(name);
        u.setStudentNo(studentNo);
        u.setPassword(password);

        // 你库里 role_id：0 管理员 / 1 学生
        u.setRoleId(roleId);

        // 给一些合理默认值（按你表字段）
        u.setIsValid("Y");
        u.setCreditScore(100);
        u.setBlacklistFlag(0);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());

        baseMapper.insert(u);
        return u;
    }
}
