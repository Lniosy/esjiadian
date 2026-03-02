<template>
  <div class="market-page">

    <!-- 顶部筛选栏 -->
    <div class="filter-bar page-card">
      <!-- 搜索行 -->
      <div class="filter-search-row">
        <el-input
          v-model="filters.keyword"
          placeholder="搜品牌、型号、关键词…"
          clearable
          @keydown.enter="loadProducts"
          style="max-width:340px;"
        >
          <template #prefix>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </template>
        </el-input>

        <div class="price-range">
          <el-input-number v-model="filters.minPrice" :min="0" :step="50" placeholder="最低价" controls-position="right" style="width:120px;" />
          <span class="price-sep">—</span>
          <el-input-number v-model="filters.maxPrice" :min="0" :step="50" placeholder="最高价" controls-position="right" style="width:120px;" />
        </div>

        <div class="filter-actions">
          <el-button type="primary" @click="loadProducts">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>

        <span class="result-count">共 {{ productList.length }} 件</span>
      </div>

      <!-- 筛选 Chips 行 -->
      <div class="filter-chips-row">
        <!-- 交易方式 -->
        <div class="chips-group">
          <span class="chips-label">方式</span>
          <button
            v-for="opt in tradeMethods"
            :key="opt.value"
            class="chip"
            :class="{ active: filters.tradeMethod === opt.value }"
            @click="setTrade(opt.value)"
          >{{ opt.label }}</button>
        </div>

        <div class="chips-divider"></div>

        <!-- 排序 -->
        <div class="chips-group">
          <span class="chips-label">排序</span>
          <button
            v-for="opt in sortOptions"
            :key="opt.value"
            class="chip"
            :class="{ active: filters.sortBy === opt.value }"
            @click="setSort(opt.value)"
          >{{ opt.label }}</button>
        </div>
      </div>
    </div>

    <!-- 商品网格 -->
    <div v-loading="loading" class="market-grid">
      <template v-if="productList.length">
        <article
          v-for="item in productList"
          :key="item.id"
          class="market-card"
          @click="openDetail(item)"
        >
          <div class="card-img-wrap">
            <img v-if="item.images?.[0]" :src="item.images[0]" :alt="item.title" class="card-img" loading="lazy" />
            <div v-else class="card-img no-img">暂无图片</div>
            <span class="trade-badge">{{ item.tradeMethods }}</span>
          </div>
          <div class="card-body">
            <p class="card-title">{{ item.title }}</p>
            <div class="card-meta">{{ item.brand }} {{ item.model }} · {{ item.region }}</div>
            <div class="card-bottom">
              <span class="card-price">¥{{ item.price }}</span>
              <span class="card-sold">已售{{ item.salesCount || 0 }}</span>
            </div>
            <button class="card-seller" @click.stop="goSeller(item.sellerId)">
              <el-avatar :size="18" style="font-size:10px;background:var(--c-primary);flex-shrink:0;">
                {{ avatarText(item.sellerShopName || item.sellerNickname) }}
              </el-avatar>
              <span>{{ item.sellerShopName || item.sellerNickname || `用户${item.sellerId}` }}</span>
            </button>
          </div>
        </article>
      </template>
      <div v-else-if="!loading" class="market-empty">
        <el-empty description="暂无符合条件的商品" />
      </div>
    </div>
  </div>

  <!-- 商品详情 Drawer -->
  <el-drawer v-model="detailDrawer.visible" direction="rtl" size="480px" :title="detailDrawer.item?.title || '商品详情'">
    <div v-if="detailDrawer.item" class="detail-body">
      <el-carousel height="256px" v-if="(detailDrawer.item.images || []).length" arrow="always">
        <el-carousel-item v-for="(src, i) in detailDrawer.item.images" :key="i">
          <img :src="src" alt="商品图片" class="detail-img" />
        </el-carousel-item>
      </el-carousel>
      <div v-else class="detail-img-empty">暂无图片</div>

      <div class="detail-price-row">
        <span class="detail-price">¥{{ detailDrawer.item.price }}</span>
        <el-tag size="small" type="info">已售 {{ detailDrawer.item.salesCount || 0 }}</el-tag>
      </div>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="品牌">{{ detailDrawer.item.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detailDrawer.item.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成色">{{ detailDrawer.item.condition || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地区">{{ detailDrawer.item.region || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易方式" :span="2">{{ detailDrawer.item.tradeMethods || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detailDrawer.item.description" class="detail-section">
        <div class="detail-sec-title">商品描述</div>
        <p class="detail-text">{{ detailDrawer.item.description }}</p>
      </div>

      <div class="detail-seller">
        <el-avatar :size="36" style="background:var(--c-primary);font-size:14px;">
          {{ avatarText(detailDrawer.item.sellerShopName || detailDrawer.item.sellerNickname) }}
        </el-avatar>
        <div>
          <div class="detail-seller-name">{{ detailDrawer.item.sellerShopName || detailDrawer.item.sellerNickname || `用户${detailDrawer.item.sellerId}` }}</div>
          <el-button text type="primary" size="small" @click="goSeller(detailDrawer.item.sellerId); detailDrawer.visible = false">
            进入店铺 →
          </el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <div style="display:flex;gap:10px;">
        <el-button style="flex:1" @click="addToCart(detailDrawer.item.id)">加入购物车</el-button>
        <el-button type="primary" style="flex:1" @click="goSeller(detailDrawer.item.sellerId); detailDrawer.visible = false">进店咨询</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi, productApi } from '../../api/modules'

const router = useRouter()
const route = useRoute()
const productList = ref([])
const loading = ref(false)
const detailDrawer = reactive({ visible: false, item: null })

const filters = reactive({
  keyword: '',
  minPrice: null,
  maxPrice: null,
  tradeMethod: '',
  sortBy: 'COMPREHENSIVE'
})

const tradeMethods = [
  { label: '全部', value: '' },
  { label: '快递邮寄', value: '快递邮寄' },
  { label: '同城自提', value: '同城自提' }
]

const sortOptions = [
  { label: '综合', value: 'COMPREHENSIVE' },
  { label: '最新', value: 'LATEST' },
  { label: '价格↑', value: 'PRICE_ASC' },
  { label: '价格↓', value: 'PRICE_DESC' }
]

const setTrade = (v) => { filters.tradeMethod = v; loadProducts() }
const setSort = (v) => { filters.sortBy = v; loadProducts() }

const resetFilters = () => {
  filters.keyword = ''
  filters.minPrice = null
  filters.maxPrice = null
  filters.tradeMethod = ''
  filters.sortBy = 'COMPREHENSIVE'
  loadProducts()
}

const openDetail = (item) => {
  detailDrawer.item = item
  detailDrawer.visible = true
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await productApi.list({
      pageNum: 1,
      pageSize: 60,
      keyword: filters.keyword || null,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      tradeMethod: filters.tradeMethod || null,
      sortBy: filters.sortBy
    })
    productList.value = res.list || []
  } finally {
    loading.value = false
  }
}

const addToCart = async (productId) => {
  await orderApi.addCart(productId)
  ElMessage.success('已加入购物车')
}

const goSeller = (sellerId) => { router.push(`/users/${sellerId}`) }
const avatarText = (name) => (name || '卖').slice(0, 2)

// 响应来自首页/导航栏的 keyword 查询参数
watch(() => route.query.keyword, (kw) => {
  if (kw !== undefined) {
    filters.keyword = String(kw || '')
    loadProducts()
  }
}, { immediate: false })

onMounted(() => {
  if (route.query.keyword) filters.keyword = String(route.query.keyword)
  loadProducts().catch(() => {})
})
</script>

<style scoped>
.market-page {
  display: grid;
  gap: 12px;
}

/* ---- 筛选栏 ---- */
.filter-bar {
  padding: 14px 18px 10px !important;
  display: grid;
  gap: 12px;
}

.filter-search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-sep {
  color: var(--c-text-tertiary);
  font-size: 13px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.result-count {
  font-size: 13px;
  color: var(--c-text-tertiary);
  margin-left: auto;
  flex-shrink: 0;
}

/* chips */
.filter-chips-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.chips-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.chips-label {
  font-size: 12px;
  color: var(--c-text-tertiary);
  white-space: nowrap;
}

.chip {
  height: 28px;
  padding: 0 12px;
  border: 1.5px solid var(--c-border);
  border-radius: var(--radius-full);
  background: transparent;
  font-size: 12px;
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: all 0.16s;
  font-weight: 500;
}

.chip:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
}

.chip.active {
  background: var(--c-primary-bg);
  border-color: var(--c-primary);
  color: var(--c-primary);
  font-weight: 700;
}

.chips-divider {
  width: 1px;
  height: 20px;
  background: var(--c-border);
  flex-shrink: 0;
}

/* ---- 商品网格 ---- */
.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
  min-height: 200px;
}

.market-empty {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* 商品卡片 */
.market-card {
  background: #fff;
  border: 1px solid var(--c-border-soft);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.market-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.card-img-wrap { position: relative; }

.card-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
  background: #f5f5f5;
}

.no-img {
  height: 180px;
  display: grid;
  place-items: center;
  background: #f5f5f5;
  color: var(--c-text-tertiary);
  font-size: 12px;
}

.trade-badge {
  position: absolute;
  bottom: 7px;
  left: 7px;
  background: rgba(0,0,0,0.52);
  color: #fff;
  font-size: 10px;
  padding: 2px 7px;
  border-radius: var(--radius-full);
}

.card-body {
  padding: 10px 12px 12px;
}

.card-title {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--c-text-primary);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}

.card-meta {
  font-size: 11px;
  color: var(--c-text-tertiary);
  margin-bottom: 8px;
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 7px;
}

.card-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-primary);
}

