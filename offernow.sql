create table content_audit_logs
(
    id           bigint auto_increment
        primary key,
    user_id      bigint                             null comment '触发内容审核的用户ID',
    content_type varchar(50)                        not null comment '内容类型（如 USER_NOTE）',
    content      text                               null comment '原始内容',
    hit_word     varchar(100)                       null comment '命中的敏感词',
    action       varchar(50)                        not null comment '处理动作（如 MASKED）',
    created_at   datetime default CURRENT_TIMESTAMP not null comment '记录创建时间'
)
    comment '内容审核日志' collate = utf8mb4_unicode_ci;

create index idx_created_at
    on content_audit_logs (created_at);

create index idx_user_id
    on content_audit_logs (user_id);

create table crawler_sync_errors
(
    id            bigint auto_increment
        primary key,
    batch_id      varchar(100)                       null comment '爬虫同步批次号',
    unique_hash   char(32)                           null comment '职位去重哈希',
    payload       text                               null comment '原始异常数据快照',
    error_message varchar(500)                       not null comment '拦截原因',
    created_at    datetime default CURRENT_TIMESTAMP not null comment '记录创建时间'
)
    comment '爬虫异常拦截数据' collate = utf8mb4_unicode_ci;

create index idx_batch_id
    on crawler_sync_errors (batch_id);

create index idx_created_at
    on crawler_sync_errors (created_at);

create table crawler_sync_logs
(
    id               bigint auto_increment
        primary key,
    batch_id         varchar(100)                       null comment '爬虫同步批次号',
    received_count   int      default 0                 not null comment '接收条数',
    inserted_count   int      default 0                 not null comment '新增条数',
    updated_count    int      default 0                 not null comment '更新条数',
    failed_count     int      default 0                 not null comment '失败条数',
    status           tinyint  default 1                 not null comment '同步状态：1-成功, 0-失败',
    error_message    varchar(500)                       null comment '失败原因',
    operator_user_id bigint                             null comment '触发同步的管理员用户ID',
    created_at       datetime default CURRENT_TIMESTAMP not null comment '记录创建时间'
)
    comment '爬虫同步日志' collate = utf8mb4_unicode_ci;

create index idx_batch_id
    on crawler_sync_logs (batch_id);

create index idx_created_at
    on crawler_sync_logs (created_at);

create table jobs
(
    id               bigint auto_increment comment '主键，自增'
        primary key,
    company_name     varchar(100)                          not null comment '公司名称 (索引)',
    company_type     varchar(50)                           not null comment '公司类型 (国企/外企等)',
    company_business varchar(100)                          null comment '公司所属行业(互联网/电商等)',
    job_title        varchar(255)                          not null comment '岗位名称 (索引，支持模糊搜索)',
    city             varchar(50)                           not null comment '工作地点 (索引)',
    recruit_type     varchar(20)                           not null comment '招聘类型 (春招/秋招/实习)',
    target_audience  varchar(50)                           null comment '招聘对象 (如：2026届)',
    announcement     text                                  null comment '招聘公告',
    salary_range     varchar(50)                           null comment '薪资范围 (用于高级筛选)',
    salary_min       int         default 0                 null comment '最低薪资(元/月) - 用于范围筛选',
    salary_max       int         default 0                 null comment '最高薪资(元/月) - 用于范围筛选',
    education        varchar(20)                           null comment '学历要求 (本科/硕士等)',
    apply_link       text                                  null comment '投递链接',
    test_info        varchar(50)                           null comment '笔试情况',
    process_stage    varchar(20) default '网申开启'        null comment '全局招聘进度',
    deadline         varchar(20)                           null comment '截止日期 (日期或招满即止)',
    unique_hash      char(32)                              null comment '爬虫去重哈希 (MD5)',
    source_origin    varchar(20) default '爬虫'            not null comment '数据来源 (爬虫/人工/用户投稿)',
    audit_status     tinyint     default 0                 not null comment '审核状态 (0:待审, 1:上线, 2:拒绝)',
    created_at       datetime    default CURRENT_TIMESTAMP not null comment '入库时间',
    updated_at       datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间 (用于排序)',
    constraint uk_unique_hash
        unique (unique_hash) comment '唯一哈希索引，防止爬虫数据重复插入'
)
    comment '职位信息主表' collate = utf8mb4_unicode_ci;

create index idx_audit_status
    on jobs (audit_status)
    comment '快速筛选已上线的职位';

create index idx_city
    on jobs (city);

create index idx_company_name
    on jobs (company_name);

create index idx_job_title
    on jobs (job_title);

create index idx_salary
    on jobs (salary_min, salary_max)
    comment '用于薪资范围查询';

