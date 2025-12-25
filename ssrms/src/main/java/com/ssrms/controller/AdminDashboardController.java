package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.controller.vo.AdminDashboardHomeVO;
import com.ssrms.service.AdminDashboardService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard/admin")
public class AdminDashboardController {

    @Resource
    private AdminDashboardService adminDashboardService;

    @GetMapping("/home")
    public Result home(@RequestParam(defaultValue = "6") Integer recentLimit) {
        return Result.success(adminDashboardService.buildHome(recentLimit));
    }
}
