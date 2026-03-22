# OfferWave Backend

OfferWave 是一个面向求职场景的后端服务，聚焦职位信息管理、用户认证、进度追踪和后台运营支持。

## 项目简介

项目主要提供以下能力：

- 用户注册、登录与邮箱验证码验证
- 职位列表、职位详情与求职进度追踪
- 会员能力与个人中心数据管理
- 管理员后台的职位审核、用户管理和内容配置
- 爬虫数据接入与内部同步支持

## 项目结构

```text
.
├─ src/
│  ├─ main/
│  │  ├─ java/com/offerwave/   # 核心业务代码
│  │  └─ resources/            # 配置与资源文件
│  └─ test/java/com/offerwave/ # 测试代码
├─ docs/                       # 项目文档与更新日志
├─ .github/workflows/          # GitHub Actions 工作流
├─ offerwave.sql               # 数据库初始化脚本
└─ README.md
```

`src/main/java/com/offerwave` 下主要按职责划分为：

- `controller`：接口入口
- `service`：业务逻辑
- `mapper`：数据访问
- `entity` / `dto`：数据对象
- `config` / `common` / `util`：公共配置与基础能力

## 快速开始

1. 准备数据库并执行 `offerwave.sql`
2. 按需配置数据库、Redis、JWT、邮件等环境变量
3. 运行 `mvn spring-boot:run`
4. 启动后访问 `/doc.html` 查看接口文档

## 文档

- 接口文档：运行后访问 `/doc.html`
- 静态接口文档：`docs/OfferWave平台 API 接口文档.md`
- 更新日志：`docs/`
- 数据库脚本：`offerwave.sql`
