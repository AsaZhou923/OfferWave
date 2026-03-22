package com.offerwave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("material_downloads")
public class MaterialDownload implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long packageId;

    private String title;

    private String downloadUrl;

    private String extractionCode;

    private String fileType;

    private String fileSize;

    private String description;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
