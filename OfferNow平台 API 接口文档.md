# OfferNow平台 API 接口文档

**文档版本**：V1.1.1

**最后更新**：2026-03-12

**状态**：接口草案

---

## 1. 全局说明 (Global Standards)

### 1.1 基础路径

* **Base URL**: `https://api.offernow.com/api/v1`
* **协议**: HTTPS

### 1.2 鉴权方式 (Authentication)

除“登录注册”和“公开职位模块”接口外，所有接口均需在 HTTP Header 中携带 Token。
* **Header Key**: `Authorization`
* **Header Value**: `Bearer <token_string>`
* **兼容写法**:
  * `Authorization: Bearer <token_string>` (推荐)
  * `Authorization: bearer <token_string>` (Bearer 大小写不敏感)
  * `Authorization: <token_string>` (兼容)
  * `token: <token_string>` (兼容旧客户端)

### 1.3 统一响应格式 (Response Format)

系统采用标准 JSON 格式响应，包含状态码、消息提示和业务数据。

#### 成功响应示例 (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "nickname": "用户名称"
  }
}
```

#### 失败响应示例 (HTTP 4xx/5xx)

```json
{
  "code": 401,
  "message": "Token已过期，请重新登录",
  "data": null
}
```

#### 1.4 常用业务状态码

| 状态码 (code) | 说明                                            |
| ------------- | ----------------------------------------------- |
| 200           | 请求成功                                        |
| 400           | 参数错误 (Bad Request)                          |
| 401           | 未授权 (Unauthorized)                           |
| 403           | 权限不足 (Forbidden) - 如非会员尝试使用会员功能 |
| 404           | 资源不存在 (Not Found)                          |
| 500           | 服务器内部错误 (Internal Server Error)          |

## 2. 接口目录 (Table of Contents)

* **3. 认证模块 (Auth)**

  * 3.1 [POST] 用户注册 `/auth/register`
  * 3.2 [POST] 用户名密码登录 `/auth/login`

* 4. **用户与会员模块 (User & Membership)**

  * 4.1 [GET] 获取个人信息与权益 `/user/me`
  * 4.2 [PUT] 更新求职偏好 `/user/preferences`
  * 4.3 [GET] 获取会员等级列表 `/memberships`
  * 4.4 [POST] 模拟购买会员 `/user/membership/upgrade`

* 5. **职位查询模块 (Jobs - Public)**

  * 5.1 [GET] 获取职位列表 (搜索/筛选) `/jobs`
  * 5.2 [GET] 获取公开职位总数 `/jobs/total`
  * 5.3 [GET] 获取职位详情 `/jobs/{id}`

* 6. **投递追踪模块 (Tracking)**

  * 6.1 [POST] 更新职位状态 (收藏/投递) `/user/jobs/{job_id}/status`
  * 6.2 [GET] 获取我的职位列表 (收藏/已投) `/user/my-jobs`

* 7. **爬虫数据接入模块 (Crawler - Internal)**

  * 7.1 [POST] 批量上传招聘数据 `/internal/crawler/sync`

## 3. 认证模块 (Authentication)

本模块处理用户的注册和登录流程

### 3.1 用户注册

- **URL**: `/auth/register`
- **Method**: `POST`
- **描述**: 创建一个新用户账号
- **权限**: 公开 (Public)

#### 请求参数 (Request Body)

| **参数名**   | **类型** | **必填** | **说明**                                        |
| ------------ | -------- | -------- | ----------------------------------------------- |
| **username** | string   | **是** | 登录账号 (4-50位，可以是手机号/邮箱/自定义字符) |
| **password** | string   | **是** | 登录密码 (至少6位)                              |

#### 成功响应 (HTTP 200)

```JSON
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

#### 失败响应

- **400 Bad Request**: 
  - `{"code": 400, "message": "注册失败: 用户名已存在"}`
  - `{"code": 400, "message": "用户名长度必须在40个字符之}`
  - `{"code": 400, "message": "密码长度至少}`

