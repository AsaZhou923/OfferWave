# OfferWave 版本更新说明（2026-02-28）

> 历史版本记录。当前鉴权、验证码、爬虫和部署契约请以 `VERSION_UPDATE_2026-08-11.md` 与静态 API 文档为准；文中 API Key 和“验证码即注册”只描述当时状态，现已删除。

## 版本信息

- 版本号：`v1.1.0`
- 发布时间：`2026-02-28`
- 更新类型：功能增强 + 权限模型升级 + 管理后台能力补齐

## 本次更新内容

### 1. 权限与安全

- 内部爬虫接口 `/api/v1/internal/crawler/sync` 改为仅管理员可调用（`ROLE_ADMIN`）。
- 新增管理员 API 路由前缀：`/api/v1/admin/**`，统一管理员权限控制。
- 用户新增账号状态控制（正常/封禁），封禁用户登录会被拦截。

### 2. 招聘信息管理（管理员）

- 新增待办审核列表（`audit_status=0`）查询与批量审核（通过/驳回）。
- 新增职位库全量管理：查询、单条录入、批量录入、编辑、批量删除。
- 新增 Excel 批量导入职位能力（后台接口接收文件解析入库）。
- 新增批量公司进度更新（按公司统一更新 `process_stage`）。
- 新增过期职位清理（自动下架到驳回状态）。

### 3. 爬虫与数据源监控

- 新增同步日志看板数据：记录每次同步接收量、新增量、更新量、失败量、状态、错误信息。
- 新增异常拦截列表：记录爬虫上报中缺必填/格式错误的数据快照及错误原因。
- 爬虫同步返回新增字段：`failed_count`。

### 4. 用户与权益管理（管理员）

- 新增用户列表查询（支持状态筛选）。
- 新增用户封禁/解封接口。
- 新增权益手工发放：可修改 `membership_id` 与手工追踪额度上限。
- 用户追踪额度逻辑支持“手工额度优先于会员权益”。

### 5. 内容审核与敏感词

- 新增敏感词管理：新增、启停、删除、列表。
- 用户备注写入时接入敏感词过滤（命中词自动脱敏）。
- 新增内容审核日志，记录命中词与处理动作。

### 6. 系统配置管理

- 新增系统配置表与配置管理接口（用于运营位、首页推荐等可配置项）。
- 新增会员配置管理接口（可在后台修改价格、权益 JSON 等）。

## 兼容性说明

- 内部爬虫调用方式发生变更：
  - 旧方式：`X-API-KEY`
  - 新方式：管理员 JWT（`Authorization: Bearer <token>`）
- 若仍使用旧爬虫调用脚本，需要同步改造认证头。

## 数据库变更摘要

- `users` 新增字段：`account_status`、`custom_track_limit`（以及此前已新增的 `role`）。
- 新增表：
  - `crawler_sync_logs`
  - `crawler_sync_errors`
  - `sensitive_words`
  - `content_audit_logs`
  - `system_configs`

---

## 2026-03-01 增量更新（邮箱验证码登录 + 忘记密码）

### 1. 认证能力新增

- 新增发送验证码接口：`POST /api/v1/auth/send-email-code`
- 新增邮箱验证码登录接口：`POST /api/v1/auth/login/email`
- 新增重置密码接口：`POST /api/v1/auth/password/reset`
- 邮箱验证码登录采用“验证码即注册”策略：未注册邮箱首次登录时必须设置密码，系统创建账号后签发 JWT。
- 账号密码登录支持“邮箱作为用户名”输入（即 `/auth/login` 的 `username` 字段可直接填邮箱）。

### 2. 安全与风控

- 验证码仅存 Redis，不落 MySQL。
- 验证码有效期：5 分钟（Redis TTL 自动过期）。
- 防刷限制：
  - 同邮箱 1 分钟内仅允许发送 1 次；
  - 同 IP 1 分钟内仅允许发送 1 次；
  - 同邮箱 24 小时最多 10 次；
  - 同 IP 24 小时最多 10 次。
- 验证码使用后立即销毁（登录/重置密码成功后删除对应 Redis Key）。

### 3. 邮件服务配置

- 使用 QQ 邮箱 SMTP 服务发信。
- 默认配置：
  - `spring.mail.host=smtp.qq.com`
  - `spring.mail.port=465`
  - `spring.mail.properties.mail.smtp.ssl.enable=true`
- 运行时需配置：`MAIL_USERNAME`、`MAIL_FROM`、`MAIL_PASSWORD`（QQ 邮箱 SMTP 授权码）。

### 4. 数据模型与迁移

- `users` 新增字段：`email VARCHAR(100)`（独立邮箱）。
- `users.email` 增加唯一索引：`uk_email`。
- 新增迁移脚本：`src/main/resources/db/migration/V20260301__add_user_email.sql`

---

## 2026-03-02 增量更新（注册与登录规则收敛）
### 1. 注册规则调整
- 废弃“用户名+密码”注册流程。
- 注册改为强制 `邮箱 + 邮箱验证码 + 密码`。
- 注册成功后邮箱即账户标识（内部自动生成唯一 username）。
- 新注册用户继续发放 7 天试用会员（VIP）。

### 2. 登录规则确认
- 支持“邮箱 + 密码”登录（`/api/v1/auth/login`，邮箱填写在 `username` 字段）。
- 支持“邮箱 + 验证码”登录（`/api/v1/auth/login/email`）。
- 取消“邮箱验证码登录自动注册”；未注册邮箱会直接返回失败。

### 3. 验证码类型调整
- `send-email-code` 新增 `register` 类型。
- 当前支持三种类型：`register` / `login` / `reset_pwd`。

### 4. 初始化 SQL 补齐
- `offerwave.sql` 的 `users` 表补充 `membership_expire_at` 字段，保证试用会员到期逻辑在全量初始化场景可用。
