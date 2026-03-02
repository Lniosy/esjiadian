<template>
  <div class="chat-page">
    <aside class="chat-sidebar">
      <div class="sidebar-head">
        <h3>会话</h3>
        <el-tag size="small" :type="wsConnected ? 'success' : 'info'">
          {{ wsConnected ? '在线' : '重连中' }}
        </el-tag>
      </div>

      <div class="tips">从商品卡片或卖家主页进入聊天，确保只和平台内有效用户会话。</div>

      <div class="conversation-list" v-if="conversations.length">
        <button
          v-for="item in conversations"
          :key="item.id"
          class="conversation-item"
          :class="{ active: item.id === activeConversationId }"
          @click="openConversation(item.id)"
        >
          <div class="avatar">{{ (item.peerName || String(item.peerId)).slice(0, 2) }}</div>
          <div class="meta">
            <div class="name-row">
              <span class="name">{{ item.peerName || `用户 ${item.peerId}` }}</span>
              <span class="time">{{ formatTime(item.lastAt) }}</span>
            </div>
            <div class="preview-row">
              <span class="preview">{{ item.lastPreview || '暂无消息' }}</span>
              <el-badge v-if="item.unread > 0" :value="item.unread" />
            </div>
          </div>
        </button>
      </div>
      <el-empty v-else description="暂无会话，可先从商品详情进入卖家主页发起聊天" />
    </aside>

    <section class="chat-main">
      <template v-if="activeConversationId">
        <header class="chat-header">
          <div class="peer">
            <span class="peer-avatar">{{ activePeerName.slice(0, 2) }}</span>
            <span>{{ activePeerName }}</span>
          </div>
          <el-button text @click="refreshCurrent">刷新</el-button>
        </header>

        <div class="message-list" ref="messageListRef">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="msg-row"
            :class="{ mine: Number(msg.fromUserId) === currentUserId }"
          >
            <div class="bubble-wrap">
              <div class="bubble-meta">
                <span>#{{ msg.id }}</span>
                <span>{{ formatTime(msg.createdAt) }}</span>
              </div>

              <div class="bubble" :class="msg.contentType === 'IMAGE' ? 'bubble-image' : ''">
                <template v-if="msg.contentType === 'IMAGE'">
                  <a :href="msg.content" target="_blank" rel="noreferrer">
                    <img :src="msg.content" alt="聊天图片" class="chat-image" />
                  </a>
                </template>
                <template v-else-if="msg.contentType === 'PRODUCT_CARD'">
                  <div class="product-card">
                    <div class="pc-title">商品卡片</div>
                    <div class="pc-content">{{ parseCard(msg.content) }}</div>
                  </div>
                </template>
                <template v-else>
                  <span class="text-content">{{ msg.content }}</span>
                </template>
              </div>

              <div class="bubble-actions" v-if="Number(msg.fromUserId) !== currentUserId">
                <el-button text size="small" @click="report(msg)">举报</el-button>
              </div>
            </div>
          </div>
        </div>

        <footer class="chat-input-bar">
          <el-input
            v-model="draft"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            resize="none"
            placeholder="输入消息，回车发送，Shift+回车换行"
            @keydown.enter.exact.prevent="sendText"
          />
          <div class="actions">
            <el-button @click="pickImage">图片</el-button>
            <el-button @click="openProductPicker">商品卡片</el-button>
            <el-button type="primary" :loading="sending" @click="sendText">发送</el-button>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="onFileChange" />
        </footer>
      </template>

      <div v-else class="empty-main">
        <el-empty description="请选择会话，或先在订单工作台/卖家主页直接发送消息" />
      </div>
    </section>

    <el-dialog v-model="productPickerVisible" title="选择商品卡片" width="520px">
      <el-select
        v-model="pickedProductId"
        filterable
        placeholder="请选择你在售的商品"
        style="width:100%;"
        :loading="loadingMyProducts"
      >
        <el-option
          v-for="item in myProducts"
          :key="item.id"
          :label="`${item.title}（￥${item.price}）`"
          :value="item.id"
        />
      </el-select>
      <el-empty v-if="!loadingMyProducts && !myProducts.length" description="暂无可发送商品，请先发布并上架商品" />
      <template #footer>
        <div class="action-group">
          <el-button @click="productPickerVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!pickedProductId" @click="sendSelectedProductCard">发送商品卡片</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Client } from '@stomp/stompjs'
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi, productApi, userApi } from '../../api/modules'
import { uploadImageFile } from '../../utils/upload-image'

