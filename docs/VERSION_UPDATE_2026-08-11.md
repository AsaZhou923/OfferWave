# OfferWave 安全与一致性更新（2026-08-11）

## 发布状态

- 本地整改：完成
- 自动化回归：67 tests，0 failures / errors / skipped
- 真实基础设施验证：MySQL 8.4、Redis 7 通过
- 发布分支：`agent/offerwave-security-remediation`
- 远程 GitHub Actions：以该发布分支草稿 PR 的精确 head checks 为准

本版本关闭 2026-08-11 项目审查中记录的全部高风险、中风险与观察项。外部审查报告保留原始发现，并在每项后标记修复实现与验证证据。

## 主要变更

### 认证与授权

- 删除普通用户自助升级会员的测试路由、DTO 和 Service 方法；
- 管理员用户列表改用 `AdminUserResponseDto`，实体密码哈希增加序列化与日志纵深防护；
- JWT 密钥改为必填 Base64，解码后必须至少 32 字节，配置错误时启动失败；
- JWT 与用户密码哈希的凭据版本绑定，密码重置后旧 Token 自动失效；
- 只接受标准 `Authorization: Bearer <JWT>`，移除查询参数、Cookie、裸 Token、旧 Header 和爬虫 API Key；
- 验证码使用 `SecureRandom`，Redis Lua 原子校验/计数/消费，5 次错误后销毁；
- 验证码发送对外采用统一消息，并按邮箱/IP 设置 1 分钟冷却与每日 10 次上限；
- 只有显式可信代理才能提供 `X-Forwarded-For` / `X-Real-IP`；
- CORS 改为精确 Origin 白名单且 `allowCredentials=false`；
- `/admin/**` 与 `/internal/crawler/**` 保持管理员角色校验。

### 职位与数据一致性

- 职位身份由服务端统一生成：`MD5(UTF-8(lower(trim(company))|lower(trim(title))|lower(trim(city))))`；
- `unique_hash` 改为可写、`NOT NULL`、唯一，迁移对历史重复数据进行确定性保留；
- 管理员导入和爬虫同步共用同一生成器，客户端哈希仅作兼容提示；
- 职位列表先执行 `newest` / `deadline` / `salary_desc` 排序，再在前 30 条中按公司轮询；
- 普通用户仍保留最多查看前 30 条的产品权益，VIP 可继续分页；
- 爬虫批量写入与同步/失败审计分离事务；
- 追踪额度使用事务与行锁，配置缺失时 fail closed；
- 资料浏览量、下载量改为数据库原子自增。

### API、安全边界与上传

- `R.code` 与真实 HTTP 状态同步；
- 5xx 响应统一脱敏为 `服务器内部错误`；
- 全局分页约束为 `page >= 1`、`1 <= size <= 100`；
- 资料图片同时校验扩展名、声明 MIME 与 PNG/JPEG/GIF/WebP magic bytes；
- 职位状态增加 `0..5` 与备注 200 字 Bean Validation；
- 管理员文件导入失败不再向客户端回显解析器/IO 异常细节。

### 数据库与部署

- 引入 Flyway 11.20.2 与 MySQL database module；
- 新增空库核心 migration、既有 2026-03 migration 和 `V20260811` 修复 migration；
- 空库启动自动创建完整 schema，并幂等写入 ID 1/2 的会员配置；
- 新增环境变量驱动的一次性管理员引导，密码只以 BCrypt 保存且不进入审计值；
- 既有手工数据库只能在显式 `OFFERWAVE_DATABASE_BASELINE_EXISTING_SCHEMA=true` 时接管；默认遇到未知非空 schema 会拒绝启动；
- `offerwave.sql` 与 `script.sql` 已同步为当前快照，但不替代 Flyway。

### 工程保障

- 恢复 GitHub Actions：Ubuntu、MySQL 8.4、Redis 7、`clean verify`、空库迁移/API 冒烟、SLO 单测、High 级 SpotBugs 门禁；
- PR 启用 Dependency Review v4；
- Dependabot 每周检查 Maven 与 GitHub Actions；
- 新增无第三方 Python 依赖的职位列表/搜索 p95 SLO 工具；
- 新增 `ADR-0001-email-first-authentication.md`，正式确定 v1 采用邮箱优先认证，未实现的微信运行配置/API 占位已删除；历史 schema 字段保留用于兼容既有数据库。

## 数据库升级说明

### 新环境

创建空数据库后直接启动应用。Flyway 会依次执行：

1. `V20260228__create_core_schema.sql`
2. `V20260301__add_user_email.sql`
3. `V20260302__add_membership_expire_at.sql`
4. `V20260309__add_material_package_tables.sql`
5. `V20260811__repair_job_identity_and_seed_memberships.sql`

### 既有非空数据库

先备份并确认历史结构至少到 2026-03-09，再只为第一次受管启动设置：

```text
OFFERWAVE_DATABASE_BASELINE_EXISTING_SCHEMA=true
```

成功后必须移除。系统会写入 `20260309` baseline，再执行 `V20260811`。对一个无 Flyway history 的非空测试库，在未设置该开关时已经验证会 fail closed。

## 发布前必须配置

- `JWT_SECRET`：至少 32 个随机字节的 Base64；
- MySQL、Redis 连接与凭据；
- `MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM` 等 SMTP 配置；
- `OFFERWAVE_CORS_ALLOWED_ORIGINS`：精确前端 Origin；
- 如有反向代理，设置 `OFFERWAVE_TRUSTED_PROXY_ADDRESSES`；
- 仅首位管理员创建期间设置三个 `OFFERWAVE_BOOTSTRAP_ADMIN_*` 变量，成功后移除。

## 破坏性兼容提醒

- 部署后旧 JWT 与旧 Redis 验证码应视为失效；
- 前端、脚本必须统一改用 Bearer Header；
- 爬虫改用管理员 JWT，不再发送 `X-API-KEY`；
- 客户端不得调用已删除的 `/user/membership/upgrade`；
- 客户端应按真实 HTTP 状态处理错误，并遵守 `size <= 100`。

## 本地验收证据

| 验证项 | 结果 |
|---|---|
| Maven `clean verify` | 23 个测试类、67 tests，全部通过 |
| 打包 | 约 70 MiB 可执行 JAR 生成成功 |
| JWT 配置 | 合法 Base64 密钥启动成功；缺失密钥 fail fast |
| MySQL 8.4 空库 | 5 个 migration 全部成功，最终版本 `20260811` |
| MySQL 8.4 既有库接管 | `20260309` baseline + `20260811` migration 成功 |
| 未授权既有库接管 | 未开启 baseline 开关时启动失败，未静默接管 |
| Redis 7 | 登录/验证码依赖连接与 API 冒烟环境可用 |
| HTTP 安全链 | 登录、Bearer、管理员、分页、CORS 与已删除路由均按预期 |
| 管理员用户列表 | 响应中不存在 `passwordHash` / `password_hash` |
| 职位身份 | 服务端规范哈希与数据库值一致；重复写入受唯一约束保护 |
| SLO（300 条职位、并发 10、各 100 请求） | 列表 p95 0.0187s；搜索 p95 0.0172s；均低于 3s/2s 目标；结果保存在 `target/performance-slo.json` |
| Python SLO 单测 | 3 tests 全部通过 |
| SpotBugs High 门禁 | 通过，High warning 为 0 |

以上是本地与隔离容器环境证据。整改已收敛到独立发布分支；GitHub 托管 CI 以该分支草稿 PR 的精确 head checks 为准。生产 SMTP 实际送达仍属于部署验收项，不把未发生的外部验证标记为完成。
