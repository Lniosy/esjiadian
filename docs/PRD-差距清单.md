# PRD 差距清单（2026-03-05）

## P0（核心闭环）
1. 当前状态
- 核心交易闭环已打通（注册登录、实名、商品、购物车、下单、模拟支付、模拟物流、收货、评价、退款/退货退款）。
- 当前无阻断性 P0 缺口，后续重点转入 P1/P2 提升项。

## P1（管理与运营能力不足）
1. 当前状态
- 管理后台已支持用户启停、订单导出、趋势统计与趋势导出。
- P1 级核心能力已覆盖，后续主要是体验增强与监控深度提升。

## P2（体验增强项）
1. 系统监控高级指标
- 现状：已支持 CPU/进程/JVM 阈值告警 + MySQL/Redis 可用性告警 + 管理员通知 + 接口调用统计/平均响应时间/慢请求计数 + MySQL慢查询计数与Top SQL明细 + Redis命中率。
- 建议：可继续补告警分级处理策略（如 WARN/ERROR 自动化动作）。

2. 聊天增强收尾
- 现状：已支持文本/图片上传（本地存储）/商品卡片、已读、举报、WS实时接收、浏览器通知提醒、短信通知模拟日志。
- 建议：若后续上生产，可替换为 Service Worker Push + 短信网关（阿里云/腾讯云）真接口。

3. 评价体系完善
- 现状：已支持评分、标签、评分分布、标签云、评价图片上传、卖家信誉分动态策略与定时复算。
- 建议：可继续增加低分惩罚权重、近期评价加权等策略。

4. 商品信息模板完善
- 现状：已补购买日期/维修历史/视频URL字段与筛选排序。
- 建议：补独立分类导航与更完整详情展示（卖家信誉等级等）。

## 推荐落地顺序（下一轮）
1. 前端补首页推荐位：精选推荐轮播 + 附近好物（同城）区块（后端已提供 `GET /api/recommend/featured`、`GET /api/recommend/nearby`）
2. 前端补商品详情视频展示（`videoUrl` 非空时展示视频播放器）
3. 前端接入收藏入口与状态（后端已提供 `/api/favorites/*`）

## 本轮已完成（2026-03-05）
1. 接口安全补齐：新增后端全局限流防刷能力（匿名IP与登录用户双阈值）
2. 监控告警增强：支持 WARN/ERROR 分级和分级通知
3. 错误日志治理：新增 `error_log` 持久化、全局异常写入、管理员查询接口 `/api/admin/error-logs`
4. 网络带宽指标补齐：监控快照新增 `networkInboundBps/networkOutboundBps/networkTotalBps`
5. 推荐个性化补齐：基于 `recommend_trace` 的行为加权首页推荐 + 相关推荐排序增强（品牌/分类/价格带/同卖家）
6. 分类层级后端能力补齐：新增分类树/平铺查询接口（`GET /api/categories/tree`、`GET /api/categories`）+ 启动分类种子数据
7. 安全增强补齐：限流支持按接口分组阈值配置（认证/交易/聊天/管理端分组）
8. 推荐策略增强：新增 `SEARCH_EXPOSE` 行为权重，首页推荐改为“个性化 + 热门 + 新上架”混排
9. 推荐联调能力补齐：新增推荐行为上报接口 `POST /api/recommend/events`（支持前端上报 `RECOMMEND_CLICK`/`SEARCH_EXPOSE` 等事件）
10. 推荐事件兼容补丁：`/api/recommend/events` 支持无 `productId` 事件（如 `CATEGORY_CLICK`）入库，避免前后端联调 400
11. 推荐偏好增强：`recommend_trace` 新增 `category_id`，`CATEGORY_FILTER/CATEGORY_CLICK` 可直接沉淀分类偏好并参与首页推荐打分
12. 商品详情补齐：`ProductDto` 新增卖家认证状态并在商品详情展示（昵称/信誉/认证状态）
13. 收藏链路后端补齐：新增 `user_favorite` 表 + 收藏接口（`GET /api/favorites/ids`、`GET /api/favorites/{productId}/exists`、`POST/DELETE /api/favorites/{productId}`）
14. 推荐策略补齐：收藏行为并入偏好权重（`FAVORITE`），支持“收藏/购买记录推荐”
15. 推荐位后端补齐：新增精选推荐 `GET /api/recommend/featured` 与同城推荐 `GET /api/recommend/nearby`
