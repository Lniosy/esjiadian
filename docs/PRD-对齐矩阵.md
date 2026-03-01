# PRD 对齐矩阵（实填，2026-03-01）

## 对齐状态说明
- `已实现`：后端接口 + 前端入口 + 基本测试均存在
- `部分实现`：核心链路可跑通，但缺少 PRD 的部分细节能力
- `未实现`：当前代码中不存在

## 功能对齐矩阵
| PRD功能 | 实现状态 | API | 前端页面/路由 | 测试用例 | 证据 |
|---|---|---|---|---|---|
| 用户注册/登录（密码+验证码、锁定） | 已实现（验证码 Redis+TTL） | `/api/auth/code/send` `/api/auth/register` `/api/auth/login/password` `/api/auth/login/code` `/api/auth/logout` | `/login` | 登录成功/失败5次锁定30分钟；验证码5分钟有效 | `AuthService` + `AuthCodeService` |
| 实名认证与用户中心 | 已实现 | `/api/verification/real-name` `/api/verification/status` `/api/users/me` `/api/users/me/addresses` `/api/admin/verification/{id}/approve|reject` | 登录后主流程可调用（无独立用户中心页） | 认证申请-审核-状态流转；地址增删改查 | `UserController`/`UserService` |
| 商品发布/编辑/上下架/审核 | 已实现（含上架数量约束） | `/api/products` `/api/products/{id}` `/api/products/{id}/on-shelf|off-shelf` `/api/admin/products/reviews` `/api/admin/products/reviews/{id}/approve|reject` | `/products` | 发布待审、审核通过上架、编辑后二次审核、最多50件上架 | `ProductService` |
| 商品搜索与详情 | 部分实现 | `/api/products` `/api/products/{id}` | `/products` | 关键词+筛选查询、排序、详情查看 | 已支持价格区间/交易方式/销量排序；分类导航独立模块未做 |
| 店铺管理与店铺页 | 已实现 | `/api/shops/me` `/api/shops/{id}` `/api/shops/{id}/overview` | `/shop` | 店铺信息维护、评分摘要、在售商品、历史评价 | 店铺页已聚合展示 |
| 推荐（首页+详情相关推荐） | 已实现（规则版） | `/api/recommend/home` `/api/recommend/products/{id}` | `/`（首页调用） | 新用户热门/同城、老用户行为相关推荐 | `RecommendController`/`RecommendService` |
| 购物车管理 | 已实现 | `/api/cart/items` `GET/POST/PUT/DELETE` | `/products` `/orders` | 添加、更新数量/选中、删除、失效提醒 | 购物车返回 `valid/invalidReason`，支付成功自动移除已售商品 |
| 订单创建与状态机 | 已实现 | `/api/orders` `/api/orders/{id}` `/api/orders/{id}/cancel|ship|confirm-receipt` | `/orders` | 状态流转、发货收货 | `OrderService` + 定时任务 |
| 超时取消/自动收货 | 已实现 | 定时任务（无独立 API） | `/orders` | 30 分钟未支付取消；7天自动收货 | `OrderLifecycleJob` |
| 支付（模拟） | 已实现（模拟） | `/api/payments/{orderId}/alipay` `/api/payments/{orderId}/wechat` `/api/payments/callback/{channel}` | `/orders` | 创建支付、回调幂等 | `PaymentService` 模拟网关 |
| 物流轨迹（模拟） | 已实现（模拟） | `/api/orders/{id}/logistics` `/api/orders/{id}/logistics/tracks` | `/orders` | 录入运单、轨迹查询、定时同步 | `LogisticsService` + `LogisticsSyncJob` |
| 聊天与通知中心 | 已实现（增强版） | `/api/chat/conversations` `/api/chat/messages` `/api/chat/messages/{id}/report` `/api/chat/read` `WS /ws/chat` 事件 `chat.send` `chat.read` `chat.receive`；`/api/files/upload/image`；`/api/notifications`；`/api/notifications/sms-logs` | `/chat` `/notifications` | 文本/图片/商品卡片消息、已读、举报、WS实时接收、站内通知、浏览器通知提醒、短信模拟日志 | 浏览器通知采用 Web Notification API（前端授权后轮询触发）；短信为模拟通道（`MOCK_SMS`） |
| 评价与信誉 | 已实现 | `/api/orders/{id}/evaluations` `/api/products/{id}/evaluations` `/api/shops/{id}/rating` `/api/shops/{id}/rating-detail` `/api/admin/stats/recompute-seller-scores` | `/orders` `/shop` `/admin` | 一单一评、15天评价窗、评价图片上传、评分分布、标签云、信誉分复算 | 卖家信誉分支持“小样本平滑”动态策略 + 管理员手动重算 + 定时重算 |
| 退款与纠纷 | 已实现（模拟退款流） | `/api/orders/{id}/refund/apply|approve|reject|ship-return|confirm-return` `/api/disputes` `/api/admin/disputes*` | `/orders` `/disputes` `/admin` | 待发货退款、已发货退货退款、纠纷申诉与裁决 | `RefundService` 已支持 `RETURN_REQUIRED -> BUYER_SHIPPED -> COMPLETED` |
| 管理后台（用户/商品/订单/纠纷/统计） | 已实现（高可用） | `/api/admin/users` `/api/admin/users/{id}/enable|disable` `/api/admin/products/reviews` `/api/admin/orders` `/api/admin/orders/export` `/api/admin/disputes` `/api/admin/stats/overview` `/api/admin/stats/trends` `/api/admin/stats/trends/export` `/api/admin/monitor` `/api/admin/monitor/alerts` `/api/admin/monitor/alerts/{id}/ack` `/api/admin/monitor/mysql/slow-queries` `/api/admin/notifications/sms-logs` `/api/admin/chat-reports` `/api/admin/chat-reports/{id}/resolve` `/api/admin/operation-logs` | `/admin` | 管理员审核、查询、裁决、用户启停、订单导出、趋势统计导出、监控阈值告警与确认、短信日志审计、违规举报处理、关键操作审计日志、接口调用/平均响应/慢请求统计、MySQL慢查询计数与明细Top SQL、Redis命中率统计 | 若数据库未启用 `performance_schema`，慢查询明细接口会降级为空列表 |

## 本轮新增补齐（相对上轮）
1. 新增 `PUT /api/cart/items`（购物车更新）。
2. 新增聊天已读接口与事件：`POST /api/chat/read` + `WS chat.read`。
3. 新增聊天留存任务：90天消息清理 `ChatRetentionJob`。
4. 修复交易一致性：下单原子锁定商品，取消释放，支付成功置 `SOLD`，降低超卖风险。
5. 商品列表仅展示 `ON_SHELF`，避免待审核商品被公开检索。
6. 评价支持图片上传（最多6张）并落库存储 URL。
7. 卖家信誉分支持动态复算（管理员接口 + 定时任务）。
8. 通知中心补齐浏览器通知提醒与短信模拟日志。
9. 验证码缓存切换为 Redis（TTL=300秒），Redis异常时保底降级内存，保证登录/注册链路可用。

## 回归验证结果
1. 后端：`mvn -q test` 通过。
2. 前端：`npm run build` 通过（存在 chunk 体积告警，不阻断）。
