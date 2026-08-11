CREATE TABLE material_categories
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)                         NOT NULL COMMENT '资料分类名称',
    slug        VARCHAR(100)                         NOT NULL COMMENT '资料分类唯一标识',
    description VARCHAR(255)                         NULL COMMENT '分类说明',
    sort_order  INT       DEFAULT 0                  NOT NULL COMMENT '排序值，越小越靠前',
    status      TINYINT   DEFAULT 1                  NOT NULL COMMENT '状态：0-停用，1-启用',
    created_at  DATETIME  DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    updated_at  DATETIME  DEFAULT CURRENT_TIMESTAMP  NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_material_category_slug UNIQUE (slug)
) COMMENT '资料包分类表';

CREATE TABLE material_packages
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     BIGINT                             NOT NULL COMMENT '所属分类ID',
    title           VARCHAR(150)                       NOT NULL COMMENT '资料包标题',
    slug            VARCHAR(150)                       NOT NULL COMMENT '资料包唯一标识',
    subtitle        VARCHAR(255)                       NULL COMMENT '资料包副标题',
    icon_url        VARCHAR(255)                       NULL COMMENT '卡片图标',
    cover_image_url VARCHAR(255)                       NULL COMMENT '封面图',
    excerpt         VARCHAR(500)                       NULL COMMENT '摘要',
    content         MEDIUMTEXT                         NOT NULL COMMENT '正文内容，建议存 HTML 或 Markdown',
    preview_images  JSON                               NULL COMMENT '正文图片列表',
    file_catalog    JSON                               NULL COMMENT '文件目录列表',
    download_tip    VARCHAR(255)                       NULL COMMENT '下载提示语',
    access_type     TINYINT   DEFAULT 1                NOT NULL COMMENT '访问类型：0-公开下载，1-会员下载',
    status          TINYINT   DEFAULT 1                NOT NULL COMMENT '状态：0-草稿，1-已发布，2-已下线',
    view_count      BIGINT    DEFAULT 0                NOT NULL COMMENT '浏览次数',
    download_count  BIGINT    DEFAULT 0                NOT NULL COMMENT '下载次数',
    sort_order      INT       DEFAULT 0                NOT NULL COMMENT '排序值，越小越靠前',
    published_at    DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '发布时间',
    created_at      DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at      DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_material_package_slug UNIQUE (slug),
    CONSTRAINT fk_material_package_category FOREIGN KEY (category_id) REFERENCES material_categories (id)
) COMMENT '资料包主表';

CREATE INDEX idx_material_package_category_status
    ON material_packages (category_id, status, sort_order);

CREATE TABLE material_downloads
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id      BIGINT                             NOT NULL COMMENT '所属资料包ID',
    title           VARCHAR(150)                       NOT NULL COMMENT '下载项标题',
    download_url    VARCHAR(1000)                      NOT NULL COMMENT '下载地址',
    extraction_code VARCHAR(100)                       NULL COMMENT '提取码',
    file_type       VARCHAR(50)                        NULL COMMENT '文件类型',
    file_size       VARCHAR(50)                        NULL COMMENT '文件大小',
    description     VARCHAR(255)                       NULL COMMENT '下载项说明',
    sort_order      INT       DEFAULT 0                NOT NULL COMMENT '排序值',
    status          TINYINT   DEFAULT 1                NOT NULL COMMENT '状态：0-停用，1-启用',
    created_at      DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at      DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_material_download_package FOREIGN KEY (package_id) REFERENCES material_packages (id) ON DELETE CASCADE
) COMMENT '资料包下载资源表';

CREATE INDEX idx_material_download_package_status
    ON material_downloads (package_id, status, sort_order);
