<template>
  <div style="display:grid;gap:16px;">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">管理后台概览</h3>
          <div style="display:flex;gap:8px;">
            <el-button @click="recomputeSellerScores">重算卖家信誉分</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户数">{{ stats.userCount }}</el-descriptions-item>
        <el-descriptions-item label="商品数">{{ stats.productCount }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ stats.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="评价数">{{ stats.evaluationCount }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header><h3 style="margin:0;">系统监控</h3></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="MySQL">{{ monitor.database.mysqlUp ? 'UP' : 'DOWN' }}</el-descriptions-item>
        <el-descriptions-item label="Redis">{{ monitor.cache.redisUp ? 'UP' : 'DOWN' }}</el-descriptions-item>
        <el-descriptions-item label="CPU负载">{{ Number(monitor.server.cpuLoad || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="进程CPU">{{ Number(monitor.server.processCpuLoad || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="JVM内存">{{ monitor.server.jvmUsedMemory }} / {{ monitor.server.jvmTotalMemory }}</el-descriptions-item>
        <el-descriptions-item label="JVM使用率">{{ Number(monitor.server.jvmMemoryUsage || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="运行时长(ms)">{{ monitor.application.uptimeMillis }}</el-descriptions-item>
        <el-descriptions-item label="接口调用总数">{{ monitor.application.requestMetrics?.requestCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="平均响应(ms)">{{ Number(monitor.application.requestMetrics?.avgResponseMs || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="慢请求数">{{ monitor.application.requestMetrics?.slowRequestCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="慢请求阈值(ms)">{{ monitor.application.requestMetrics?.slowRequestThresholdMs || 0 }}</el-descriptions-item>
        <el-descriptions-item label="MySQL慢查询">{{ monitor.databaseMetrics?.slowQueries ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="MySQL连接数">{{ monitor.databaseMetrics?.threadsConnected ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Redis命中次数">{{ monitor.cacheMetrics?.keyspaceHits ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Redis未命中">{{ monitor.cacheMetrics?.keyspaceMisses ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Redis命中率">{{ formatRate(monitor.cacheMetrics?.hitRate) }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:12px;">
        <div style="font-weight:600;margin-bottom:8px;">监控告警（实时判断）</div>
        <el-table :data="monitorAlerts" size="small">
          <el-table-column prop="level" label="级别" width="80" />
          <el-table-column prop="title" label="告警标题" width="180" />
          <el-table-column prop="message" label="详情" />
        </el-table>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">告警事件（可确认）</h3>
          <el-button @click="loadAlertEvents">刷新</el-button>
        </div>
      </template>
      <el-table :data="alertEvents">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="level" label="级别" width="90" />
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="message" label="内容" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" type="primary" :disabled="scope.row.status !== 'OPEN'" @click="ackAlert(scope.row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">MySQL 慢查询明细（Top）</h3>
          <el-button @click="loadSlowQueries">刷新</el-button>
        </div>
      </template>
      <div v-if="!slowQueryState.available" style="margin-bottom:8px;color:#e6a23c;">
        {{ slowQueryState.message || 'performance_schema 不可用或无权限' }}
      </div>
      <el-table :data="slowQueries">
        <el-table-column prop="count" label="执行次数" width="100" />
        <el-table-column prop="avgMs" label="平均耗时(ms)" width="130" />
        <el-table-column prop="totalMs" label="总耗时(ms)" width="130" />
        <el-table-column prop="rowsExamined" label="扫描行数" width="120" />
        <el-table-column prop="digestText" label="SQL摘要" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">短信通知日志（模拟）</h3>
          <el-button @click="loadSmsLogs">刷新</el-button>
        </div>
      </template>
      <el-table :data="smsLogs">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="类型" width="110" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="phoneMasked" label="手机号" width="130" />
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="provider" label="通道" width="100" />
        <el-table-column prop="status" label="状态" width="90" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">关键操作审计日志</h3>
          <el-button @click="loadOperationLogs">刷新</el-button>
        </div>
      </template>
      <el-table :data="operationLogs">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operatorId" label="操作人ID" width="90" />
        <el-table-column prop="operatorRole" label="角色" width="90" />
        <el-table-column prop="action" label="动作" width="210" />
        <el-table-column prop="targetType" label="目标类型" width="100" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column prop="detail" label="详情" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
          <h3 style="margin:0;">违规举报处理</h3>
          <div style="display:flex;gap:8px;">
            <el-select v-model="reportFilter.status" style="width:130px;">
              <el-option label="待处理" value="PENDING" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-input v-model="reportFilter.keyword" placeholder="关键词筛选" style="width:200px;" />
            <el-button @click="loadChatReports">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="chatReports">
        <el-table-column prop="id" label="举报ID" width="90" />
        <el-table-column prop="messageId" label="消息ID" width="90" />
        <el-table-column prop="reporterName" label="举报人" width="120" />
        <el-table-column prop="reportedUserName" label="被举报人" width="120" />
        <el-table-column prop="reason" label="举报原因" width="180" />
        <el-table-column prop="messageType" label="类型" width="90" />
        <el-table-column prop="messageContent" label="消息内容" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="decisionNote" label="处理说明" width="160" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" type="danger" :disabled="scope.row.status !== 'PENDING'" @click="resolveChatReport(scope.row, 'CONFIRMED')">判定违规</el-button>
            <el-button size="small" :disabled="scope.row.status !== 'PENDING'" @click="resolveChatReport(scope.row, 'REJECTED')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">趋势统计</h3>
          <div>
            <el-select v-model="trendDays" style="width:110px;margin-right:8px;">
              <el-option :value="7" label="近7天" />
              <el-option :value="14" label="近14天" />
              <el-option :value="30" label="近30天" />
            </el-select>
            <el-button @click="loadTrends">刷新</el-button>
            <el-button type="primary" @click="exportTrends">导出趋势CSV</el-button>
          </div>
        </div>
      </template>
      <el-table :data="trendList">
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column prop="userCount" label="新增用户" width="100" />
        <el-table-column prop="productCount" label="新增商品" width="100" />
        <el-table-column prop="orderCount" label="新增订单" width="100" />
        <el-table-column prop="gmv" label="GMV" width="120" />
      </el-table>
      <div style="display:grid;grid-template-columns:1fr;gap:12px;margin-top:12px;">
        <div style="border:1px solid #ebeef5;border-radius:8px;padding:8px;">
          <div style="font-weight:600;margin-bottom:6px;">用户/订单趋势</div>
          <svg viewBox="0 0 640 180" style="width:100%;height:180px;background:#fafafa;border-radius:6px;">
            <polyline :points="chartPoints('userCount')" fill="none" stroke="#409eff" stroke-width="2" />
            <polyline :points="chartPoints('orderCount')" fill="none" stroke="#67c23a" stroke-width="2" />
          </svg>
          <div style="display:flex;gap:12px;font-size:12px;color:#606266;">
            <span>蓝线：新增用户</span>
            <span>绿线：新增订单</span>
          </div>
        </div>
        <div style="border:1px solid #ebeef5;border-radius:8px;padding:8px;">
          <div style="font-weight:600;margin-bottom:6px;">GMV 趋势</div>
          <svg viewBox="0 0 640 180" style="width:100%;height:180px;background:#fafafa;border-radius:6px;">
            <polyline :points="chartPoints('gmv')" fill="none" stroke="#e6a23c" stroke-width="2" />
          </svg>
          <div style="font-size:12px;color:#606266;">橙线：每日 GMV</div>
        </div>
      </div>
    </el-card>

    <el-card>
      <template #header><h3 style="margin:0;">纠纷处理</h3></template>
      <el-table :data="disputes">
        <el-table-column prop="id" label="纠纷ID" width="90" />
        <el-table-column prop="orderId" label="订单ID" width="90" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="resultNote" label="结果" width="220" />
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button size="small" type="success" @click="resolve(scope.row, 'RESOLVED')">结案</el-button>
            <el-button size="small" @click="resolve(scope.row, 'PROCESSING')">处理中</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header><h3 style="margin:0;">用户管理</h3></template>
      <div style="display:flex;gap:8px;align-items:center;margin-bottom:10px;flex-wrap:wrap;">
        <el-input v-model="userFilter.keyword" placeholder="昵称/邮箱/手机号" style="width:220px;" />
        <el-select v-model="userFilter.enabled" placeholder="启用状态" style="width:120px;">
          <el-option label="全部" :value="null" />
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
        <el-select v-model="userFilter.role" placeholder="角色" style="width:140px;">
          <el-option label="全部" value="" />
          <el-option label="买家" value="ROLE_BUYER" />
          <el-option label="卖家" value="ROLE_SELLER" />
          <el-option label="管理员" value="ROLE_ADMIN" />
        </el-select>
        <el-button type="primary" @click="loadUsers">筛选</el-button>
      </div>
      <el-table :data="users">
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="roles" label="角色" width="180" />
        <el-table-column prop="authStatus" label="实名状态" width="130" />
        <el-table-column label="启用状态" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'danger'">{{ scope.row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" type="success" @click="enableUser(scope.row)" :disabled="scope.row.enabled">启用</el-button>
            <el-button size="small" type="danger" @click="disableUser(scope.row)" :disabled="!scope.row.enabled">禁用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">订单管理</h3>
          <el-button type="primary" @click="exportOrders">导出CSV</el-button>
        </div>
      </template>
      <el-table :data="orders">
        <el-table-column prop="id" label="订单ID" width="90" />
        <el-table-column prop="orderNo" label="订单号" width="210" />
        <el-table-column prop="buyerId" label="买家ID" width="90" />
        <el-table-column prop="sellerId" label="卖家ID" width="90" />
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column prop="paidAmount" label="金额" width="120" />
        <el-table-column prop="status" label="状态" width="140" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, disputeApi } from '../../api/modules'

const stats = reactive({ userCount: 0, productCount: 0, orderCount: 0, evaluationCount: 0 })
const monitor = reactive({ server: {}, application: {}, database: {}, databaseMetrics: {}, cache: {}, cacheMetrics: {} })
const disputes = ref([])
const users = ref([])
const orders = ref([])
const trendDays = ref(7)
const trendList = ref([])
const monitorAlerts = ref([])
const alertEvents = ref([])
const smsLogs = ref([])
const chatReports = ref([])
const slowQueries = ref([])
const operationLogs = ref([])
const slowQueryState = reactive({ available: true, message: '' })
const userFilter = reactive({
  keyword: '',
  enabled: null,
  role: ''
})
const reportFilter = reactive({
  status: 'PENDING',
  keyword: ''
})

const load = async () => {
  const data = await adminApi.stats()
  Object.assign(stats, data)
  const m = await adminApi.monitor()
  Object.assign(monitor.server, m.server || {})
  Object.assign(monitor.application, m.application || {})
  Object.assign(monitor.database, m.database || {})
  Object.assign(monitor.databaseMetrics, m.databaseMetrics || {})
  Object.assign(monitor.cache, m.cache || {})
  Object.assign(monitor.cacheMetrics, m.cacheMetrics || {})
  monitorAlerts.value = m.alerts || []
  await loadAlertEvents()
  await loadSlowQueries()
  await loadSmsLogs()
  await loadOperationLogs()
  await loadChatReports()
  disputes.value = await disputeApi.all()
  await loadUsers()
  orders.value = await adminApi.orders()
  await loadTrends()
}

const recomputeSellerScores = async () => {
  const r = await adminApi.recomputeSellerScores()
  ElMessage.success(`已重算 ${r.recomputed} 个卖家信誉分`)
}

const loadAlertEvents = async () => {
  alertEvents.value = await adminApi.monitorAlerts('OPEN')
}

const loadSmsLogs = async () => {
  smsLogs.value = await adminApi.smsLogs({ limit: 100 })
}

const loadOperationLogs = async () => {
  operationLogs.value = await adminApi.operationLogs(100)
}

const loadSlowQueries = async () => {
  const data = await adminApi.monitorSlowQueries(20)
  slowQueryState.available = Boolean(data?.available)
  slowQueryState.message = data?.message || ''
  slowQueries.value = data?.list || []
}

const loadChatReports = async () => {
  chatReports.value = await adminApi.chatReports({
    status: reportFilter.status,
    keyword: reportFilter.keyword || null
  })
}

const resolveChatReport = async (row, status) => {
  await adminApi.resolveChatReport(row.id, {
    status,
    note: status === 'CONFIRMED' ? '消息内容违规' : '证据不足，驳回举报'
  })
  ElMessage.success('举报处理完成')
  await loadChatReports()
}

const ackAlert = async (row) => {
  await adminApi.ackMonitorAlert(row.id)
  ElMessage.success('告警已确认')
  await loadAlertEvents()
}

const loadUsers = async () => {
  users.value = await adminApi.users({
    keyword: userFilter.keyword || null,
    enabled: userFilter.enabled,
    role: userFilter.role || null
  })
}

const resolve = async (row, status) => {
  await disputeApi.resolve(row.id, { status, resultNote: status === 'RESOLVED' ? '平台已处理' : '已介入处理中' })
  await load()
}

const enableUser = async (row) => {
  await adminApi.enableUser(row.userId)
  ElMessage.success('用户已启用')
  await load()
}

const disableUser = async (row) => {
  await adminApi.disableUser(row.userId)
  ElMessage.success('用户已禁用')
  await load()
}

const exportOrders = async () => {
  const token = localStorage.getItem('token') || ''
  const resp = await fetch('/api/admin/orders/export', {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
  if (!resp.ok) {
    throw new Error('导出失败')
  }
  const blob = await resp.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'orders.csv'
  a.click()
  window.URL.revokeObjectURL(url)
}

const loadTrends = async () => {
  const data = await adminApi.trends(trendDays.value)
  trendList.value = data.list || []
}

const exportTrends = async () => {
  const token = localStorage.getItem('token') || ''
  const resp = await fetch(`/api/admin/stats/trends/export?days=${trendDays.value}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
  if (!resp.ok) {
    throw new Error('导出失败')
  }
  const blob = await resp.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'stats-trends.csv'
  a.click()
  window.URL.revokeObjectURL(url)
}

const chartPoints = (key) => {
  const list = trendList.value || []
  if (!list.length) return ''
  const width = 640
  const height = 180
  const padding = 20
  const xs = list.map((_, i) => padding + (i * (width - padding * 2)) / Math.max(1, list.length - 1))
  const ysRaw = list.map((it) => Number(it[key] || 0))
  const min = Math.min(...ysRaw)
  const max = Math.max(...ysRaw)
  return xs.map((x, i) => {
    const ratio = max === min ? 0.5 : (ysRaw[i] - min) / (max - min)
    const y = height - padding - ratio * (height - padding * 2)
    return `${x},${y}`
  }).join(' ')
}

const formatRate = (n) => {
  if (n === null || n === undefined || Number(n) < 0) return '-'
  return `${(Number(n) * 100).toFixed(2)}%`
}

onMounted(load)
</script>
