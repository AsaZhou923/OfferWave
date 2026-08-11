# OfferWave 平台 API 接口文档

- 文档版本：V1.2.0
- 最后更新：2026-08-11
- 状态：与当前实现同步
- Base URL：`https://api.offerwave.com/api/v1`

运行时字段模型以 `/v3/api-docs` 和 `/doc.html` 为准；本文固定跨模块的鉴权、状态码、分页、职位身份和爬虫契约。

## 1. 全局契约

### 1.1 鉴权

需要登录的请求只接受：

```http
Authorization: Bearer <jwt>
```

Bearer scheme 大小写不敏感，但 Header 中必须同时包含 scheme 和 Token。以下旧方式已经删除并按未认证处理：

- `?token=...`
- `Cookie: token=...`
- `token: ...`
- `X-ACCESS-TOKEN: ...`
- `Authorization: <裸 token>`
- 爬虫 `X-API-KEY`

权限分层：

| 范围 | 权限 |
|---|---|
| `/auth/**`、`/jobs/**`、公开资料与会员列表 | 公开 |
| `/user/**`、用户资料下载 | 登录用户 |
| `/admin/**`、`/internal/crawler/**` | `role=1` 管理员 |

密码修改后，原 JWT 因凭据版本不再匹配而失效。

### 1.2 响应与 HTTP 状态

