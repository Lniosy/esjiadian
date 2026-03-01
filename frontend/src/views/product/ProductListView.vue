<template>
  <div class="page-stack">
    <section class="hero-panel market-hero">
      <h1 class="hero-title">闲逛二手家电</h1>
      <p class="hero-subtitle">按品牌、价格和交易方式快速筛选，点击卖家头像进入主页发起会话。</p>
    </section>

    <el-card class="feed-card page-card">
      <template #header>
        <div class="head-row">
          <h3>市场筛选</h3>
          <el-button size="small" @click="loadProducts">刷新</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="filters.keyword" placeholder="搜品牌、型号、关键词" style="width:220px;" />
        <el-input-number v-model="filters.minPrice" :min="0" :step="50" placeholder="最低价" />
        <el-input-number v-model="filters.maxPrice" :min="0" :step="50" placeholder="最高价" />
        <el-select v-model="filters.tradeMethod" placeholder="交易方式" style="width:140px;">
          <el-option label="全部" value="" />
          <el-option label="同城自提" value="同城自提" />
          <el-option label="快递邮寄" value="快递邮寄" />
        </el-select>
        <el-select v-model="filters.sortBy" placeholder="排序" style="width:140px;">
          <el-option label="综合" value="COMPREHENSIVE" />
          <el-option label="最新" value="LATEST" />
          <el-option label="价格↑" value="PRICE_ASC" />
          <el-option label="价格↓" value="PRICE_DESC" />
        </el-select>
        <el-button type="primary" @click="loadProducts">筛选</el-button>
      </div>

      <div class="kpi-grid" style="margin-bottom: 12px;">
        <article class="kpi-item">
          <div class="kpi-label">在售商品</div>
          <div class="kpi-value">{{ products.list?.length || 0 }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">排序方式</div>
          <div class="kpi-value">{{ sortText }}</div>
        </article>
      </div>

      <div v-loading="loading" class="feed-grid" v-if="(products.list || []).length">
        <article class="item" v-for="item in products.list" :key="item.id">
          <img v-if="item.images?.[0]" :src="item.images[0]" class="cover" alt="商品图片" />
          <div class="cover empty" v-else>暂无图片</div>

          <div class="content">
            <h4>{{ item.title }}</h4>
            <div class="price-row">
              <span class="price">¥{{ item.price }}</span>
              <span class="sales">已售 {{ item.salesCount || 0 }}</span>
            </div>
            <div class="meta">{{ item.brand }} {{ item.model }} · {{ item.region }}</div>

            <div class="seller-row">
              <button class="seller-btn" @click="goSeller(item.sellerId)">
                <el-avatar :size="26" :src="item.sellerAvatarUrl || ''">{{ avatarText(item.sellerNickname) }}</el-avatar>
                <span>{{ item.sellerNickname || `用户${item.sellerId}` }}</span>
              </button>
              <div class="trade">{{ item.tradeMethods }}</div>
            </div>

            <div class="actions">
              <el-button size="small" @click="addToCart(item.id)">加入购物车</el-button>
              <el-button size="small" type="primary" @click="goSeller(item.sellerId)">进店看看</el-button>
            </div>
          </div>
        </article>
      </div>

      <el-empty v-else description="暂无符合条件的商品" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi, productApi } from '../../api/modules'

const router = useRouter()
const products = ref({ list: [] })
const loading = ref(false)
const filters = reactive({
  keyword: '',
  minPrice: null,
  maxPrice: null,
  tradeMethod: '',
  sortBy: 'COMPREHENSIVE'
})

const sortText = computed(() => {
  const map = {
    COMPREHENSIVE: '综合',
    LATEST: '最新',
    PRICE_ASC: '价格↑',
    PRICE_DESC: '价格↓'
  }
  return map[filters.sortBy] || '综合'
})

const loadProducts = async () => {
  loading.value = true
  try {
    products.value = await productApi.list({
      pageNum: 1,
      pageSize: 50,
      keyword: filters.keyword || null,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      tradeMethod: filters.tradeMethod || null,
      sortBy: filters.sortBy
    })
  } finally {
    loading.value = false
  }
}

const addToCart = async (productId) => {
  await orderApi.addCart(productId)
  ElMessage.success('已加入购物车')
}

const goSeller = (sellerId) => {
  router.push(`/users/${sellerId}`)
}

const avatarText = (name) => (name || '卖家').slice(0, 2)

onMounted(() => {
  loadProducts().catch(() => {})
})
</script>

<style scoped>
.feed-card {
  border-radius: var(--radius-md);
}

.market-hero {
  background:
    linear-gradient(135deg, rgba(11, 120, 94, 0.92), rgba(19, 168, 134, 0.88)),
    radial-gradient(circle at 90% 10%, rgba(255, 255, 255, 0.24), transparent 34%);
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head-row h3 {
  margin: 0;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.item {
  border: 1px solid #e8edf3;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.item:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(25, 52, 86, 0.12);
}

.cover {
  width: 100%;
  height: 186px;
  object-fit: cover;
  display: block;
  background: #f1f5f9;
}

.cover.empty {
  display: grid;
  place-items: center;
  color: #9aa4b2;
  font-size: 14px;
}

.content {
  padding: 10px;
}

.content h4 {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.4;
  min-height: 42px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #ff5a2a;
  font-size: 22px;
  font-weight: 700;
}

.sales {
  color: #8a94a3;
  font-size: 12px;
}

.meta {
  margin-top: 6px;
  color: #6f7988;
  font-size: 12px;
}

.seller-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.seller-btn {
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  cursor: pointer;
  color: #1f2937;
}

.trade {
  color: #5878ff;
  font-size: 12px;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
</style>
