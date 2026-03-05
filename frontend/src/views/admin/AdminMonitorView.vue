<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>系统监控总览</h3>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="数据库状态">{{ yesNoText(monitor.database.mysqlUp) }}</el-descriptions-item>
        <el-descriptions-item label="在线用户">{{ monitor.application.onlineUsers ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="缓存状态">{{ yesNoText(monitor.cache.redisUp) }}</el-descriptions-item>
        <el-descriptions-item label="服务负载">{{ Number(monitor.server.cpuLoad || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="进程负载">{{ Number(monitor.server.processCpuLoad || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="运行内存">{{ monitor.server.jvmUsedMemory }} / {{ monitor.server.jvmTotalMemory }}</el-descriptions-item>
        <el-descriptions-item label="内存使用率">{{ formatRate(monitor.server.jvmMemoryUsage) }}</el-descriptions-item>
        <el-descriptions-item label="运行时长(ms)">{{ monitor.application.uptimeMillis }}</el-descriptions-item>
        <el-descriptions-item label="接口调用总数">{{ monitor.application.requestMetrics?.requestCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="平均响应(ms)">{{ Number(monitor.application.requestMetrics?.avgResponseMs || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="慢请求数">{{ monitor.application.requestMetrics?.slowRequestCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="慢请求阈值(ms)">{{ monitor.application.requestMetrics?.slowRequestThresholdMs || 0 }}</el-descriptions-item>
        <el-descriptions-item label="磁盘空间(空闲/总)">{{ formatSize(monitor.server.diskFree) }} / {{ formatSize(monitor.server.diskTotal) }}</el-descriptions-item>
        <el-descriptions-item label="磁盘使用率">{{ formatRate(monitor.server.diskUsage) }}</el-descriptions-item>
        <el-descriptions-item label="网络带宽(入/出)">
          <div class="bandwidth-viz">
            <div class="bw-item">
              <span class="bw-label">↓</span>
              <el-progress :percentage="bandwidthPercent(monitor.server.networkInboundBps)" :color="bandwidthColor(monitor.server.networkInboundBps)" :show-text="false" :stroke-width="10" />
              <el-tag :type="bandwidthTagType(monitor.server.networkInboundBps)" size="small">{{ formatBandwidth(monitor.server.networkInboundBps) }}</el-tag>
            </div>
            <div class="bw-item">
              <span class="bw-label">↑</span>
              <el-progress :percentage="bandwidthPercent(monitor.server.networkOutboundBps)" :color="bandwidthColor(monitor.server.networkOutboundBps)" :show-text="false" :stroke-width="10" />
              <el-tag :type="bandwidthTagType(monitor.server.networkOutboundBps)" size="small">{{ formatBandwidth(monitor.server.networkOutboundBps) }}</el-tag>
            </div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="网络总带宽">
          <div class="bandwidth-viz total">
            <el-progress :percentage="bandwidthPercent(monitor.server.networkTotalBps, true)" :color="bandwidthColor(monitor.server.networkTotalBps, true)" :show-text="false" :stroke-width="14" />
            <el-tag :type="bandwidthTagType(monitor.server.networkTotalBps, true)" effect="dark" size="small">{{ formatBandwidth(monitor.server.networkTotalBps) }}</el-tag>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="数据库慢查询数">{{ monitor.databaseMetrics?.slowQueries ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据库连接数">{{ monitor.databaseMetrics?.threadsConnected ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="缓存命中次数">{{ monitor.cacheMetrics?.keyspaceHits ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="缓存未命中">{{ monitor.cacheMetrics?.keyspaceMisses ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="缓存命中率">{{ formatRate(monitor.cacheMetrics?.hitRate) }}</el-descriptions-item>
      </el-descriptions>
      <div class="section-title">监控告警（实时）</div>
      <el-table empty-text="暂无数据" :data="monitorAlerts" size="small">
        <el-table-column label="发现时间" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.detectedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="级别" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.level === 'ERROR' ? 'danger' : (scope.row.level === 'WARN' ? 'warning' : 'info')" size="small">
              {{ alertLevelText(scope.row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="告警标题" width="180" />
        <el-table-column prop="message" label="详情" />
        <el-table-column label="有效至" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.expireAt) }}
          </template>
        </el-table-column>
        <el-table-column label="剩余时效" width="130">
          <template #default="scope">
            {{ remainDurationText(scope.row.expireAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>告警事件（可确认）</h3>
          <el-button @click="loadAlertEvents">刷新</el-button>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="alertEvents">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column label="首次触发" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最近更新" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="级别" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.level === 'ERROR' ? 'danger' : (scope.row.level === 'WARN' ? 'warning' : 'info')" size="small">
              {{ alertLevelText(scope.row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="message" label="内容" />
        <el-table-column label="持续时长" width="140">
          <template #default="scope">
            {{ alertDurationText(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            {{ alertStatusText(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="primary" :disabled="scope.row.status !== 'OPEN'" @click="ackAlert(scope.row)">确认</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>数据库慢查询明细</h3>
          <el-button @click="loadSlowQueries">刷新</el-button>
        </div>
      </template>
      <div v-if="!slowQueryState.available" class="warn-msg">{{ slowQueryState.message || '慢查询统计不可用或权限不足' }}</div>
      <el-table empty-text="暂无数据" :data="slowQueries">
        <el-table-column prop="count" label="执行次数" width="100" />
        <el-table-column prop="avgMs" label="平均耗时(ms)" width="130" />
        <el-table-column prop="totalMs" label="总耗时(ms)" width="130" />
        <el-table-column prop="rowsExamined" label="扫描行数" width="120" />
        <el-table-column prop="digestText" label="语句摘要" />
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>系统错误日志 (最近)</h3>
          <div class="filter-row">
            <el-input v-model="errorLogQuery.keyword" placeholder="关键词搜索" clearable style="width: 180px;" @change="loadErrorLogs" />
            <el-select v-model="errorLogQuery.httpStatus" placeholder="状态码" clearable style="width: 120px;" @change="loadErrorLogs">
              <el-option label="5xx 错误" value="5" />
              <el-option label="4xx 错误" value="4" />
            </el-select>
            <el-select v-model="errorLogQuery.limit" placeholder="数量" style="width: 100px;" @change="loadErrorLogs">
              <el-option label="50条" :value="50" />
              <el-option label="100条" :value="100" />
              <el-option label="200条" :value="200" />
            </el-select>
            <el-button @click="loadErrorLogs">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="errorLogs" size="small" style="width: 100%">
        <el-table-column type="expand">
          <template #default="props">
            <div class="expand-content">
              <div class="log-detail-item"><strong>异常类:</strong> {{ props.row.exceptionClass }}</div>
              <div class="log-detail-item"><strong>堆栈信息:</strong></div>
              <pre class="stack-trace">{{ props.row.stackTrace }}</pre>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.httpStatus)" size="small">
              {{ scope.row.httpStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="方法/路径" width="200">
          <template #default="scope">
            <code class="log-path">[{{ scope.row.method }}] {{ scope.row.path }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="errorCode" label="错误码" width="100" />
        <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip />
        <el-table-column prop="clientIp" label="IP" width="130" />
        <el-table-column prop="userId" label="用户ID" width="80" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/modules'
import { alertLevelText, alertStatusText, formatDateTimeText, formatDurationText, remainDurationText, yesNoText } from '../../utils/display'

const monitor = reactive({ server: {}, application: {}, database: {}, databaseMetrics: {}, cache: {}, cacheMetrics: {} })
const monitorAlerts = ref([])
const alertEvents = ref([])
const slowQueries = ref([])
const slowQueryState = reactive({ available: true, message: '' })
const errorLogs = ref([])
const errorLogQuery = reactive({ keyword: '', httpStatus: '', limit: 100 })

const load = async () => {
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
  await loadErrorLogs()
}

const loadAlertEvents = async () => {
  alertEvents.value = await adminApi.monitorAlerts('OPEN')
}

const loadSlowQueries = async () => {
  const data = await adminApi.monitorSlowQueries(20)
  slowQueryState.available = Boolean(data?.available)
  slowQueryState.message = data?.message || ''
  slowQueries.value = data?.list || []
}

const loadErrorLogs = async () => {
  errorLogs.value = await adminApi.errorLogs(errorLogQuery)
}

const statusTagType = (status) => {
  const s = Number(status)
  if (s >= 500) return 'danger'
  if (s >= 400) return 'warning'
  return 'info'
}

const ackAlert = async (row) => {
  await adminApi.ackMonitorAlert(row.id)
  ElMessage.success('告警已确认')
  await loadAlertEvents()
}

const formatRate = (n) => {
  if (n === null || n === undefined || Number(n) < 0) return '-'
  return `${(Number(n) * 100).toFixed(2)}%`
}

const formatSize = (bytes) => {
  if (bytes === null || bytes === undefined || bytes < 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let val = bytes
  let unitIndex = 0
  while (val >= 1024 && unitIndex < units.length - 1) {
    val /= 1024
    unitIndex++
  }
  return `${val.toFixed(2)} ${units[unitIndex]}`
}

const formatBandwidth = (bps) => {
  if (bps === null || bps === undefined || bps < 0) return '-'
  const units = ['B/s', 'KB/s', 'MB/s', 'GB/s']
  let val = bps
  let unitIndex = 0
  while (val >= 1024 && unitIndex < units.length - 1) {
    val /= 1024
    unitIndex++
  }
  return `${val.toFixed(2)} ${units[unitIndex]}`
}

const bandwidthTagType = (bps, isTotal = false) => {
  if (!bps) return 'info'
  // 阈值设定：单向 1MB/s 或 总和 2MB/s 以上显示警告
  const threshold = isTotal ? 2 * 1024 * 1024 : 1024 * 1024
  return bps > threshold ? 'warning' : 'success'
}

const bandwidthPercent = (bps, isTotal = false) => {
  if (!bps) return 0
  // 基准：10MB/s 为 100% (仅作展示比例用)
  const max = (isTotal ? 20 : 10) * 1024 * 1024
  return Math.min(100, Math.round((bps / max) * 100))
}

const bandwidthColor = (bps, isTotal = false) => {
  const s = bandwidthTagType(bps, isTotal)
  if (s === 'warning') return '#E6A23C'
  if (s === 'success') return '#67C23A'
  return '#909399'
}

const alertDurationText = (row) => {
  const start = Number(row?.createdAt || 0)
  if (!start) return '-'
  const end = Number(row?.updatedAt || Date.now())
  return formatDurationText(Math.max(0, end - start))
}

onMounted(() => {
  load().catch(() => {})
})
</script>

<style scoped>
.section-title {
  font-weight: 600;
  margin: 10px 0 8px;
}

.warn-msg {
  margin-bottom: 8px;
  color: #e6a23c;
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.log-path {
  background: #f5f7fa;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: monospace;
}

.expand-content {
  padding: 10px 20px;
  background: #fafafa;
}

.log-detail-item {
  margin-bottom: 8px;
}

.stack-trace {
  white-space: pre-wrap;
  word-break: break-all;
  background: #303133;
  color: #fff;
  padding: 15px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 400px;
  overflow-y: auto;
}

/* 流量可视化 */
.bandwidth-viz {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 180px;
}

.bandwidth-viz.total {
  flex-direction: row;
  align-items: center;
  gap: 12px;
}

.bandwidth-viz.total :deep(.el-progress) {
  flex: 1;
}

.bw-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bw-item :deep(.el-progress) {
  flex: 1;
}

.bw-label {
  font-family: monospace;
  font-weight: bold;
  color: var(--c-text-tertiary);
  width: 12px;
}
</style>
