package com.offernow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offernow.entity.CrawlerSyncError;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrawlerSyncErrorMapper extends BaseMapper<CrawlerSyncError> {
}
