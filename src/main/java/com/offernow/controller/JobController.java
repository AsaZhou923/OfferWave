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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public job query controller.
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
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键词（公司/岗位）") @RequestParam(required = false) String keyword,
            @Parameter(description = "城市（模糊匹配）") @RequestParam(required = false) String city,
            @Parameter(description = "行业（模糊匹配）") @RequestParam(required = false) String industry,
            @Parameter(description = "招聘类型") @RequestParam(required = false) String recruit_type,
            @Parameter(description = "最低薪资") @RequestParam(required = false) Integer salary_min,
            @Parameter(description = "学历要求") @RequestParam(required = false) String education,
            @Parameter(description = "排序：newest/deadline/salary_desc")
            @RequestParam(required = false, defaultValue = "newest") String sort
    ) {
        Page<Job> pageInfo = new Page<>(page, size);
        Page<Job> resultPage = jobService.searchJobs(pageInfo, keyword, city, industry, recruit_type, salary_min, education, sort);
        return R.success(resultPage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取职位详情", description = "返回职位公开详情；登录后可附带我的状态")
    public R<JobDetailDto> getJobById(@Parameter(description = "职位 ID") @PathVariable Long id) {
        JobDetailDto jobDetail = jobService.getJobDetail(id);
        if (jobDetail == null) {
            return R.error(404, "职位不存在或未上线");
        }
        return R.success(jobDetail);
    }
}