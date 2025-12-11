package com.ssrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssrms.entity.User;
import com.ssrms.mapper.UserMapper;
import com.ssrms.service.UserService;
import org.springframework.stereotype.Service;

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
}
