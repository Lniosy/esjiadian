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
        <el-descriptions-item label="缓存状态">{{ yesNoText(monitor.cache.redisUp) }}</el-descriptions-item>
        <el-descriptions-item label="服务负载">{{ Number(monitor.server.cpuLoad || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="进程负载">{{ Number(monitor.server.processCpuLoad || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="运行内存">{{ monitor.server.jvmUsedMemory }} / {{ monitor.server.jvmTotalMemory }}</el-descriptions-item>
        <el-descriptions-item label="内存使用率">{{ Number(monitor.server.jvmMemoryUsage || 0).toFixed(4) }}</el-descriptions-item>
        <el-descriptions-item label="运行时长(ms)">{{ monitor.application.uptimeMillis }}</el-descriptions-item>
        <el-descriptions-item label="接口调用总数">{{ monitor.application.requestMetrics?.requestCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="平均响应(ms)">{{ Number(monitor.application.requestMetrics?.avgResponseMs || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="慢请求数">{{ monitor.application.requestMetrics?.slowRequestCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="慢请求阈值(ms)">{{ monitor.application.requestMetrics?.slowRequestThresholdMs || 0 }}</el-descriptions-item>
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
            {{ alertLevelText(scope.row.level) }}
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
            {{ alertLevelText(scope.row.level) }}
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

const ackAlert = async (row) => {
  await adminApi.ackMonitorAlert(row.id)
  ElMessage.success('告警已确认')
  await loadAlertEvents()
}

const formatRate = (n) => {
  if (n === null || n === undefined || Number(n) < 0) return '-'
  return `${(Number(n) * 100).toFixed(2)}%`
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
</style>
