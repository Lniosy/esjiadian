<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>通知中心</h3>
          <div class="actions-row">
            <el-button @click="enableBrowserPush" :disabled="browserEnabled || !notificationSupported">
              {{ browserEnabled ? '浏览器通知已开启' : '开启浏览器通知' }}
            </el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="kpi-grid" style="margin-bottom: 12px;">
        <article class="kpi-item">
          <div class="kpi-label">通知总数</div>
          <div class="kpi-value">{{ items.length }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">未读通知</div>
          <div class="kpi-value">{{ unreadCount }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">短信日志</div>
          <div class="kpi-value">{{ smsLogs.length }}</div>
        </article>
      </div>

      <el-table empty-text="暂无数据" :data="items">
        <el-table-column label="时间" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="scope">
            {{ notifyTypeText(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="已读" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.readFlag ? 'success' : 'warning'">{{ scope.row.readFlag ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" @click="read(scope.row.id)">标记已读</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header><div class="page-head"><h3>短信通知（模拟）</h3></div></template>
      <el-table empty-text="暂无数据" :data="smsLogs">
        <el-table-column label="时间" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="scope">
            {{ notifyTypeText(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="phoneMasked" label="手机号" width="140" />
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="通道" width="100">
          <template #default="scope">
            {{ providerText(scope.row.provider) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            {{ sendStatusText(scope.row.status) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '../../api/modules'
import { formatDateTimeText, notifyTypeText, providerText, sendStatusText } from '../../utils/display'

const items = ref([])
const smsLogs = ref([])
const browserEnabled = ref(localStorage.getItem('notification-browser-enabled') === '1')
const notificationSupported = typeof window !== 'undefined' && 'Notification' in window
let timer = null

const unreadCount = computed(() => items.value.filter((n) => !n.readFlag).length)

const showBrowserNotification = (item) => {
  if (!notificationSupported || Notification.permission !== 'granted') return
  new Notification(item.title || '新通知', {
    body: item.content || '',
    tag: `site-notification-${item.id}`
  })
}

const detectNewNotification = (list) => {
  const latest = (list || [])[0]
  if (!latest) return
  const key = 'notification-last-seen-id'
  const lastSeen = Number(localStorage.getItem(key) || 0)
  if (browserEnabled.value && latest.id > lastSeen && !latest.readFlag) {
    showBrowserNotification(latest)
  }
  if (latest.id > lastSeen) {
    localStorage.setItem(key, String(latest.id))
  }
}

const load = async () => {
  items.value = await notificationApi.list()
  smsLogs.value = await notificationApi.smsLogs(30)
  detectNewNotification(items.value)
}

const read = async (id) => {
  await notificationApi.read(id)
  await load()
}

const enableBrowserPush = async () => {
  if (!notificationSupported) {
    ElMessage.warning('当前浏览器不支持通知')
    return
  }
  const p = await Notification.requestPermission()
  if (p === 'granted') {
    browserEnabled.value = true
    localStorage.setItem('notification-browser-enabled', '1')
    ElMessage.success('浏览器通知已开启')
  } else {
    ElMessage.warning('浏览器通知权限未授予')
  }
}

onMounted(async () => {
  try {
    await load()
  } catch {}
  timer = setInterval(() => {
    load().catch(() => {})
  }, 15000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>
