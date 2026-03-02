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

### 2) 一键预检（推荐）
```bash
bash scripts/preflight.sh
```

Windows PowerShell：
```powershell
powershell -ExecutionPolicy Bypass -File .\\scripts\\preflight.ps1
```

### 3) 一键启动（推荐）
```bash
# 默认 backend=8080, frontend=5173
bash scripts/start-dev.sh
```

端口冲突时可临时改端口（例如 backend 8081）：
```bash
BACKEND_PORT=8081 FRONTEND_PORT=5174 bash scripts/start-dev.sh
```

Windows PowerShell：
```powershell
$env:BACKEND_PORT=\"8080\"; $env:FRONTEND_PORT=\"5173\"; powershell -ExecutionPolicy Bypass -File .\\scripts\\start-dev.ps1
```

### 4) 后端（手动方式）
```bash
cd backend
mvn spring-boot:run
```
- 默认端口：`8080`
- 健康检查：`GET /api/health`
- OpenAPI：`/swagger-ui/index.html`

### 5) 前端（手动方式）
```bash
cd frontend
npm install
npm run dev
```
- 默认端口：`5173`
- 代理默认指向 `http://localhost:8080`，可通过 `VITE_PROXY_TARGET` 临时覆盖

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

## Redis 说明（本地演示）
- 验证码使用 Redis（key: `auth:code:<account>`，TTL=300秒）。
- 若本机未安装 Redis，可用 Docker 启动：
```bash
docker run -d --name used-appliance-redis -p 6379:6379 redis:7-alpine
```

## 图片上传说明
- 后端图片上传接口：`POST /api/files/upload/image`（`multipart/form-data`，字段名 `file`）。
- 统一限制：单图不超过 `5MB`，支持 `jpg/jpeg/png/webp/gif`。
- 本地文件目录：`backend` 启动目录下的 `uploads/`（可通过 `app.file.upload-dir` 调整）。
- 访问路径：`/uploads/{filename}`。
- 业务引用策略：商品图、店铺图、聊天图、评价图、退款凭证图均仅在业务数据中保存 URL，不直接入库二进制。
- 清理建议：当前不启用自动孤儿文件清理；演示环境建议按需手工清理长期未引用文件。
