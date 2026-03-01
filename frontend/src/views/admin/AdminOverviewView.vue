<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>经营概览</h3>
          <div class="actions">
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

    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>趋势统计</h3>
          <div class="actions">
            <el-select v-model="trendDays" style="width:110px;">
              <el-option :value="7" label="近7天" />
              <el-option :value="14" label="近14天" />
              <el-option :value="30" label="近30天" />
            </el-select>
            <el-button @click="loadTrends">刷新</el-button>
            <el-button type="primary" @click="exportTrends">导出趋势数据</el-button>
          </div>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="trendList">
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column prop="userCount" label="新增用户" width="100" />
        <el-table-column prop="productCount" label="新增商品" width="100" />
        <el-table-column prop="orderCount" label="新增订单" width="100" />
        <el-table-column label="成交额(元)" width="120">
          <template #default="scope">
            {{ amountText(scope.row.gmv) }}
          </template>
        </el-table-column>
      </el-table>
      <div class="chart-stack">
        <section class="chart-box">
          <div class="chart-title">用户/订单趋势</div>
          <svg viewBox="0 0 640 180" class="chart-svg">
            <polyline :points="chartPoints('userCount')" fill="none" stroke="#2f7bff" stroke-width="3" stroke-linecap="round" />
            <polyline :points="chartPoints('orderCount')" fill="none" stroke="#08b48a" stroke-width="3" stroke-linecap="round" />
          </svg>
          <div class="legend"><span>蓝线：新增用户</span><span>绿线：新增订单</span></div>
        </section>
        <section class="chart-box">
          <div class="chart-title">成交额趋势</div>
          <svg viewBox="0 0 640 180" class="chart-svg">
            <polyline :points="chartPoints('gmv')" fill="none" stroke="#ee8c1a" stroke-width="3" stroke-linecap="round" />
          </svg>
          <div class="legend"><span>橙线：每日成交额</span></div>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/modules'

const stats = reactive({ userCount: 0, productCount: 0, orderCount: 0, evaluationCount: 0 })
const trendDays = ref(7)
const trendList = ref([])

const load = async () => {
  Object.assign(stats, await adminApi.stats())
  await loadTrends()
}

const recomputeSellerScores = async () => {
  const r = await adminApi.recomputeSellerScores()
  ElMessage.success(`已重算 ${r.recomputed} 个卖家信誉分`)
}

const loadTrends = async () => {
  const data = await adminApi.trends(trendDays.value)
  trendList.value = data.list || []
}

const amountText = (n) => Number(n || 0).toFixed(2)

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
  a.download = '趋势统计数据.csv'
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

onMounted(() => {
  load().catch(() => {})
})
</script>

<style scoped>
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-stack {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-top: 12px;
}

.chart-box {
  border: 1px solid var(--app-border);
  border-radius: 14px;
  padding: 10px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.chart-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.chart-svg {
  width: 100%;
  height: 180px;
  background:
    linear-gradient(180deg, rgba(29, 109, 255, 0.03), rgba(29, 109, 255, 0)),
    repeating-linear-gradient(to right, transparent 0, transparent 63px, rgba(167, 183, 207, 0.2) 64px),
    repeating-linear-gradient(to bottom, transparent 0, transparent 35px, rgba(167, 183, 207, 0.2) 36px);
  border-radius: 10px;
}

.legend {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #606266;
}
</style>
