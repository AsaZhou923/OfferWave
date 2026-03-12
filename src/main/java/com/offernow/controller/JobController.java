package com.offernow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.common.R;
import com.offernow.dto.JobDetailDto;
import com.offernow.entity.Job;
import com.offernow.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开职位查询控制器。
 */
@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "公开职位查询", description = "提供职位公开搜索、筛选和详情展示")
@SecurityRequirement(name = "Authorization")
public class JobController {

    @Autowired
    private JobService jobService;

    @GetMapping
    @Operation(summary = "获取职位列表", description = "支持关键词、城市、行业、类型、薪资和排序筛选")
    public R<Page<Job>> searchJobs(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键词（匹配公司名/岗位名）") @RequestParam(required = false) String keyword,
            @Parameter(description = "城市（模糊匹配）") @RequestParam(required = false) String city,
            @Parameter(description = "行业（模糊匹配）") @RequestParam(required = false) String industry,
            @Parameter(description = "招聘类型（春招/秋招/实习）") @RequestParam(required = false) String recruit_type,
            @Parameter(description = "最低薪资（数值）") @RequestParam(required = false) Integer salary_min,
            @Parameter(description = "学历要求") @RequestParam(required = false) String education,
            @Parameter(description = "排序：newest（默认）/deadline/salary_desc")
            @RequestParam(required = false, defaultValue = "newest") String sort
    ) {
        Page<Job> pageInfo = new Page<>(page, size);
        Page<Job> resultPage = jobService.searchJobs(pageInfo, keyword, city, industry, recruit_type, salary_min, education, sort);
        return R.success(resultPage);
    }

    @GetMapping("/total")
    @Operation(summary = "获取公开职位总数", description = "返回当前已审核上线的职位总数，游客可访问")
    public R<Map<String, Long>> getPublicJobTotal() {
        return R.success(Map.of("total_count", jobService.countPublicJobs()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取职位详情", description = "返回职位详情；登录后额外返回当前用户对该职位的收藏/投递状态")
    public R<JobDetailDto> getJobById(
            @Parameter(description = "职位 ID") @PathVariable Long id,
            @Parameter(description = "可选。Bearer Token；传入后会返回 myStatus（含收藏状态）")
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        JobDetailDto jobDetail = jobService.getJobDetail(id);
        if (jobDetail == null) {
            return R.error(404, "职位不存在或未上线");
        }
        return R.success(jobDetail);
    }
}
