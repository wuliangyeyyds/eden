package com.ssrms.service;

import com.ssrms.controller.vo.AdminDashboardHomeVO;

public interface AdminDashboardService {

    /**
     * 构建管理员首页看板数据
     * @param recentLimit 最近动态条数
     */
    AdminDashboardHomeVO buildHome(int recentLimit);
}
