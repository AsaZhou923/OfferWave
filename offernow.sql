CREATE TABLE `jobs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键，自增',
  `company_name` VARCHAR(100) NOT NULL COMMENT '公司名称 (索引)',
  `company_type` VARCHAR(50) NOT NULL COMMENT '公司类型 (国企/外企等)',
  `company_business` VARCHAR(100) DEFAULT NULL COMMENT '公司所属行业(互联网/电商等)',
  `job_title` VARCHAR(255) NOT NULL COMMENT '岗位名称 (索引，支持模糊搜索)',
  `city` VARCHAR(50) NOT NULL COMMENT '工作地点 (索引)',
  `recruit_type` VARCHAR(20) NOT NULL COMMENT '招聘类型 (春招/秋招/实习)',
  `target_audience` VARCHAR(50) DEFAULT NULL COMMENT '招聘对象 (如：2026届)',
  `announcement` TEXT COMMENT '招聘公告',
  `salary_range` VARCHAR(50) DEFAULT NULL COMMENT '薪资范围 (用于高级筛选)',
  `salary_min` INT DEFAULT 0 COMMENT '最低薪资(元/月) - 用于范围筛选',
  `salary_max` INT DEFAULT 0 COMMENT '最高薪资(元/月) - 用于范围筛选',
  `education` VARCHAR(20) DEFAULT NULL COMMENT '学历要求 (本科/硕士等)',
  `apply_link` TEXT NOT NULL COMMENT '投递链接',
  `test_info` VARCHAR(50) DEFAULT NULL COMMENT '笔试情况',
  `process_stage` VARCHAR(20) DEFAULT '网申开启' COMMENT '全局招聘进度',
  `deadline` VARCHAR(20) DEFAULT NULL COMMENT '截止日期 (日期或招满即止)',
  `unique_hash` CHAR(32) NOT NULL COMMENT '爬虫去重哈希 (MD5)',
  `source_origin` VARCHAR(20) NOT NULL DEFAULT '爬虫' COMMENT '数据来源 (爬虫/人工/用户投稿)',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态 (0:待审, 1:上线, 2:拒绝)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间 (用于排序)',

  -- 索引设计
  UNIQUE INDEX `uk_unique_hash` (`unique_hash`) COMMENT '唯一哈希索引，防止爬虫数据重复插入',
  INDEX `idx_company_name` (`company_name`),
  INDEX `idx_job_title` (`job_title`),
  INDEX `idx_city` (`city`),
  INDEX `idx_audit_status` (`audit_status`) COMMENT '快速筛选已上线的职位',
  INDEX `idx_salary` (`salary_min`, `salary_max`) COMMENT '用于薪资范围查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位信息主表';

CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键，自增',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号(可以是手机号/邮箱/自定义字母)',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt密码哈希值',
  `wechat_openid` VARCHAR(64) DEFAULT NULL COMMENT '微信唯一标识 (唯一索引)',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
  `membership_id` INT NOT NULL DEFAULT 1 COMMENT '会员等级 (关联等级表 ID)',
  `pref_industry` VARCHAR(255) DEFAULT NULL COMMENT '偏好行业 (建议存储 JSON 数组或逗号分隔)',
  `pref_city` VARCHAR(255) DEFAULT NULL COMMENT '偏好城市',
  `pref_job` VARCHAR(255) DEFAULT NULL COMMENT '偏好岗位',
  `education_background` VARCHAR(20) DEFAULT NULL COMMENT '学历 (本科/硕士等)',
  `salary` INT DEFAULT NULL COMMENT '期望薪资 (月薪)',
  `last_login` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后登录时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',

  -- 索引设计
  UNIQUE INDEX `uk_wechat_openid` (`wechat_openid`) COMMENT '保证每个微信用户全局唯一',
  UNIQUE INDEX `uk_username` (`username`) COMMENT '确保账号不重复',
  INDEX `idx_membership` (`membership_id`) COMMENT '用于统计不同等级的用户分布'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础信息表';

CREATE TABLE `user_job_status` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键，自增',
  `user_id` BIGINT NOT NULL COMMENT '关联用户 ID (外键)',
  `job_id` BIGINT NOT NULL COMMENT '关联职位 ID (外键)',
  `is_collected` BOOLEAN DEFAULT FALSE COMMENT '是否收藏 (TRUE: 是, FALSE: 否)',
  `delivery_status` TINYINT DEFAULT 0 COMMENT '投递状态 (0:未投, 1:已投, 2:笔试, 3:面试, 4:录用, 5:感谢信)',
  `user_note` VARCHAR(200) DEFAULT NULL COMMENT '用户个人备注',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态最后变更时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次关联时间',

  -- 约束与索引
  UNIQUE KEY `uk_user_job` (`user_id`, `job_id`) COMMENT '确保每个用户对每个职位的状态记录唯一',
  INDEX `idx_user_id` (`user_id`) COMMENT '用于快速查询“我的收藏”或“我的投递”',

  -- 外键 (确保数据的完整性)
  CONSTRAINT `fk_status_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_status_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-职位交互状态表 (收藏与追踪)';

CREATE TABLE `memberships` (
  `id` INT PRIMARY KEY COMMENT '主键 ID (1: 普通用户, 2: VIP会员)',
  `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称 (如：“普通用户”、“VIP会员”)',
  `price` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '价格 (单位：元，0 表示免费)',
  `duration_days` INT NOT NULL DEFAULT 30 COMMENT '有效期天数 (30代表月卡, 365代表年卡, -1代表永久)',
  `privileges` JSON DEFAULT NULL COMMENT '权益配置 (存储具体权限：如最大追踪数、是否可看分析等)',
  `icon_url` VARCHAR(255) DEFAULT NULL COMMENT '等级图标 (前端显示的徽章图片链接)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  INDEX `idx_level_name` (`level_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级配置表';

-- 插入预设数据示例
INSERT INTO `memberships` (`id`, `level_name`, `price`, `duration_days`, `privileges`) VALUES
(1, '普通用户', 0.00, -1, '{"max_job_track": 10, "can_view_analysis": false, "refresh_limit": 5}'),
(2, 'VIP会员', 9.90, 30, '{"max_job_track": 999, "can_view_analysis": true, "refresh_limit": 100}');