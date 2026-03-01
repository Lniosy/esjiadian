<template>
  <el-card>
    <template #header><h3 style="margin:0;">即时通讯</h3></template>

    <div style="display:flex;gap:12px;align-items:center;margin-bottom:12px;">
      <span>对方用户ID</span>
      <el-input-number v-model="toUserId" :min="1" />
      <el-button type="primary" @click="loadConversation">加载会话</el-button>
    </div>

    <el-table :data="messages" style="margin-bottom:12px;">
      <el-table-column prop="id" label="消息ID" width="90" />
      <el-table-column prop="fromUserId" label="发送方" width="90" />
      <el-table-column prop="toUserId" label="接收方" width="90" />
      <el-table-column label="内容">
        <template #default="scope">
          <template v-if="scope.row.contentType === 'IMAGE'">
            <a :href="scope.row.content" target="_blank">{{ scope.row.content }}</a>
          </template>
          <template v-else-if="scope.row.contentType === 'PRODUCT_CARD'">
            {{ parseCard(scope.row.content) }}
          </template>
          <template v-else>
            {{ scope.row.content }}
          </template>
        </template>
      </el-table-column>
      <el-table-column prop="contentType" label="类型" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" type="danger" @click="report(scope.row)">举报</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;gap:8px;">
      <el-input v-model="content" placeholder="输入消息" />
      <el-button type="primary" @click="send">发送</el-button>
      <el-button @click="pickImage">发图片</el-button>
      <el-button @click="sendProductCard">发商品卡片</el-button>
    </div>
    <input ref="fileInput" type="file" accept="image/*" style="display:none;" @change="onFileChange" />
  </el-card>
</template>

<script setup>
import { Client } from '@stomp/stompjs'
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi, fileApi } from '../../api/modules'

const toUserId = ref(1)
const content = ref('你好，我对这个商品感兴趣')
const messages = ref([])
const fileInput = ref(null)
let wsClient = null

const convId = () => {
  const myId = Number(localStorage.getItem('userId') || 0)
  const min = Math.min(myId, toUserId.value)
  const max = Math.max(myId, toUserId.value)
  return `${min}_${max}`
}

const loadConversation = async () => {
  messages.value = await chatApi.messages(convId())
  await chatApi.read({ conversationId: convId() }).catch(() => {})
}

const send = async () => {
  await chatApi.send({ toUserId: toUserId.value, contentType: 'TEXT', content: content.value })
  ElMessage.success('发送成功')
  content.value = ''
  await loadConversation()
}

const pickImage = () => fileInput.value?.click()

const onFileChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const uploaded = await fileApi.uploadImage(file)
    const url = uploaded.url
    await chatApi.send({ toUserId: toUserId.value, contentType: 'IMAGE', content: url })
    ElMessage.success('图片消息已发送')
    await loadConversation()
  } finally {
    e.target.value = ''
  }
}

const sendProductCard = async () => {
  try {
    const idInput = await ElMessageBox.prompt('输入商品ID', '发送商品卡片', { inputValue: '1' })
    const titleInput = await ElMessageBox.prompt('输入商品标题', '发送商品卡片', { inputValue: '二手冰箱 95新' })
    const priceInput = await ElMessageBox.prompt('输入商品价格', '发送商品卡片', { inputValue: '899.00' })
    const card = {
      productId: Number(idInput.value),
      title: titleInput.value,
      price: priceInput.value
    }
    await chatApi.send({ toUserId: toUserId.value, contentType: 'PRODUCT_CARD', content: JSON.stringify(card) })
    ElMessage.success('商品卡片已发送')
    await loadConversation()
  } catch {
    // 用户取消输入
  }
}

const parseCard = (raw) => {
  try {
    const card = JSON.parse(raw)
    return `商品#${card.productId} ${card.title} ￥${card.price}`
  } catch {
    return raw
  }
}

const report = async (row) => {
  try {
    const r = await ElMessageBox.prompt('请输入举报原因', '消息举报', { inputValue: '疑似违规内容' })
    await chatApi.report(row.id, { reason: r.value })
    ElMessage.success('举报已提交')
  } catch {
    // 用户取消输入
  }
}

const connectWs = () => {
  const token = localStorage.getItem('token') || ''
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const brokerURL = `${protocol}://${window.location.host}/ws/chat`
  wsClient = new Client({
    brokerURL,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000
  })
  wsClient.onConnect = () => {
    wsClient.subscribe('/user/queue/chat.receive', async () => {
      await loadConversation()
      ElMessage.success('收到新消息')
    })
  }
  wsClient.activate()
}

onMounted(async () => {
  connectWs()
  await loadConversation()
})

onUnmounted(() => {
  if (wsClient) {
    wsClient.deactivate()
    wsClient = null
  }
})
</script>
