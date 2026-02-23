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

@RestController
@RequestMapping("/api/v1")
@Tag(name = "公共模块", description = "无需登录即可访问的接口")
public class MembershipController {

    @Autowired
    private UserService userService;

    @GetMapping("/memberships")
    @Operation(summary = "获取会员等级列表", description = "获取所有可用的会员等级及其价格、权益，用于“会员购买”页面展示。")
    public R<List<Membership>> getAllMemberships() {
        List<Membership> memberships = userService.listMemberships();
        return R.success(memberships);
    }
}