.card-sold {
  font-size: 11px;
  color: var(--c-text-tertiary);
}

.card-seller {
  display: flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 4px 0 0;
  font-size: 11px;
  color: var(--c-text-secondary);
  width: 100%;
  overflow: hidden;
}

.card-seller span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- Drawer ---- */
.detail-body { display: grid; gap: 14px; }

.detail-img {
  width: 100%;
  height: 256px;
  object-fit: contain;
  background: #f5f5f5;
  display: block;
}

.detail-img-empty {
  height: 160px;
  display: grid;
  place-items: center;
  background: #f5f5f5;
  border-radius: var(--radius-sm);
  color: var(--c-text-tertiary);
}

.detail-price-row { display: flex; align-items: center; gap: 10px; }

.detail-price {
  font-size: 26px;
  font-weight: 700;
  color: var(--c-primary);
}

.detail-section {
  border-top: 1px solid var(--c-border-soft);
  padding-top: 12px;
}

.detail-sec-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-tertiary);
  margin-bottom: 6px;
  text-transform: uppercase;
}

.detail-text {
  margin: 0;
  line-height: 1.8;
  color: var(--c-text-secondary);
  font-size: 13px;
  white-space: pre-wrap;
}

.detail-seller {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border-soft);
}

.detail-seller-name {
  font-weight: 600;
  color: var(--c-text-primary);
  margin-bottom: 2px;
}

/* 响应式 */
@media (max-width: 760px) {
  .market-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .filter-search-row { gap: 8px; }
  .price-range { display: none; }
}
</style>
