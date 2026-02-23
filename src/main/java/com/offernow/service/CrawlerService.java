package com.offernow.service;

import com.offernow.dto.CrawlerSyncDto;
import java.util.Map;

/**
 * 爬虫数据接入服务接口
 */
public interface CrawlerService {

    /**
     * 批量同步招聘数据
     * @param syncDto 爬虫上报的数据
     * @return 包含新增和更新数量的 Map
     */
    Map<String, Integer> syncJobs(CrawlerSyncDto syncDto);
}