const currentUserId = Number(localStorage.getItem('userId') || 0)
const token = localStorage.getItem('token') || ''
const route = useRoute()

const conversations = ref([])
const activeConversationId = ref('')
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const wsConnected = ref(false)
const fileInput = ref(null)
const messageListRef = ref(null)
const peerProfileCache = ref({})
const productPickerVisible = ref(false)
const loadingMyProducts = ref(false)
const myProducts = ref([])
const pickedProductId = ref(null)
let wsClient = null

const activePeerId = computed(() => extractPeerId(activeConversationId.value))
const activePeerName = computed(() => {
  const id = activePeerId.value
  if (!id) return '请选择会话'
  return (
    conversations.value.find((c) => c.id === activeConversationId.value)?.peerName ||
    peerProfileCache.value[id]?.nickname ||
    `用户${id}`
  )
})

const normalizeConversationId = (a, b) => {
  const x = Number(a)
  const y = Number(b)
  return `${Math.min(x, y)}_${Math.max(x, y)}`
}

const extractPeerId = (conversationId) => {
  const parts = String(conversationId || '').split('_').map((n) => Number(n))
  if (parts.length !== 2 || parts.some(Number.isNaN)) return 0
  return parts[0] === currentUserId ? parts[1] : parts[0]
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return ''
  const hh = `${d.getHours()}`.padStart(2, '0')
  const mm = `${d.getMinutes()}`.padStart(2, '0')
  return `${hh}:${mm}`
}

const previewText = (msg) => {
  if (!msg) return ''
  if (msg.contentType === 'IMAGE') return '[图片]'
  if (msg.contentType === 'PRODUCT_CARD') return '[商品卡片]'
  return msg.content || ''
}

const upsertConversation = (conversationId, patch = {}) => {
  const idx = conversations.value.findIndex((c) => c.id === conversationId)
  if (idx < 0) {
    const peerId = extractPeerId(conversationId)
    const peerName = patch.peerName || peerProfileCache.value[peerId]?.nickname
    if (!peerName) return
    conversations.value.unshift({
      id: conversationId,
      peerId,
      peerName,
      lastAt: patch.lastAt || null,
      lastPreview: patch.lastPreview || '',
      unread: patch.unread || 0
    })
    return
  }
  conversations.value[idx] = { ...conversations.value[idx], ...patch }
}

const resolvePeerProfile = async (peerId) => {
  if (peerProfileCache.value[peerId]) return peerProfileCache.value[peerId]
  try {
    const profile = await userApi.publicProfile(peerId)
    peerProfileCache.value[peerId] = profile
    return profile
  } catch {
    return null
  }
}

const sortConversations = () => {
  conversations.value.sort((a, b) => {
    const ta = a.lastAt ? new Date(a.lastAt).getTime() : 0
    const tb = b.lastAt ? new Date(b.lastAt).getTime() : 0
    return tb - ta
  })
}

