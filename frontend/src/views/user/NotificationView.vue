<template>
  <div style="display:grid;gap:16px;">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
          <h3 style="margin:0;">通知中心</h3>
          <div style="display:flex;gap:8px;">
            <el-button @click="enableBrowserPush" :disabled="browserEnabled || !notificationSupported">
              {{ browserEnabled ? '浏览器通知已开启' : '开启浏览器通知' }}
            </el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items">
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="readFlag" label="已读" width="80" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button size="small" @click="read(scope.row.id)">标记已读</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header><h3 style="margin:0;">短信通知（模拟）</h3></template>
      <el-table :data="smsLogs">
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="phoneMasked" label="手机号" width="140" />
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="provider" label="通道" width="100" />
        <el-table-column prop="status" label="状态" width="90" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '../../api/modules'

const items = ref([])
const smsLogs = ref([])
const browserEnabled = ref(localStorage.getItem('notification-browser-enabled') === '1')
const notificationSupported = typeof window !== 'undefined' && 'Notification' in window
let timer = null

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
  await load()
  timer = setInterval(load, 15000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>