---

### 3.2 用户名密码登录

- **URL**: `/auth/login`
- **Method**: `POST`
- **描述**: 用户通过账号和密码登录，换取用于后续请求的 JWT Token。
- **权限**: 公开 (Public)

#### 请求参数 (Request Body)

| **参数名**   | **类型** | **必填** | **说明** |
| ------------ | -------- | -------- | -------- |
| **username** | string   | **是** | 登录账号 |
| **password** | string   | **是** | 登录密码 |

#### 成功响应 (HTTP 200)

```JSON
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", // 核心鉴权 Token
    "user": {
      "id": 1001,
      "nickname": "User_1a2b3c", 
      "avatar": null, 
      "membership_level": 1,
      "is_vip": false
    },
    "is_new_user": false 
  }
}
```

#### 失败响应

- **401 Unauthorized**: `{"code": 401, "message": "登录失败: 用户名或密码错误"}`

---

## 4. 用户与会员模块 (User & Membership)

此模块管理用户画像、偏好设置及会员权益

### 4.1 获取个人信息 (含权

* **URL:** `/user/me`
* **Method:** `GET`

* **描述:** 获取当前登录用户的详细信息，包括求职偏好、会员权益详情以及统计数据

* **权限:** 需登录

**请求参数**



**成功响应 (HTTP 200)**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "nickname": "张三",
    "avatar": "[https://example.com/avatar.jpg](https://example.com/avatar.jpg)",
    "preferences": {
      "industry": ["互联, "金融"], // 解析pref_industry (JSON/CSV)
      "city": "北京",
      "job": "后端开
    },
    "membership": {
      "id": 1,
      "level_name": "普通用,
      "expire_date": null, // null 表示永久有效
      "privileges": {      // 解析memberships.privileges
        "max_track_count": 10,
        "can_view_analysis": false,
        "refresh_limit": 5
      }
    },
    "stats": {
      "tracked_count": 8,   // 当前已追收藏的职位数 (用于前端判断是否超限)
      "delivered_count": 2  // 已投递数
    }
  }
}
```

### 4.2 更新求职偏好

* **URL:** `/user/preferences`

* **Method:** `PUT`

* **描述:** 更新用户的求职意向，用于首页推荐算法

* **权限:** 需登录

**请求参数 (Request Body)**

| 参数       | 类型   | 必填 | 说明                          |
| :------------ | :----- | :--- | :---------------------------- |
| pref_industry | string |   | "偏好行业 ("互联教育")" |
| pref_city     | string |   | "偏好城市 ("上海")"        |
| pref_job      | string |   | "偏好岗位 ("Java开)"    |

**成功响应 (HTTP 200)**

```JSON
{
  "code": 200,
  "message": "偏好设置已更,
  "data": true
}
```

### 4.3 获取会员等级列表

* **URL:** `/memberships`

* **Method:** `GET`

* **描述:** 获取所有可用的会员等级及其价格、权益，用于“会员购买”页面展示
* **权限:** 需登录

**请求参数**


**成功响应 (HTTP 200)**

```JSON
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "level_name": "普通用,
      "price": 0.00,
      "desc": "基础功能，限制追10 个职
    },
    {
      "id": 2,
      "level_name": "VIP会员",
      "price": 9.90,
      "desc": "无限追踪，解锁竞争力分析报告",
      "tag": "推荐"
    }
  ]
}
```

### 4.4 模拟购买/升级会员 (MVP)

* **URL:** `/user/membership/upgrade`

* **Method:** `POST`

* **描述:** 开发测试专用接口。将当前用户直接升级为指定等级

* **权限:** 需登录

**请求参数 (Request Body)**

| 参数                 | 类型 | 必填 | 说明                        |
| :---------------------- | :--- | :--- | :-------------------------- |
| target_level_id         | int  |   | 目标等级 ID (2 代表 VIP) |

**成功响应 (HTTP 200)**

```JSON
{
  "code": 200,
  "message": "升级成功",
  "data": {
    "current_level": "VIP会员",
    "expire_date": "2026-03-20"
  }
}
```

## 5. 职位查询模块 (Jobs - Public)


此模块提供职位的检索、筛选和详情展示
**注意**：此模块接口**无需鉴权**即可访问，但如果 Request Header 中带有 Token，后端会在返回详情时附加“我的投递状态”。

### 5.1 获取职位列表 (搜索/筛选)

* **URL**: `/jobs`
* **Method**: `GET`
* **描述**: 首页表格视图的数据源。支持多维度组合筛选和模糊搜索
* **权限**: 公开 (Public)

#### 请求参数 (Query Params)

| 参数          | 类型   | 必填 | 说明                                                |
| :--------------- | :----- | :--- | :-------------------------------------------------- |
| **page**         | int    | 否 | 页码 (默认 1)                                      |
| **size**         | int    | 否 | 每页数量 (默认 20)                                 |
| **keyword**      | string | 否 | 搜索关键词 (匹配公司名或岗位)                      |
| **city**         | string | 否 | 城市筛选 (如"北京")                             |
| **industry**     | string | 否 | 行业筛选 (如"互联网")                           |
| **recruit_type** | string | 否 | 招聘类型 (春招/秋招/实习)                          |
| **salary_min**   | int    | 否 | 最低薪资过滤 (如 15000)                            |
| **education**    | string | 否 | 学历过滤 (如 本科)                                 |
| **sort**         | string | 否 | 排序字段: `newest`(默认), `deadline`, `salary_desc` |

#### 成功响应 (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 150, // 总记录数
    "current": 1, // 当前页
    "size": 20,
    "records": [
      {
        "id": 2048,
        "company_name": "字节跳动",
        "company_business": "互联网",
        "industry": "互联网",
        "job_title": "后端开发工程师",
        "city": "北京",
        "recruit_type": "春招",
        "education": "本科",
        "salary_range": "20k-40k", // 展示用文本
        "deadline": "2026-03-31",  // 前端需比较此时间：< 7 天则显示红色
        "process_stage": "网申开启",
        "updated_at": "2026-02-18 10:00:00",
        "is_urgent": true          // 后端计算字段: true 表示快截止
      },
      {
        "id": 2049,
        "company_name": "腾讯",
        // ...
      }
    ]
  }
}
```

