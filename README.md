# OfferWave Backend

OfferWave 是面向求职场景的 Spring Boot 后端，提供邮箱认证、职位检索、求职进度追踪、会员权益、资料内容、管理员运营和内部爬虫同步能力。

## 技术基线

- Java 17
- Maven 3.9+
- MySQL 8.4
- Redis 7
- Spring Boot 3.1.5、MyBatis-Plus、Spring Security、Flyway 11

## 本地启动

### 1. 准备依赖

创建一个空的 MySQL 数据库 `offerwave`，并启动 Redis。新环境不需要手工执行 `offerwave.sql`；应用启动时会自动执行 `src/main/resources/db/migration` 中的 Flyway 迁移，并写入基础会员数据。

`offerwave.sql` 和 `script.sql` 仅作为当前 schema 快照与人工核对材料，Flyway migration 才是数据库结构的唯一执行源。

### 2. 配置环境变量

最低启动配置如下：

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/offerwave?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='offerwave'
export DB_PASSWORD='replace-with-database-password'
export REDIS_HOST='127.0.0.1'
export REDIS_PORT='6379'
export JWT_SECRET="$(openssl rand -base64 32)"
```

`JWT_SECRET` 必须是至少 32 个随机字节的 Base64 编码。缺失、不是合法 Base64 或解码后不足 32 字节时，应用会拒绝启动。Windows PowerShell 可这样生成：

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

生产环境还应明确配置：

| 环境变量 | 用途 | 要求 |
|---|---|---|
| `MAIL_HOST`、`MAIL_PORT` | SMTP 服务 | 邮箱认证上线前必填 |
| `MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM` | SMTP 凭据与发件人 | 不得提交到仓库 |
| `OFFERWAVE_CORS_ALLOWED_ORIGINS` | 浏览器前端 Origin 白名单 | 逗号分隔的精确 Origin，不允许 `*` |
| `OFFERWAVE_TRUSTED_PROXY_ADDRESSES` | 可提供转发 IP Header 的代理地址 | 逗号分隔；无代理时留空 |
| `OFFERWAVE_STORAGE_ROOT` | 上传文件根目录 | 使用持久化、受限目录 |
| `OFFERWAVE_STORAGE_PUBLIC_URL_PREFIX` | 上传文件公开 URL 前缀 | 默认 `/uploads` |

应用只接受 `Authorization: Bearer <JWT>`。查询参数、Cookie、`token`、`X-ACCESS-TOKEN` 与旧爬虫 `X-API-KEY` 均不再作为认证入口。

### 3. 可选：一次性创建首位管理员

仅在数据库还没有管理员时设置以下环境变量：

```bash
export OFFERWAVE_BOOTSTRAP_ADMIN_USERNAME='initial-admin'
export OFFERWAVE_BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export OFFERWAVE_BOOTSTRAP_ADMIN_PASSWORD='replace-with-12-plus-character-secret'
```

启动后系统会用 BCrypt 创建管理员并记录一次性审计标记；密码不会写入审计记录。确认创建成功后立即从部署配置中移除这三个变量。若已经存在管理员，引导程序不会改写账号。

### 4. 启动

```bash
mvn spring-boot:run
```

默认端口为 `8080`。启动后可访问：

- OpenAPI UI：`http://127.0.0.1:8080/doc.html`
- 静态接口契约：`docs/OfferWave平台 API 接口文档.md`

## 既有手工建库的迁移接管

对历史上通过 SQL 手工建立、已经包含业务数据但没有 `flyway_schema_history` 的数据库：

1. 先完成可恢复备份，并确认 schema 至少已经包含截至 2026-03-09 的结构；
2. 仅首次接管时设置 `OFFERWAVE_DATABASE_BASELINE_EXISTING_SCHEMA=true`；
3. 启动应用，确认 Flyway 建立 `20260309` baseline，并成功执行 `V20260811` 修复迁移；
4. 验证数据与接口后，立即移除该环境变量，后续只允许正常版本迁移。

不要把此开关用于空数据库，也不要长期保留。默认关闭是为了防止应用把未知的非空 schema 误判为已受管；关闭状态下遇到非空且无历史表的数据库会 fail closed。

## 验证

```bash
mvn clean verify
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s scripts/performance -p 'test_*.py' -v
mvn com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:check \
  -Dspotbugs.effort=Max \
  -Dspotbugs.threshold=High \
  -Dspotbugs.failOnError=true
```

针对正在运行的实例执行职位列表与搜索 SLO 检查：

```bash
python3 scripts/performance/offerwave_slo_check.py \
  --base-url http://127.0.0.1:8080/api/v1 \
  --requests 100 \
  --concurrency 10 \
  --output target/performance-slo.json
```

GitHub Actions 使用 Ubuntu、MySQL 8.4 和 Redis 7 执行干净构建、迁移/API 冒烟、SLO 单测、High 级 SpotBugs 门禁和 PR 依赖审查。

## 文档

- `docs/OfferWave平台 API 接口文档.md`：当前 API 契约
- `docs/ADR-0001-email-first-authentication.md`：v1 邮箱优先认证决策
- `docs/VERSION_UPDATE_2026-08-11.md`：安全与一致性整改记录、迁移和发布注意事项
- `offerwave.sql` / `script.sql`：schema 快照，不替代 Flyway migration