create table memberships
(
    id            int                                      not null comment '主键 ID (1: 普通用户, 2: VIP会员)'
        primary key,
    level_name    varchar(50)                              not null comment '等级名称 (如：“普通用户”、“VIP会员”)',
    price         decimal(10, 2) default 0.00              not null comment '价格 (单位：元，0 表示免费)',
    duration_days int            default 30                not null comment '有效期天数 (30代表月卡, 365代表年卡, -1代表永久)',
    privileges    json                                     null comment '权益配置 (存储具体权限：如最大追踪数、是否可看分析等)',
    icon_url      varchar(255)                             null comment '等级图标 (前端显示的徽章图片链接)',
    created_at    datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '会员等级配置表' collate = utf8mb4_unicode_ci;

create index idx_level_name
    on memberships (level_name);

create table sensitive_words
(
    id         bigint auto_increment
        primary key,
    word       varchar(100)                       not null comment '敏感词',
    enabled    tinyint  default 1                 not null comment '是否启用：1-启用, 0-停用',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_word
        unique (word)
)
    comment '敏感词表' collate = utf8mb4_unicode_ci;

create table system_configs
(
    id           bigint auto_increment
        primary key,
    config_key   varchar(100)                       not null comment '配置键',
    config_value json                               not null comment '配置值（JSON）',
    description  varchar(255)                       null comment '配置说明',
    created_at   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_config_key
        unique (config_key)
)
    comment '系统配置表' collate = utf8mb4_unicode_ci;

create table users
(
    id                   bigint auto_increment comment '主键，自增'
        primary key,
    username             varchar(50)                        not null comment '登录账号(可以是手机号/邮箱/自定义字母)',
    email                varchar(100)                       null comment 'independent email for code-login and password reset',
    password_hash        varchar(255)                       not null comment 'BCrypt密码哈希值',
    role                 tinyint  default 0                 not null comment '角色：0-普通用户, 1-系统管理员',
    account_status       tinyint  default 1                 not null comment '账号状态：1-正常, 0-封禁',
    wechat_openid        varchar(64)                        null comment '微信唯一标识(暂留空)',
    nickname             varchar(64)                        null comment '用户昵称',
    membership_id        int      default 1                 not null comment '会员等级 (关联等级表 ID)',
    membership_expire_at datetime                           null comment '会员到期时间（NULL 表示永久）',
    pref_industry        varchar(255)                       null comment '偏好行业 (建议存储 JSON 数组或逗号分隔)',
    pref_city            varchar(255)                       null comment '偏好城市',
    pref_job             varchar(255)                       null comment '偏好岗位',
    education_background varchar(20)                        null comment '学历 (本科/硕士等)',
    salary               int                                null comment '期望薪资 (月薪)',
    custom_track_limit   int                                null comment '手工追踪额度上限（NULL=按会员权益）',
    last_login           datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后登录时间',
    created_at           datetime default CURRENT_TIMESTAMP null comment '注册时间',
    constraint uk_email
        unique (email),
    constraint uk_username
        unique (username),
    constraint uk_wechat_openid
        unique (wechat_openid) comment '保证每个微信用户全局唯一'
)
    comment '用户基础信息表' collate = utf8mb4_unicode_ci;

create table user_job_status
(
    id              bigint auto_increment comment '主键，自增'
        primary key,
    user_id         bigint                               not null comment '关联用户 ID (外键)',
    job_id          bigint                               not null comment '关联职位 ID (外键)',
    is_collected    tinyint(1) default 0                 null comment '是否收藏 (TRUE: 是, FALSE: 否)',
    delivery_status tinyint    default 0                 null comment '投递状态 (0:未投, 1:已投, 2:笔试, 3:面试, 4:录用, 5:感谢信)',
    user_note       varchar(200)                         null comment '用户个人备注',
    updated_at      datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '状态最后变更时间',
    created_at      datetime   default CURRENT_TIMESTAMP not null comment '首次关联时间',
    constraint uk_user_job
        unique (user_id, job_id) comment '确保每个用户对每个职位的状态记录唯一',
    constraint fk_status_job
        foreign key (job_id) references jobs (id)
            on delete cascade,
    constraint fk_status_user
        foreign key (user_id) references users (id)
            on delete cascade
)
    comment '用户-职位交互状态表 (收藏与追踪)' collate = utf8mb4_unicode_ci;

create index idx_user_id
    on user_job_status (user_id)
    comment '用于快速查询“我的收藏”或“我的投递”';

create index idx_membership
    on users (membership_id)
    comment '用于统计不同等级的用户分布';


