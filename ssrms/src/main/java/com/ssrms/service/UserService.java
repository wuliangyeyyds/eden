package com.ssrms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ssrms.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    User login(String account, String password, Integer roleId);
}
