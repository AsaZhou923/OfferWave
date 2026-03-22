package com.offerwave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crawler_sync_logs")
public class CrawlerSyncLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchId;

    private Integer receivedCount;

    private Integer insertedCount;

    private Integer updatedCount;

    private Integer failedCount;

    private Integer status;

    private String errorMessage;

    private Long operatorUserId;

    private LocalDateTime createdAt;
}