const scrollToBottom = async () => {
  await nextTick()
  const el = messageListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const buildConversationList = async () => {
  const ids = await chatApi.conversations()
  const items = []
  for (const id of ids) {
    const peerId = extractPeerId(id)
    const peer = await resolvePeerProfile(peerId)
    if (!peer) continue
    const list = await chatApi.messages(id)
    const last = list.length ? list[list.length - 1] : null
    items.push({
      id,
      peerId,
      peerName: peer.nickname,
      lastAt: last?.createdAt || null,
      lastPreview: previewText(last),
      unread: 0
    })
  }
  conversations.value = items
  sortConversations()
  if (!activeConversationId.value && conversations.value.length) {
    activeConversationId.value = conversations.value[0].id
  }
}

const openConversation = async (conversationId) => {
  activeConversationId.value = conversationId
  const list = await chatApi.messages(conversationId)
  messages.value = list
  upsertConversation(conversationId, {
    unread: 0,
    lastAt: list.length ? list[list.length - 1].createdAt : null,
    lastPreview: previewText(list[list.length - 1])
  })
  await chatApi.read({ conversationId }).catch(() => {})
  sortConversations()
  await scrollToBottom()
}

const createConversationForPeer = async (peerId) => {
  const targetId = Number(peerId)
  if (!targetId || targetId <= 0 || targetId === currentUserId) return
  try {
    const peer = await userApi.publicProfile(targetId)
    peerProfileCache.value[targetId] = peer
  } catch {
    ElMessage.error('目标用户不存在或不可聊天')
    return
  }
  const id = normalizeConversationId(currentUserId, targetId)
  upsertConversation(id, {
    peerId: targetId,
    peerName: peerProfileCache.value[targetId]?.nickname || `用户${targetId}`,
    unread: 0
  })
  sortConversations()
  await openConversation(id)
}

const refreshCurrent = async () => {
  if (!activeConversationId.value) return
  await openConversation(activeConversationId.value)
}

const sendText = async () => {
  const text = draft.value.trim()
  if (!text || !activePeerId.value) return
  sending.value = true
  try {
    await chatApi.send({ toUserId: activePeerId.value, contentType: 'TEXT', content: text })
    draft.value = ''
    await refreshCurrent()
  } finally {
    sending.value = false
  }
}

const pickImage = () => fileInput.value?.click()

const onFileChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file || !activePeerId.value) return
  try {
    const uploaded = await uploadImageFile(file)
    await chatApi.send({ toUserId: activePeerId.value, contentType: 'IMAGE', content: uploaded.url })
    await refreshCurrent()
  } finally {
    e.target.value = ''
  }
}

const loadMyProducts = async () => {
  loadingMyProducts.value = true
  try {
    const page = await productApi.list({
      pageNum: 1,
      pageSize: 100,
      sellerId: currentUserId,
      sortBy: 'LATEST'
    })
    myProducts.value = (page?.list || []).filter((p) => String(p.status || '').toUpperCase() === 'ON_SHELF')
    if (!myProducts.value.some((p) => Number(p.id) === Number(pickedProductId.value))) {
      pickedProductId.value = myProducts.value.length ? myProducts.value[0].id : null
    }
  } finally {
    loadingMyProducts.value = false
  }
}

const openProductPicker = async () => {
  if (!activePeerId.value) return
  await loadMyProducts().catch(() => {})
  productPickerVisible.value = true
}

const sendSelectedProductCard = async () => {
  if (!activePeerId.value || !pickedProductId.value) return
  const picked = myProducts.value.find((p) => Number(p.id) === Number(pickedProductId.value))
  if (!picked) {
    ElMessage.warning('请选择有效商品')
    return
  }
  const card = {
    productId: Number(picked.id),
    title: picked.title,
    price: picked.price
  }
  await chatApi.send({ toUserId: activePeerId.value, contentType: 'PRODUCT_CARD', content: JSON.stringify(card) })
  productPickerVisible.value = false
  await refreshCurrent()
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
    // 用户取消
  }
}

const parseWsBody = (body) => {
  if (!body) return null
  try {
    const data = JSON.parse(body)
    if (data && typeof data === 'object' && 'data' in data) return data.data
    return data
  } catch {
    return null
  }
}

const connectWs = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const brokerURL = `${protocol}://${window.location.host}/ws/chat`
  wsClient = new Client({
    brokerURL,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000
  })

  wsClient.onConnect = () => {
    wsConnected.value = true
    wsClient.subscribe('/user/queue/chat.receive', async (frame) => {
      const msg = parseWsBody(frame.body)
      if (!msg) return
      const conversationId = normalizeConversationId(msg.fromUserId, msg.toUserId)
      const peerId = extractPeerId(conversationId)
      const peer = await resolvePeerProfile(peerId)
      if (!peer) return
      const isActive = conversationId === activeConversationId.value

      upsertConversation(conversationId, {
        peerName: peer.nickname,
        lastAt: msg.createdAt,
        lastPreview: previewText(msg),
        unread: isActive ? 0 : (conversations.value.find((c) => c.id === conversationId)?.unread || 0) + 1
      })
      sortConversations()

      if (isActive) {
        messages.value.push(msg)
        await chatApi.read({ conversationId }).catch(() => {})
        await scrollToBottom()
      }
    })
  }

  wsClient.onWebSocketClose = () => {
    wsConnected.value = false
  }

  wsClient.onStompError = () => {
    wsConnected.value = false
  }

  wsClient.activate()
}

