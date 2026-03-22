package com.offerwave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("material_packages")
public class MaterialPackage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String title;

    private String slug;

    private String subtitle;

    private String iconUrl;

    private String coverImageUrl;

    private String excerpt;

    private String content;

    private String previewImages;

    private String fileCatalog;

    private String downloadTip;

    private Integer accessType;

    private Integer status;

    private Long viewCount;

    private Long downloadCount;

    private Integer sortOrder;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
