package com.offernow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crawler_sync_errors")
public class CrawlerSyncError implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchId;

    private String uniqueHash;

    private String payload;

    private String errorMessage;

    private LocalDateTime createdAt;
}
