# OfferNow Backend

OfferNow 平台后端服务，提供职位查询、用户认证、职位追踪、会员体系、管理员后台及爬虫数据接入能力。

## 功能概览
- 用户注册/登录（JWT）
- 职位列表与详情查询（支持筛选、排序、分页）
- 用户个人中心（偏好设置、我的职位、会员升级）
- 管理员后台（职位审核/管理、用户管理、敏感词审核、系统配置）
- 爬虫数据批量同步（仅管理员 JWT 调用）
- 统一响应结构：`code` / `message` / `data`

## 技术栈
- Java 17
- Spring Boot 3.1.5
- Spring Security
- MyBatis-Plus 3.5.3.1
- MySQL
- Redis
- JWT（jjwt 0.11.5）
- Knife4j / OpenAPI 3

## 项目结构
```text
src/main/java/com/offernow
├─ common         # 统一返回体、异常处理
├─ config         # 安全、MVC、Bean 配置
├─ controller     # API 控制器
├─ dto            # 请求/响应 DTO
├─ entity         # 数据实体
├─ interceptor    # JWT 过滤器
├─ mapper         # MyBatis-Plus Mapper
├─ service        # 业务逻辑
└─ util           # JWT 工具等

src/main/resources
└─ application.yml
```

## 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8+
- Redis 6+

## 快速启动
1. 初始化数据库
- 执行 `offernow.sql`。

2. 修改配置
- 编辑 `src/main/resources/application.yml`：
  - `spring.datasource.url/username/password`
  - `spring.data.redis.host/port/password`
  - `offernow.jwt.secret`

推荐优先使用环境变量覆盖配置（避免将真实密钥写入仓库）：

| 环境变量 | 说明 |
| --- | --- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接信息 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | Redis 连接信息 |
| `WECHAT_APPID` / `WECHAT_SECRET` | 微信登录配置 |
| `JWT_SECRET` / `JWT_EXPIRATION` | JWT 密钥与过期时间 |

> 未配置环境变量时的影响：
> - 微信相关变量（`WECHAT_APPID` / `WECHAT_SECRET`）为空不会影响项目启动；仅在你接入微信登录功能时才需要配置。
> - `JWT_SECRET` 未配置会使用开发默认值，仅适用于本地开发，生产环境务必显式配置。

3. 启动服务
```bash
mvn spring-boot:run
```

4. 验证
- 服务默认端口：`8080`
- 文档地址：`http://localhost:8080/doc.html`

## 鉴权说明
### JWT 鉴权（用户接口）
请求头支持以下形式：
- `Authorization: Bearer <token>`（推荐）
- `Authorization: <token>`（兼容）
- `token: <token>`（兼容旧客户端）

### 管理员鉴权（管理员接口 + 内部爬虫接口）
- 请求头：`Authorization: Bearer <token>`
- 角色要求：`role=1`（系统管理员）
- 作用路径：
  - `/api/v1/admin/**`
  - `/api/v1/internal/crawler/**`

## 主要接口
### 公开接口
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/jobs`
- `GET /api/v1/jobs/{id}`

### 需登录接口
- `GET /api/v1/user/me`
- `PUT /api/v1/user/preferences`
- `POST /api/v1/user/membership/upgrade`
- `POST /api/v1/user/jobs/{job_id}/status`
- `GET /api/v1/user/my-jobs`
- `GET /api/v1/memberships`

### 内部接口
- `POST /api/v1/internal/crawler/sync`

### 管理员接口（节选）
- `GET /api/v1/admin/jobs/pending-audit`
- `POST /api/v1/admin/jobs/audit`
- `POST /api/v1/admin/jobs/import-excel`
- `GET /api/v1/admin/crawler/sync-logs`
- `GET /api/v1/admin/crawler/error-items`
- `GET /api/v1/admin/users`
- `PUT /api/v1/admin/users/{id}/status`
- `GET /api/v1/admin/moderation/sensitive-words`
- `GET /api/v1/admin/configs`

## 响应示例
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 相关文档
- 接口文档：`OfferNow平台 API 接口文档.md`
- 版本更新：`VERSION_UPDATE_2026-02-28.md`
- 数据库脚本：`offernow.sql`

## 注意事项
- 当前 `application.yml` 含示例/开发配置，部署前请替换为真实安全配置。
- 建议通过环境变量或配置中心管理密钥与数据库凭据。
- 如遇中文注释乱码，请确认终端/IDE 文件编码为 UTF-8。