onMounted(async () => {
  try {
    await buildConversationList()
    const peerId = Number(route.query.peerId || 0)
    if (peerId > 0) {
      await createConversationForPeer(peerId)
    }
    if (activeConversationId.value) {
      await openConversation(activeConversationId.value)
    }
    connectWs()
  } catch {}
})

onUnmounted(() => {
  if (wsClient) {
    wsClient.deactivate()
    wsClient = null
  }
})
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 320px 1fr;
  min-height: calc(100vh - 140px);
  gap: 14px;
}

.chat-sidebar,
.chat-main {
  border: 1px solid #e7eaf0;
  border-radius: 16px;
  background: #fff;
}

.chat-sidebar {
  padding: 14px;
  display: flex;
  flex-direction: column;
  min-height: 640px;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sidebar-head h3 {
  margin: 0;
  font-size: 18px;
}

.tips {
  font-size: 12px;
  color: #738194;
  line-height: 1.6;
  background: #f5f8ff;
  border: 1px solid #e8efff;
  border-radius: 10px;
  padding: 8px 10px;
  margin-bottom: 12px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.conversation-item {
  border: 1px solid #edf1f6;
  background: #fafbfd;
  border-radius: 12px;
  padding: 10px;
  text-align: left;
  display: grid;
  grid-template-columns: 36px 1fr;
  gap: 10px;
  cursor: pointer;
}

.conversation-item.active {
  border-color: #3a7afe;
  background: #eef4ff;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #3a7afe, #36cfc9);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.meta {
  min-width: 0;
}

.name-row,
.preview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.name {
  font-weight: 600;
  color: #152238;
}

.time {
  font-size: 12px;
  color: #9aa4b2;
}

.preview {
  font-size: 13px;
  color: #6b7380;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.chat-main {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 640px;
}

.chat-header {
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f6;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.peer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #19253a;
}

.peer-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-grid;
  place-items: center;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(135deg, #3a7afe, #36cfc9);
}

.message-list {
  padding: 16px;
  overflow-y: auto;
  background: linear-gradient(180deg, #f8fafc, #f1f5f9);
}

.msg-row {
  display: flex;
  margin-bottom: 12px;
}

.msg-row.mine {
  justify-content: flex-end;
}

.bubble-wrap {
  max-width: 74%;
}

.bubble-meta {
  font-size: 12px;
  color: #98a1ae;
  margin-bottom: 4px;
  display: flex;
  gap: 10px;
}

.bubble {
  border-radius: 14px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e6ebf2;
  color: #1f2937;
  line-height: 1.5;
}

.msg-row.mine .bubble {
  background: linear-gradient(135deg, #2f7bff, #51a5ff);
  border: 0;
  color: #fff;
}

.bubble-image {
  padding: 6px;
}

.chat-image {
  max-width: 240px;
  max-height: 240px;
  border-radius: 10px;
  display: block;
}

.product-card {
  border-radius: 10px;
  background: rgba(16, 24, 40, 0.06);
  padding: 8px 10px;
}

.msg-row.mine .product-card {
  background: rgba(255, 255, 255, 0.2);
}

.pc-title {
  font-size: 12px;
  opacity: 0.85;
}

.pc-content {
  font-weight: 600;
  margin-top: 2px;
}

.bubble-actions {
  margin-top: 4px;
}

.chat-input-bar {
  border-top: 1px solid #eef2f6;
  padding: 12px;
  background: #fff;
}

.actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.empty-main {
  display: grid;
  place-items: center;
}

@media (max-width: 960px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .chat-sidebar,
  .chat-main {
    min-height: auto;
  }

  .bubble-wrap {
    max-width: 88%;
  }
}
</style>