### 5.2 获取公开职位总数

- **URL**: `/jobs/total`
- **Method**: `GET`
- **描述**: 获取当前已审核上线职位总数，游客可访问。
- **权限**: 公开 (Public)

#### 成功响应 (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total_count": 1234
  }
}
```

### 5.3 获取职位详情

- **URL**: `/jobs/{id}`
- **Method**: `GET`
- **描述**: 获取单条职位的完整信息。如果用户已登录，会同时返回用户对该职位的收藏/投递状态
- **权限**: 公开 (Public)

#### 请求参数

| 参数| 位置 | 类型 | 必填 | 说明 |
| :-- | :-- | :-- | :-- | :-- |
| `id` | Path | long | | 职位 ID |
| `Authorization` | Header | string | | 可选。传`Bearer <token>` 后，响应中会返回 `my_status`（包含收藏状态与投递状态）；未传时 `my_status` `null` |

#### 返回参数说明（data
| 参数| 类型 | 说明 |
| :-- | :-- | :-- |
| `id` | long | 职位 ID |
| `company_name` | string | 公司名称 |
| `job_title` | string | 岗位名称 |
| `announcement` | string | 职位描述/公告信息 |
| `apply_link` | string | 投递链|
| `test_info` | string | 笔试或测评说|
| `deadline` | string | 截止日期（`YYYY-MM-DD`|
| `my_status` | object/null | 当前用户对该职位的个性化状态；未登录时为 `null` |
| `my_status.is_collected` | boolean | 用户对该职位的收藏状态（`true`=已收藏，`false`=未收藏） |
| `my_status.delivery_status` | int | 投递状态码（见“投递状态枚举”） |
| `my_status.delivery_status_str` | string | 投递状态文|
| `my_status.user_note` | string | 用户备注 |

#### 成功响应 (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2048,
    "company_name": "字节跳动",
    "job_title": "后端开发工程师",
    "announcement": "职位描述...", 
    "apply_link": "[https://job.bytedance.com/](https://job.bytedance.com/)...", // 投递跳转链
    "test_info": "需进行在线测评",
    "deadline": "2026-03-31",
    
    // 用户个性化状态 (未登录时 null)
    "my_status": {
      "is_collected": true,
      "delivery_status": 1, // 1:已投
      "delivery_status_str": "已投,
      "user_note": "面试定在周三"
    }
  }
}
```

