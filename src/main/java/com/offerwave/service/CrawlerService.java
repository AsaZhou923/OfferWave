package com.offerwave.service;

import com.offerwave.dto.CrawlerSyncDto;

import java.util.Map;

/**
 * 爬虫数据接入服务接口。
 */
public interface CrawlerService {

    /**
     * 批量同步招聘数据。
     *
     * @param syncDto 爬虫上报的批量数据
     * @return 同步统计结果（接收/新增/更新）
     */
    Map<String, Integer> syncJobs(CrawlerSyncDto syncDto);
}
