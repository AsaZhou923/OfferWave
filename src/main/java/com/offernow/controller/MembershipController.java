package com.offernow.controller;

import com.offernow.common.R;
import com.offernow.entity.Membership;
import com.offernow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员信息公开接口控制器。
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "公开模块", description = "无需登录即可访问的公开接口")
public class MembershipController {

    @Autowired
    private UserService userService;

    @GetMapping("/memberships")
    @Operation(summary = "获取会员等级列表", description = "返回可用会员等级、价格和权益信息")
    public R<List<Membership>> getAllMemberships() {
        List<Membership> memberships = userService.listMemberships();
        return R.success(memberships);
    }
}