## 6. 投递追踪模块 (Tracking)

此模块是用户个人中心的核心，用于管理求职进度。**权限**: 需登录 (Bearer Token)。

### 6.1 更新职位状态 (收藏/投递)

- **URL**: `/user/jobs/{job_id}/status`
- **Method**: `POST`
- **描述**: 用户手动标记某个职位的状态
- **逻辑约束**:
  1. 检查用户会员等级
  2. 如果普通用户中 `is_collected=true` 或 `delivery_status > 0` 的总数已达到权益上限（10），则拒绝操作。

#### 请求参数 (Request Body)

| **参数名**          | **类型** | **必填** | **说明**                |
| ------------------- | -------- | -------- | ----------------------- |
| **is_collected**    | boolean  | 否      | 是否收藏                |
| **delivery_status** | int      | 否      | 投递状态码 (见底部枚举) |
| **user_note**       | string   | 否      | 用户备注 (最多100字)      |

#### 成功响应 (HTTP 200)

```json
{
  "code": 200,
  "message": "状态已更新",
  "data": null
}
```

#### 失败响应 (HTTP 403 - 权益不足)

```json
{
  "code": 403,
  "message": "普通用户只能追10 个职位，请升VIP 解锁无限权益",
  "data": {
    "current_count": 10,
    "limit": 10,
    "upgrade_url": "/memberships" // 前端引导跳转
  }
}
```

### 6.2 获取我的职位列表

- **URL**: `/user/my-jobs`
- **Method**: `GET`
- **描述**: 获取用户“收藏夹”或“投递进度表”的数据。
- **权限**: 需登录

#### 请求参数 (Query Params)

| **参数名** | **类型** | **必填** | **说明**                                     |
| ---------- | -------- | -------- | -------------------------------------------- |
| **type**   | string   | 是      | `collected` (仅收藏) 或 `delivered` (已投递) |
| **page**   | int      | 否      | 页码                                         |
| **size**   | int      | 否      | 每页数量 (默认 20)                           |



------

### 附录：状态码枚举 (Enumerations)

前端渲染时请参考以下映射：

#### 投递状态 (delivery_status)

| **值** | **含义**           | **建议颜色 (Element Plus)** |
| ------ | ------------------ | --------------------------- |
| **0**  | **未投递**         | Info (灰色)                 |
| **1**  | **已投递**         | Primary (蓝色)              |
| **2**  | **笔试中**         | Purple (紫色)               |
| **3**  | **面试中**         | Warning (橙色)              |
| **4**  | **已录用 (Offer)** | Success (绿色)            |
| **5**  | **流程结束 (挂)**  | Danger (红色)             |



## 7. 爬虫数据接入模块 (Crawler - Internal)

此模块专供内部 Python 爬虫程序调用，用于批量上报抓取到的招聘数据。
**鉴权**: 使用专用 API Key（在 Header 中传递，不使用用户 Token）。

### 7.1 批量上报招聘数据

* **URL**: `/internal/crawler/sync`
* **Method**: `POST`
* **描述**: 接收爬虫清洗后的结构化数据列表。后端根据 `unique_hash` 进行去重：若存在则更新 `updated_at` 和 `process_stage`，若不存在则插入新记录。
* **Header**:
  * `X-API-KEY`: `<your_secret_crawler_key>` (需在后端配置匹配)

