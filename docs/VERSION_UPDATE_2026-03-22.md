# OfferWave 版本更新说明（2026-03-22）

> 历史版本记录。当前配置项与部署契约请以 `VERSION_UPDATE_2026-08-11.md` 和 README 为准；未实现的 `offerwave.crawler.*` 配置已在后续安全整改中删除。

## 版本信息

- 版本标识：项目重命名调整记录
- 发布时间：`2026-03-22`
- 更新类型：命名统一 / 配置前缀调整 / 文档与脚本同步

## 本次更新内容

### 1. 项目名称统一

- 项目名称由 `OfferNow` 统一调整为 `OfferWave`。
- Maven 坐标同步调整：
  - `groupId`：`com.offernow` -> `com.offerwave`
  - `artifactId`：`offernow-backend` -> `offerwave-backend`
- Spring 应用名同步调整为：`offerwave-backend`。

### 2. Java 包名与启动类调整

- 主源码包名由 `com.offernow` 迁移为 `com.offerwave`。
- 测试源码包名由 `com.offernow` 迁移为 `com.offerwave`。
- 启动类由 `OfferNowApplication` 更名为 `OfferWaveApplication`。

### 3. 配置前缀与默认配置调整

- 业务配置前缀由 `offernow.*` 调整为 `offerwave.*`。
- 受影响配置包括：
  - `offerwave.jwt.*`
  - `offerwave.crawler.*`
  - `offerwave.mail.*`
  - `offerwave.storage.*`
- MyBatis 实体包配置同步调整为：`com.offerwave.entity`。
- 默认数据库名由 `offer_now` 调整为 `offerwave`。

### 4. 环境变量与本地运行配置同步

- 存储相关环境变量前缀由 `OFFERNOW_*` 调整为 `OFFERWAVE_*`：
  - `OFFERWAVE_STORAGE_ROOT`
  - `OFFERWAVE_STORAGE_PUBLIC_URL_PREFIX`
  - `OFFERWAVE_MATERIAL_IMAGE_MAX_SIZE`
- 本地 VS Code 启动配置中的项目名、主类名、默认数据库连接名已同步更新。

### 5. 文档与脚本同步调整

- 接口文档标题由 `OfferNow平台 API 接口文档` 调整为 `OfferWave平台 API 接口文档`。
- 数据库脚本文件名由 `offernow.sql` 调整为 `offerwave.sql`。
- 会话脚本文件名由 `offernow.session.sql` 调整为 `offerwave.session.sql`。
- README、版本说明、OpenAPI 标题、邮件主题中的旧项目名已同步替换。

## 兼容性说明

- 如果外部部署仍使用旧配置前缀 `offernow.*`，需要同步改为 `offerwave.*`。
- 如果运行环境仍使用旧存储环境变量 `OFFERNOW_*`，需要同步改为 `OFFERWAVE_*`。
- 如果数据库实际名称仍为 `offer_now`，需要更新为 `offerwave`，或通过 `DB_URL` 显式覆盖。
- 如果 IDE、脚本、CI/CD 或部署平台仍引用旧启动类 `com.offernow.OfferNowApplication`，需要改为 `com.offerwave.OfferWaveApplication`。

## 验证结果

- 已完成全局旧项目名扫描，仓库内未发现 `OfferNow` / `offernow` / `offer_now` 残留引用。
- 已执行编译校验：`mvn -q -DskipTests test-compile`
- 编译通过。
