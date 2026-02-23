package com.offernow.controller;

import com.offernow.common.R;
import com.offernow.dto.CrawlerSyncDto;
import com.offernow.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/crawler")
@Tag(name = "内部模块 - 爬虫", description = "供内部爬虫程序调用的数据同步接口")
@SecurityRequirement(name = "ApiKeyAuth")
public class CrawlerController {

    @Autowired
    private CrawlerService crawlerService;

    @PostMapping("/sync")
    @Operation(summary = "批量上报招聘数据", description = "接收爬虫清洗后的结构化数据列表，进行去重和更新。")
    public R<Map<String, Integer>> syncData(@RequestBody CrawlerSyncDto syncDto) {
        if (syncDto == null || syncDto.getItems() == null || syncDto.getItems().isEmpty()) {
            return R.error(400, "items 列表不能为空");
        }

        Map<String, Integer> result = crawlerService.syncJobs(syncDto);
        return R.success(result);
    }
}