统一响应体：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "map": {}
}
```

`code` 与真实 HTTP 状态保持一致。常见状态为 `200`、`400`、`401`、`403`、`404`、`429`、`500`。服务端异常只返回固定的 `服务器内部错误`，不会把数据库、Redis、SMTP、JWT 或堆栈文本回显给客户端。

### 1.3 分页

所有 `/api/**` 的分页参数统一约束：

- `page >= 1`
- `1 <= size <= 100`
- 不合法时返回 HTTP `400`

### 1.4 跨域

浏览器 Origin 必须命中部署变量 `OFFERWAVE_CORS_ALLOWED_ORIGINS` 中的精确白名单。服务不允许通配 Origin，也不开放凭据化 CORS。

## 2. 接口目录

### 2.1 公开接口

| Method | Path | 用途 |
|---|---|---|
| `POST` | `/auth/send-email-code` | 发送注册、登录或重置密码验证码 |
| `POST` | `/auth/register` | 邮箱 + 验证码 + 密码注册 |
| `POST` | `/auth/login` | 用户名/邮箱 + 密码登录 |
| `POST` | `/auth/login/email` | 已注册邮箱 + 验证码登录 |
| `POST` | `/auth/password/reset` | 邮箱验证码重置密码 |
| `GET` | `/jobs` | 职位搜索、筛选和排序 |
| `GET` | `/jobs/total` | 已上线职位总数 |
| `GET` | `/jobs/{id}` | 职位详情；登录时附带个人状态 |
| `GET` | `/memberships` | 会员等级列表 |
| `GET` | `/material-categories/sections` | 资料分类与内容 |
| `GET` | `/material-packages` | 资料包列表 |
| `GET` | `/material-packages/{id}` | 资料包详情 |

### 2.2 登录用户接口

| Method | Path | 用途 |
|---|---|---|
| `GET` | `/user/me` | 当前用户、偏好、会员与统计 |
| `PUT` | `/user/preferences` | 更新求职偏好 |
| `POST` | `/user/jobs/{job_id}/status` | 收藏、投递状态和备注 |
| `GET` | `/user/my-jobs` | 我的收藏/投递列表 |
| `GET` | `/user/material-packages/{id}/downloads` | 获取有权限的下载项 |

普通用户自助升级接口 `/user/membership/upgrade` 已从生产代码删除；调用返回 `404`。会员调整只能由管理员接口或未来经过验证的支付流程执行。

### 2.3 管理员接口

以下接口均要求管理员 JWT：

| Method | Path | 用途 |
|---|---|---|
| `GET` | `/admin/jobs/pending-audit` | 待审核职位 |
| `POST` | `/admin/jobs/audit` | 批量审核职位 |
| `DELETE` | `/admin/jobs` | 批量删除职位 |
| `GET` | `/admin/jobs` | 全量职位库 |
| `POST` | `/admin/jobs` | 新建职位 |
| `POST` | `/admin/jobs/batch` | 批量新建职位 |
| `POST` | `/admin/jobs/import-file` | 导入 Excel/CSV |
| `PUT` | `/admin/jobs/{id}` | 更新职位 |
| `POST` | `/admin/jobs/company-stage` | 批量更新公司招聘阶段 |
| `POST` | `/admin/jobs/cleanup-expired` | 清理过期职位 |
| `GET` | `/admin/crawler/sync-logs` | 爬虫同步日志 |
| `GET` | `/admin/crawler/error-items` | 爬虫异常条目 |
| `GET` | `/admin/users` | 用户列表；不返回任何密码哈希 |
| `PUT` | `/admin/users/{id}/status` | 封禁/解封用户 |
| `PUT` | `/admin/users/{id}/benefits` | 调整会员与追踪额度 |
| `GET` | `/admin/moderation/sensitive-words` | 敏感词列表 |
| `POST` | `/admin/moderation/sensitive-words` | 新增敏感词 |
| `PUT` | `/admin/moderation/sensitive-words/{id}/status` | 启停敏感词 |
| `DELETE` | `/admin/moderation/sensitive-words/{id}` | 删除敏感词 |
| `GET` | `/admin/moderation/audit-logs` | 内容审核日志 |
| `GET` | `/admin/memberships` | 会员配置 |
| `PUT` | `/admin/memberships/{id}` | 更新会员配置 |
| `GET` | `/admin/configs` | 运营配置 |
| `POST` | `/admin/configs` | 新增/更新运营配置 |
| `GET` | `/admin/material-categories` | 资料分类管理 |
| `POST` | `/admin/material-categories` | 新建资料分类 |
| `PUT` | `/admin/material-categories/{id}` | 更新资料分类 |
| `GET` | `/admin/material-packages` | 资料包管理列表 |
| `GET` | `/admin/material-packages/{id}` | 资料包管理详情 |
| `POST` | `/admin/material-packages` | 新建资料包 |
| `PUT` | `/admin/material-packages/{id}` | 更新资料包 |
| `POST` | `/admin/material-packages/images` | 上传资料图片 |

## 3. 认证接口

### 3.1 发送验证码

`POST /auth/send-email-code`

```json
{
  "email": "user@example.com",
  "type": "register"
}
```

`type` 仅允许 `register`、`login`、`reset_pwd`。无论邮箱是否存在，成功受理时均返回相同的外部消息，避免账号枚举。验证码有效期 5 分钟；同邮箱和来源 IP 每种用途 1 分钟最多发送一次、24 小时最多 10 次。

### 3.2 注册

`POST /auth/register`

```json
{
  "email": "user@example.com",
  "code": "123456",
  "password": "at-least-6-characters"
}
```

验证码必须来自 `register` 场景。注册成功返回 HTTP `200`；重复或校验失败返回固定的 HTTP `400` 消息。

### 3.3 密码登录

`POST /auth/login`

```json
{
  "username": "username-or-email",
  "password": "secret"
}
```

成功响应的 `data`：

```json
{
  "token": "<jwt>",
  "user": {
    "id": 1001,
    "nickname": "User_xxxxxx",
    "role": 0,
    "is_admin": false,
    "membership_level": 1,
    "is_vip": false,
    "email": "user@example.com"
  },
  "is_new_user": false
}
```

未知账号与错误密码都返回相同的 HTTP `401` 消息。

### 3.4 邮箱验证码登录

`POST /auth/login/email`

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

仅支持已经注册且未被封禁的邮箱，不会自动创建账号。

### 3.5 重置密码

`POST /auth/password/reset`

```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "new-secret"
}
```

验证码最多允许 5 次错误尝试，并通过 Redis 脚本原子校验、计数与单次消费。密码更新后，旧 JWT 自动失效。

## 4. 职位查询与追踪

### 4.1 职位列表

`GET /jobs`

| Query | 类型 | 说明 |
|---|---|---|
| `page` | int | 默认 1 |
| `size` | int | 默认 20，最大 100 |
| `keyword` | string | 公司名或职位名模糊匹配 |
| `city` | string | 城市模糊匹配 |
| `industry` | string | 行业模糊匹配 |
| `recruit_type` | string | 招聘类型精确匹配 |
| `salary_min` | int | 最低薪资下限 |
| `education` | string | 学历要求 |
| `sort` | string | `newest`（默认）、`deadline`、`salary_desc` |

排序先按用户选择生成候选窗口，再在前 30 条内按公司轮询提高多样性。游客和普通会员最多查看该排序结果的前 30 条；VIP 可以继续分页查看全部结果。不会再按最小 ID 返回最旧数据。

### 4.2 更新追踪状态

`POST /user/jobs/{job_id}/status`

```json
{
  "isCollected": true,
  "deliveryStatus": 1,
  "userNote": "已投递，等待通知"
}
```

- `deliveryStatus`：`0` 未投递、`1` 已投递、`2` 笔试中、`3` 面试中、`4` 已录用、`5` 流程结束；
- `userNote` 最多 200 字；
- 普通用户额度检查在事务中锁定用户记录；会员配置缺失时 fail closed。

### 4.3 我的职位

`GET /user/my-jobs?type=collected&page=1&size=20`

`type` 为 `collected` 或 `delivered`。

## 5. 内部爬虫同步

`POST /internal/crawler/sync`

只允许管理员 JWT，不接受 API Key。

```json
{
  "batchId": "crawl_20260811_01",
  "items": [
    {
      "companyName": "Acme",
      "companyType": "民企",
      "companyBusiness": "互联网",
      "jobTitle": "Java Developer",
      "city": "Tokyo",
      "recruitType": "社招",
      "education": "本科",
      "salaryRange": "20k-30k",
      "salaryMin": 20000,
      "salaryMax": 30000,
      "applyLink": "https://example.com/jobs/1",
      "deadline": "2026-09-30",
      "processStage": "网申开启",
      "sourceOrigin": "crawler"
    }
  ]
}
```

单条必填字段为 `companyName`、`companyType`、`jobTitle`、`city`、`recruitType`；`batchId` 必填且不超过 100 字符。`uniqueHash` 是兼容性字段，只作为提示，服务端始终重新计算并拥有最终职位身份。

服务端规范算法：

```text
MD5(UTF-8(lower(trim(companyName)) + "|" + lower(trim(jobTitle)) + "|" + lower(trim(city))))
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "received_count": 1,
    "inserted_count": 1,
    "updated_count": 0,
    "failed_count": 0
  },
  "map": {}
}
```

失败同步与非法条目的审计使用独立事务记录，不会随主写入事务一起回滚。

## 6. 兼容性与破坏性变更

2026-08-11 安全整改包含以下有意的破坏性变更：

- 所有旧 JWT 在部署新 `JWT_SECRET` 后失效；
- 密码重置会撤销旧 JWT；
- 部署前 Redis 中遗留的邮箱验证码不保证继续有效；
- `/user/membership/upgrade` 已删除；
- `X-API-KEY`、查询参数/Cookie/旧 Header Token 已删除；
- 错误响应改用真实 HTTP 4xx/5xx；
- 所有分页 `size` 上限为 100。

客户端必须在发布前完成相应迁移。
