# 二手家电交易系统

## 项目结构
- `backend`: Spring Boot 3 + Security + MyBatis-Plus + Flyway + Redis + WebSocket
- `frontend`: Vue3 + Vite + Pinia + Element Plus
- `docs`: 交付文档（PRD 对齐矩阵、差距清单、数据存储矩阵、毕设规则检查清单）

## 快速启动
### 1) 基础依赖
- JDK 17
- Maven 3.9+
- Node.js 20+
- MySQL 8
- Redis 7

### 2) 后端
```bash
cd backend
mvn spring-boot:run
```
- 默认端口：`8080`
- 健康检查：`GET /api/health`
- OpenAPI：`/swagger-ui/index.html`

### 3) 前端
```bash
cd frontend
npm install
npm run dev
```
- 默认端口：`5173`

## 默认账号
- 管理员：`admin@example.com / Admin1234`

## 当前实现说明
- 已覆盖规划中的接口骨架和核心流程（注册登录、用户中心、商品、购物车、订单、支付、物流、聊天、评价、后台统计、推荐）。
- Auth/User/Product/Order/Cart/Chat/Evaluation/Payment/Logistics 已切换到 MyBatis-Plus + MySQL 持久化。
- 支付网关支持按渠道路由（`alipay`/`wechat`/`mock`），物流网关支持按提供商路由（`kd100`/`kdniao`/`mock`）。
- 已接入物流轨迹定时同步任务，Cron 通过 `app.logistics.sync-cron` 配置。

## 配置项（application.yml）
- `app.payment.alipay.mock-base-url`
- `app.payment.wechat.mock-base-url`
- `app.logistics.provider`
- `app.logistics.sync-cron`

## 迁移与初始化
- Flyway 迁移：`backend/src/main/resources/db/migration`
- 幂等 seed：`backend/src/main/resources/db/seed/seed_demo.sql`