#### 请求参数 (Request Body)

| 参数                | 类型   | 必填   | 说明                                       |
| :--------------------- | :----- | :----- | :----------------------------------------- |
| **batch_id**           | string | 是   | 批次号 (如 "crawl_20260218_01")            |
| **items**              | array  | 是   | 职位数据列表 (建议单次不超过 100 条)        |
| **unique_hash**      | string | **是** | **核心去重字段** (MD5: 公司岗位城市) |
| **company_name**     | string | 是   | 公司名称                                   |
| **company_type**     | string | 否   | 公司类型 (国企/外企/民企/上市公司)         |
| **company_business** | string | 否   | 所属行业                                  |
| **job_title**        | string | 是   | 岗位名称                                   |
| **city**             | string | 是   | 工作地点                                   |
| **recruit_type**     | string | 是   | 招聘类型 (春招/秋招/实习)                  |
| **education**        | string | 否   | 学历要求                                   |
| **salary_range**     | string | 否   | 薪资范围文本 ("15k-25k")                |
| **salary_min**       | int    | 否   | 最低薪资 (数值，用于筛选)                  |
| **salary_max**       | int    | 否   | 最高薪资 (数值)                            |
| **apply_link**       | string | 是   | 投递链接                                  |
| **deadline**         | string | 否   | 截止日期 (YYYY-MM-DD 或 "2026-03-31")      |
| **process_stage**    | string | 否   | 招聘进度 (如 "网申开启", "笔试中")         |
| **test_info**        | string | 否   | 笔试信息 (如 "需在线测评")                 |
| **source_origin**    | string | 否   | 来源 (默认 "crawler")                      |

#### 成功响应 (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "received_count": 50,
    "inserted_count": 10, // 新增职位
    "updated_count": 40   // 更新职位数 (hash已存在)
  }
}
```

失败响应 (HTTP 401/400)


- **401 Unauthorized**: `{"code": 401, "message": "无效API Key"}`
- **400 Bad Request**: `{"code": 400, "message": "items 列表不能为空"}`

------

### 附录：爬虫端去重逻辑 (Python 示例)

建议爬虫在生成数据时，按如下逻辑计算 `unique_hash`

```python
import hashlib

def generate_hash(company, title, city):
    # 1. 拼接关键字段 (中间加分隔符防止粘连)
    raw_str = f"{company}|{title}|{city}"
    
    # 2. 计算 MD5
    m = hashlib.md5()
    m.update(raw_str.encode('utf-8'))
    
    # 3. 返回 32 位小写 hex 字符串
    return m.hexdigest()

