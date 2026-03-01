<template>
  <div class="seller-page" v-loading="loading">
    <section class="hero-panel seller-hero">
      <div class="hero-left">
        <el-avatar :size="72" :src="profile?.avatarUrl || ''">{{ avatarText }}</el-avatar>
        <div>
          <div class="name-row">
            <h2>{{ profile?.nickname || '卖家' }}</h2>
            <el-tag size="small" :type="profile?.authStatus === 'APPROVED' ? 'success' : 'info'">
              {{ profile?.authStatus === 'APPROVED' ? '已认证' : '未认证' }}
            </el-tag>
          </div>
          <p>{{ profile?.bio || '这个卖家还没有填写简介。' }}</p>
          <div class="meta">在售商品 {{ products.length }} 件</div>
        </div>
      </div>
      <div class="hero-right">
        <el-button type="primary" size="large" @click="startChat">聊一聊</el-button>
      </div>
    </section>

    <el-card>
      <template #header>
        <div class="list-header">
          <h3>TA 的在售商品</h3>
          <el-button text @click="loadAll">刷新</el-button>
        </div>
      </template>
      <div v-if="products.length" class="product-grid">
        <article v-for="item in products" :key="item.id" class="product-card">
          <img v-if="item.images?.[0]" :src="item.images[0]" alt="商品图" class="cover" />
          <div v-else class="cover empty">暂无图片</div>
          <div class="card-body">
            <h4>{{ item.title }}</h4>
            <div class="price">¥{{ item.price }}</div>
            <div class="desc">{{ item.brand }} {{ item.model }} · {{ item.region }}</div>
            <div class="actions">
              <el-button size="small" @click="addToCart(item.id)">加入购物车</el-button>
              <el-button size="small" type="primary" @click="startChat">咨询</el-button>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="该卖家暂时没有在售商品" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi, productApi, shopApi, userApi } from '../../api/modules'

const route = useRoute()
const router = useRouter()
const sellerId = Number(route.params.id)
const loading = ref(false)
const profile = ref(null)
const products = ref([])
const shop = ref(null)

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

const startChat = async () => {
  try {
    await userApi.publicProfile(sellerId)
    router.push({ path: '/chat', query: { peerId: String(sellerId) } })
  } catch {
    ElMessage.error('目标用户不存在或不可聊天')
  }
}

onMounted(() => {
  loadAll().catch(() => {})
})
</script>

<style scoped>
.seller-page {
  display: grid;
  gap: 14px;
}

.seller-hero {
  background:
    linear-gradient(142deg, rgba(70, 95, 255, 0.9), rgba(65, 147, 255, 0.88)),
    radial-gradient(circle at 88% 8%, rgba(255, 255, 255, 0.28), transparent 38%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.hero-left {
  display: flex;
  gap: 14px;
  align-items: center;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-row h2 {
  margin: 0;
  font-size: 24px;
  color: #fff;
}

.hero-left p {
  margin: 8px 0 4px;
  color: rgba(255, 255, 255, 0.9);
}

.meta {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header h3 {
  margin: 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.product-card {
  border: 1px solid #e9edf4;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

.cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  background: #f3f6fa;
}

.cover.empty {
  display: grid;
  place-items: center;
  color: #9aa4b2;
  font-size: 14px;
}

.card-body {
  padding: 10px;
}

.card-body h4 {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.4;
  min-height: 42px;
}

.price {
  color: #ff5a2a;
  font-weight: 700;
  font-size: 20px;
}

.desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6f7988;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

@media (max-width: 900px) {
  :deep(.el-card__body) {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
