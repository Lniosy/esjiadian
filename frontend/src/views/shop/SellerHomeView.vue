<template>
  <div class="seller-page" v-loading="loading">

    <!-- 卖家主页 Profile 卡 -->
    <el-card class="profile-card" shadow="never">
      <div class="profile-main">
        <el-avatar :size="72" :src="profile?.avatarUrl || ''" class="profile-avatar">
          {{ avatarText }}
        </el-avatar>
        <div class="profile-info">
          <div class="name-row">
            <span class="seller-name">{{ profile?.nickname || '卖家' }}</span>
            <el-tag
              size="small"
              :type="profile?.authStatus === 'APPROVED' ? 'success' : 'info'"
              round
            >
              {{ profile?.authStatus === 'APPROVED' ? '✓ 已认证' : '未认证' }}
            </el-tag>
          </div>
          <p class="seller-bio">{{ profile?.bio || '这个卖家还没有填写简介。' }}</p>
          <div class="seller-stats">
            <span class="stat-item">
              <span class="stat-val">{{ products.length }}</span>
              <span class="stat-lab">在售</span>
            </span>
            <span class="stat-divider">|</span>
            <span class="stat-item">
              <span class="stat-val">{{ shop?.totalSold ?? 0 }}</span>
              <span class="stat-lab">已售</span>
            </span>
          </div>
        </div>
        <div class="profile-actions">
          <el-button type="primary" round size="large" @click="openChatDrawer()">
            💬 聊一聊
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 在售商品 -->
    <div class="section-title">
      <span>TA 的在售商品</span>
      <span class="count-badge">{{ products.length }} 件</span>
      <el-button text size="small" @click="loadAll" style="margin-left:auto;">刷新</el-button>
    </div>

    <div v-if="products.length" class="product-grid">
      <article v-for="item in products" :key="item.id" class="market-card">
        <div class="card-img-wrap">
          <img v-if="item.images?.[0]" :src="item.images[0]" alt="商品图" class="card-cover" />
          <div v-else class="card-cover empty-cover">
            <span>暂无图片</span>
          </div>
          <span v-if="item.tradeMethod === '同城自提'" class="trade-badge local">自提</span>
          <span v-else class="trade-badge mail">发货</span>
        </div>
        <div class="card-body">
          <div class="card-title-text">{{ item.title }}</div>
          <div class="card-meta">{{ item.brand }} {{ item.model }} · {{ item.region }}</div>
          <div class="card-footer">
            <span class="card-price">¥{{ item.price }}</span>
            <div class="card-btns">
              <el-button size="small" plain round @click="addToCart(item.id)">🛒</el-button>
              <el-button size="small" type="primary" round @click="openChatDrawer(item)">咨询</el-button>
            </div>
          </div>
        </div>
      </article>
    </div>
    <el-empty v-else-if="!loading" description="该卖家暂时没有在售商品" :image-size="80" />

    <el-drawer v-model="chatDrawerVisible" :title="`联系 ${profile?.nickname || '卖家'}`" size="36%">
      <el-alert type="info" :closable="false" show-icon>
        在当前页面直接发送消息，不需要跳转聊天页。
      </el-alert>
      <el-form label-width="80px" style="margin-top: 14px;">
        <el-form-item label="消息内容">
          <el-input
            v-model="chatDraft"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="请输入咨询内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="action-group">
          <el-button @click="chatDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="sendingChat" :disabled="!chatDraft.trim()" @click="sendChat">
            发送消息
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatApi, orderApi, productApi, shopApi, userApi } from '../../api/modules'

const route = useRoute()
const sellerId = Number(route.params.id)
const currentUserId = Number(localStorage.getItem('userId') || 0)
const loading = ref(false)
const profile = ref(null)
const products = ref([])
const shop = ref(null)
const chatDrawerVisible = ref(false)
const sendingChat = ref(false)
const chatDraft = ref('')

const avatarText = computed(() => {
  const n = profile.value?.nickname || '卖家'
  return n.slice(0, 2)
})

const loadAll = async () => {
  if (!sellerId || Number.isNaN(sellerId)) {
    ElMessage.error('卖家编号无效')
    return
  }
  loading.value = true
  try {
    profile.value = await userApi.publicProfile(sellerId)
    shop.value = await shopApi.byUser(sellerId).catch(() => null)
    const page = await productApi.list({
      pageNum: 1,
      pageSize: 50,
      sellerId,
      sortBy: 'LATEST'
    })
    products.value = page.list || []
  } finally {
    loading.value = false
  }
}

const addToCart = async (productId) => {
  await orderApi.addCart(productId)
  ElMessage.success('已加入购物车')
}

const openChatDrawer = (product = null) => {
  if (!sellerId || sellerId === currentUserId) {
    ElMessage.warning('当前账号无法发起该会话')
    return
  }
  if (product) {
    chatDraft.value = `你好，我想咨询商品「${product.title}」（#${product.id}）`
  } else {
    chatDraft.value = ''
  }
  chatDrawerVisible.value = true
}

const sendChat = async () => {
  const content = chatDraft.value.trim()
  if (!content) return
  try {
    await userApi.publicProfile(sellerId)
    sendingChat.value = true
    await chatApi.send({
      toUserId: sellerId,
      contentType: 'TEXT',
      content
    })
    ElMessage.success('消息已发送')
    chatDrawerVisible.value = false
    chatDraft.value = ''
  } catch {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    sendingChat.value = false
  }
}

onMounted(() => {
  loadAll().catch(() => {})
})
</script>

<style scoped>
.seller-page {
  display: grid;
  gap: 16px;
}

/* Profile 卡 */
.profile-card {
  border-radius: 16px !important;
  border: none !important;
  background: #fff;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 18px;
}

.profile-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #FF5722, #FF8C00);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.seller-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
}

.seller-bio {
  font-size: 13px;
  color: #888;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.seller-stats {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-val {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-primary, #FF5722);
  line-height: 1;
}

.stat-lab {
  font-size: 11px;
  color: #aaa;
  margin-top: 2px;
}

.stat-divider {
  color: #e0e0e0;
  font-size: 18px;
}

.profile-actions {
  flex-shrink: 0;
}

/* 分区标题 */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #333;
}

.count-badge {
  background: #fff3ee;
  color: var(--c-primary, #FF5722);
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

/* 商品瀑布网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.market-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  transition: transform 0.18s, box-shadow 0.18s;
  cursor: pointer;
}

.market-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(255, 87, 34, 0.12);
}

.card-img-wrap {
  position: relative;
  width: 100%;
  padding-top: 75%;
}

.card-cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.empty-cover {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  font-size: 12px;
  color: #bbb;
}

.trade-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 8px;
  font-weight: 600;
}

.trade-badge.local  { background: rgba(255,170,0,0.9); color:#fff; }
.trade-badge.mail   { background: rgba(255,87,34,0.9);  color:#fff; }

.card-body {
  padding: 10px;
}

.card-title-text {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.card-meta {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  gap: 6px;
}

.card-price {
  font-size: 18px;
  font-weight: 800;
  color: var(--c-primary, #FF5722);
}

.card-btns {
  display: flex;
  gap: 4px;
}

@media (max-width: 600px) {
  .profile-main { flex-wrap: wrap; }
  .product-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
