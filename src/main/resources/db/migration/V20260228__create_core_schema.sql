CREATE TABLE content_audit_logs
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT                             NULL COMMENT '触发内容审核的用户ID',
    content_type VARCHAR(50)                        NOT NULL COMMENT '内容类型（如 USER_NOTE）',
    content      TEXT                               NULL COMMENT '原始内容',
    hit_word     VARCHAR(100)                       NULL COMMENT '命中的敏感词',
    action       VARCHAR(50)                        NOT NULL COMMENT '处理动作（如 MASKED）',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '记录创建时间',
    INDEX idx_created_at (created_at),
    INDEX idx_user_id (user_id)
) COMMENT '内容审核日志' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE crawler_sync_errors
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id      VARCHAR(100)                       NULL COMMENT '爬虫同步批次号',
    unique_hash   CHAR(32)                           NULL COMMENT '职位去重哈希',
    payload       TEXT                               NULL COMMENT '原始异常数据快照',
    error_message VARCHAR(500)                       NOT NULL COMMENT '拦截原因',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '记录创建时间',
    INDEX idx_batch_id (batch_id),
    INDEX idx_created_at (created_at)
) COMMENT '爬虫异常拦截数据' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE crawler_sync_logs
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id         VARCHAR(100)                       NULL COMMENT '爬虫同步批次号',
    received_count   INT      DEFAULT 0                 NOT NULL COMMENT '接收条数',
    inserted_count   INT      DEFAULT 0                 NOT NULL COMMENT '新增条数',
    updated_count    INT      DEFAULT 0                 NOT NULL COMMENT '更新条数',
    failed_count     INT      DEFAULT 0                 NOT NULL COMMENT '失败条数',
    status           TINYINT  DEFAULT 1                 NOT NULL COMMENT '同步状态：1-成功, 0-失败',
    error_message    VARCHAR(500)                       NULL COMMENT '失败原因',
    operator_user_id BIGINT                             NULL COMMENT '触发同步的管理员用户ID',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '记录创建时间',
    INDEX idx_batch_id (batch_id),
    INDEX idx_created_at (created_at)
) COMMENT '爬虫同步日志' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE jobs
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name     VARCHAR(100)                          NOT NULL COMMENT '公司名称',
    company_type     VARCHAR(50)                           NOT NULL COMMENT '公司类型',
    company_business VARCHAR(100)                          NULL COMMENT '公司所属行业',
    job_title        VARCHAR(255)                          NOT NULL COMMENT '岗位名称',
    city             VARCHAR(50)                           NOT NULL COMMENT '工作地点',
    recruit_type     VARCHAR(20)                           NOT NULL COMMENT '招聘类型',
    target_audience  VARCHAR(50)                           NULL COMMENT '招聘对象',
    announcement     TEXT                                  NULL COMMENT '招聘公告',
    salary_range     VARCHAR(50)                           NULL COMMENT '薪资范围',
    salary_min       INT         DEFAULT 0                 NULL COMMENT '最低薪资',
    salary_max       INT         DEFAULT 0                 NULL COMMENT '最高薪资',
    education        VARCHAR(20)                           NULL COMMENT '学历要求',
    apply_link       TEXT                                  NULL COMMENT '投递链接',
    test_info        VARCHAR(50)                           NULL COMMENT '笔试情况',
    process_stage    VARCHAR(20) DEFAULT '网申开启'        NULL COMMENT '全局招聘进度',
    deadline         VARCHAR(20)                           NULL COMMENT '截止日期',
    unique_hash      CHAR(32)                              NULL COMMENT '职位去重哈希',
    source_origin    VARCHAR(20) DEFAULT '爬虫'            NOT NULL COMMENT '数据来源',
    audit_status     TINYINT     DEFAULT 0                 NOT NULL COMMENT '审核状态',
    created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '入库时间',
    updated_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_unique_hash UNIQUE (unique_hash),
    INDEX idx_audit_status (audit_status),
    INDEX idx_city (city),
    INDEX idx_company_name (company_name),
    INDEX idx_job_title (job_title),
    INDEX idx_salary (salary_min, salary_max)
) COMMENT '职位信息主表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE memberships
(
    id            INT                                      NOT NULL PRIMARY KEY,
    level_name    VARCHAR(50)                              NOT NULL COMMENT '等级名称',
    price         DECIMAL(10, 2) DEFAULT 0.00              NOT NULL COMMENT '价格',
    duration_days INT            DEFAULT 30                NOT NULL COMMENT '有效期天数',
    privileges    JSON                                     NULL COMMENT '权益配置',
    icon_url      VARCHAR(255)                             NULL COMMENT '等级图标',
    created_at    DATETIME       DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at    DATETIME       DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_level_name (level_name)
) COMMENT '会员等级配置表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE sensitive_words
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    word       VARCHAR(100)                       NOT NULL COMMENT '敏感词',
    enabled    TINYINT  DEFAULT 1                 NOT NULL COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_word UNIQUE (word)
) COMMENT '敏感词表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE system_configs
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key   VARCHAR(100)                       NOT NULL COMMENT '配置键',
    config_value JSON                               NOT NULL COMMENT '配置值',
    description  VARCHAR(255)                       NULL COMMENT '配置说明',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_config_key UNIQUE (config_key)
) COMMENT '系统配置表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE users
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    username             VARCHAR(50)                        NOT NULL COMMENT '登录账号',
    password_hash        VARCHAR(255)                       NOT NULL COMMENT 'BCrypt密码哈希值',
    role                 TINYINT  DEFAULT 0                 NOT NULL COMMENT '角色',
    account_status       TINYINT  DEFAULT 1                 NOT NULL COMMENT '账号状态',
    wechat_openid        VARCHAR(64)                        NULL COMMENT '微信唯一标识',
    nickname             VARCHAR(64)                        NULL COMMENT '用户昵称',
    membership_id        INT      DEFAULT 1                 NOT NULL COMMENT '会员等级',
    pref_industry        VARCHAR(255)                       NULL COMMENT '偏好行业',
    pref_city            VARCHAR(255)                       NULL COMMENT '偏好城市',
    pref_job             VARCHAR(255)                       NULL COMMENT '偏好岗位',
    education_background VARCHAR(20)                        NULL COMMENT '学历',
    salary               INT                                NULL COMMENT '期望薪资',
    custom_track_limit   INT                                NULL COMMENT '手工追踪额度上限',
    last_login           DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后登录时间',
    created_at           DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '注册时间',
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_wechat_openid UNIQUE (wechat_openid),
    INDEX idx_membership (membership_id)
) COMMENT '用户基础信息表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_job_status
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT                               NOT NULL COMMENT '关联用户 ID',
    job_id          BIGINT                               NOT NULL COMMENT '关联职位 ID',
    is_collected    TINYINT    DEFAULT 0                 NULL COMMENT '是否收藏',
    delivery_status TINYINT    DEFAULT 0                 NULL COMMENT '投递状态',
    user_note       VARCHAR(200)                         NULL COMMENT '用户个人备注',
    updated_at      DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '状态最后变更时间',
    created_at      DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '首次关联时间',
    CONSTRAINT uk_user_job UNIQUE (user_id, job_id),
    CONSTRAINT fk_status_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_status_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) COMMENT '用户-职位交互状态表' COLLATE = utf8mb4_unicode_ci;