# 示例
# generate_hash("字节跳动", "后端开发", "北京")
# -> "7a9d2f6b1c3e4a5d8f9e0b1c2d3e4f5a"
```

------

## 8. 数据库枚举参考 (Database Enums)

后端存入数据库时，请参考以下状态码定义

### 8.1 审核状态 (audit_status)

| **值** | **含义**                     |
| ------ | ---------------------------- |
| **0**  | **待审核** (默认状态        |
| **1**  | **已上线** (公开可见)        |
| **2**  | **审核拒绝** (内容违规/重复) |

### 8.2 会员等级 (membership_id)

| **值** | **含义**            |
| ------ | ------------------- |
| **1**  | **普通用户** (免费) |
| **2**  | **VIP会员** (付费)  |
## 9. v1.1.0 更新说明026-02-28

### 9.1 鉴权模型调整
- `/api/v1/internal/crawler/sync` 已改为管理员 JWT 鉴权（`Authorization: Bearer <token>`）
- 角色要求：`role=1`（系统管理员）
- 不再使用 `X-API-KEY` 作为爬虫接口鉴权方式

### 9.2 新增管理员模
- 新增路由前缀：`/api/v1/admin/**`
- 覆盖能力
  - 招聘信息管理（待审列表、审核、职位全量管理、Excel 导入、批量进度更新、过期清理）
  - 爬虫监控（同步日志、异常拦截列表）
  - 用户与权益管理（用户列表、封禁/解封、手工权益发放）
  - 内容审核（敏感词管理、审核日志）
  - 系统配置（会员配置管理、运营配置管理）

### 9.3 爬虫同步响应变更
- `/internal/crawler/sync` 成功返回新增字段：`failed_count`

### 9.4 登录与个人信息返回补充字段
- 登录返回 `user` 中新增：`role`、`is_admin`
- `/user/me` 返回中新增：`role`、`is_admin`

## 10. v1.1.1 更新说明（2026-03-01）

### 10.1 认证新增：邮箱验证码登录与忘记密码

新增 3 个接口（均为公开接口，无需 JWT）：

- `POST /auth/send-email-code`
- `POST /auth/login/email`
- `POST /auth/password/reset`

### 10.2 [POST] 发送邮箱验证码 `/auth/send-email-code`

- **用途**：发送登录验证码或重置密码验证码
- **限流**：同邮箱/IP 1 分钟 1 次，24 小时最多 10 次

#### Request Body

| 参数| 类型 | 必填 | 说明 |
| :-- | :-- | :-- | :-- |
| `email` | string | 是 | 邮箱地址 |
| `type` | string | 是 | 验证码类型：`login`（登录）或 `reset_pwd`（重置密码） |

#### Success Response (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": "验证码发送成功"
}
```

#### Error Response

- `429`：触发限流
- `400`：邮箱格式错误、类型错误、`reset_pwd` 下邮箱未注册

### 10.3 [POST] 邮箱验证码登录 `/auth/login/email`

- **用途**：邮箱 + 验证码登录
- **策略**：若邮箱未注册，自动创建账号并登录（`is_new_user=true`）

#### Request Body

| 参数| 类型 | 必填 | 说明 |
| :-- | :-- | :-- | :-- |
| `email` | string | 是 | 邮箱地址 |
| `code` | string | | 邮箱验证|
| `password` | string | 条件必填 | 首次邮箱验证码登录（自动注册）必填，至少 6 位；已注册用户可不传 |

#### Success Response (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "<jwt_token>",
    "user": {
      "id": 1001,
      "nickname": "User_xxxxxx",
      "avatar": null,
      "role": 0,
      "is_admin": false,
      "membership_level": 1,
      "is_vip": false,
      "email": "user@example.com"
    },
    "is_new_user": true
  }
}
```

#### Error Response

- `401`：验证码错误/过期，或账号状态异
- `401`：首次邮箱登录未传密码，或密码长度不足 6 位

### 10.4 [POST] 重置密码 `/auth/password/reset`

- **用途**：忘记密码场景下重置密码
- **安全**：验证码校验通过后，使用 BCrypt 更新 `password_hash`，并销毁验证码

#### Request Body

| 参数| 类型 | 必填 | 说明 |
| :-- | :-- | :-- | :-- |
| `email` | string | 是 | 邮箱地址 |
| `code` | string | | 邮箱验证|
| `newPassword` | string | | 新密码，至少 6 |

#### Success Response (HTTP 200)

```json
{
  "code": 200,
  "message": "success",
  "data": "密码重置成功"
}
```

#### Error Response

- `400`：验证码错误/过期、邮箱未注册、参数不合法

### 10.5 数据与配置变

- `users` 新增 `email` 字段，并加唯一索引 `uk_email`
- Redis 验证Key
  - `login_code:{email}`
  - `reset_pwd_code:{email}`
- 验证TTL 分钟
- 新增邮件配置（QQ SMTP）：
  - `spring.mail.host=smtp.qq.com`
  - `spring.mail.port=465`
  - `spring.mail.username=asazhou@qq.com`
  - `spring.mail.password=${MAIL_PASSWORD}`





