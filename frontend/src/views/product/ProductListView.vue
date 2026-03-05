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
          style="max-width:300px;"
        >
          <template #prefix>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </template>
        </el-input>

        <div class="price-range">
          <el-input-number v-model="filters.minPrice" :min="0" :step="50" placeholder="最低价" controls-position="right" style="width:110px;" />
          <span class="price-sep">—</span>
          <el-input-number v-model="filters.maxPrice" :min="0" :step="50" placeholder="最高价" controls-position="right" style="width:110px;" />
        </div>

        <el-cascader
          v-model="filters.categoryPath"
          :options="categoriesTree"
          :props="{ label: 'name', value: 'id', children: 'children', checkStrictly: true }"
          placeholder="全部分类"
          clearable
          style="width: 160px;"
          @change="handleCategoryChange"
        />

        <el-input v-model="filters.region" placeholder="所在地区" clearable style="width: 120px;" />

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
          <span class="chips-label">方式:</span>
          <button
            v-for="opt in tradeMethods"
            :key="opt.value"
            class="chip"
            :class="{ active: filters.tradeMethod === opt.value }"
            @click="setTrade(opt.value)"
          >{{ opt.label }}</button>
        </div>

        <div class="chips-divider"></div>

        <!-- 成色 -->
        <div class="chips-group">
          <span class="chips-label">成色:</span>
          <button
            v-for="opt in conditionOptions"
            :key="opt.value"
            class="chip"
            :class="{ active: filters.conditionLevel === opt.value }"
            @click="setCondition(opt.value)"
          >{{ opt.label }}</button>
        </div>

        <div class="chips-divider"></div>

        <!-- 功能状态 -->
        <div class="chips-group">
          <span class="chips-label">功能:</span>
          <button
            v-for="opt in functionOptions"
            :key="opt.value"
            class="chip"
            :class="{ active: filters.functionStatus === opt.value }"
            @click="setFunction(opt.value)"
          >{{ opt.label }}</button>
        </div>

        <div class="chips-divider"></div>

        <!-- 排序 -->
        <div class="chips-group">
          <span class="chips-label">排序:</span>
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
            <button class="fav-btn" :class="{ active: isFavorited(item.id) }" @click.stop="toggleFavorite(item)">
              <el-icon><StarFilled v-if="isFavorited(item.id)" /><Star v-else /></el-icon>
            </button>
          </div>
          <div class="card-body">
            <p class="card-title">{{ item.title }}</p>
            <div class="card-meta">{{ item.brand }} {{ item.model }} · {{ item.region }}</div>
            <div class="card-bottom">
              <span class="card-price">¥{{ item.price }}</span>
              <span class="card-sold">已售{{ item.salesCount || 0 }}</span>
            </div>
            <button class="card-seller" @click.stop="goSeller(item.sellerId)">
              <el-avatar
                :size="18"
                :src="item.sellerShopLogo || item.sellerAvatarUrl"
                style="font-size:10px;background:var(--c-primary);flex-shrink:0;"
              >
                {{ avatarText(item.sellerShopName || item.sellerNickname) }}
              </el-avatar>
              <span class="seller-name">{{ item.sellerShopName || item.sellerNickname || `用户${item.sellerId}` }}</span>
              <span class="seller-score">⭐{{ (item.sellerScore || 5.0).toFixed(1) }}</span>
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

      <!-- 视频展示 -->
      <div v-if="detailDrawer.item.videoUrl" class="detail-video-wrap">
        <div class="detail-sec-title">视频展示</div>
        <video :src="detailDrawer.item.videoUrl" controls class="detail-video"></video>
      </div>

      <div class="detail-price-row">
        <span class="detail-price">¥{{ detailDrawer.item.price }}</span>
        <el-tag size="small" type="info">已售 {{ detailDrawer.item.salesCount || 0 }}</el-tag>
        <el-button 
          :type="isFavorited(detailDrawer.item.id) ? 'warning' : ''" 
          :icon="isFavorited(detailDrawer.item.id) ? 'StarFilled' : 'Star'"
          circle
          style="margin-left:auto"
          @click="toggleFavorite(detailDrawer.item)"
        />
      </div>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="品牌">{{ detailDrawer.item.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detailDrawer.item.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成色">{{ detailDrawer.item.conditionLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="功能状况">{{ detailDrawer.item.functionStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地区">{{ detailDrawer.item.region || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易方式" :span="2">{{ detailDrawer.item.tradeMethods || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detailDrawer.item.description" class="detail-section">
        <div class="detail-sec-title">商品描述</div>
        <p class="detail-text">{{ detailDrawer.item.description }}</p>
      </div>

      <div class="detail-seller">
        <el-avatar
          :size="36"
          :src="detailDrawer.item.sellerShopLogo || detailDrawer.item.sellerAvatarUrl"
          style="background:var(--c-primary);font-size:14px;"
        >
          {{ avatarText(detailDrawer.item.sellerShopName || detailDrawer.item.sellerNickname) }}
        </el-avatar>
        <div>
          <div class="detail-seller-header">
            <span class="detail-seller-name">{{ detailDrawer.item.sellerShopName || detailDrawer.item.sellerNickname || `用户${detailDrawer.item.sellerId}` }}</span>
            <el-tag v-if="detailDrawer.item.sellerAuthStatus === 'APPROVED'" size="small" type="success" effect="plain" class="auth-tag">已认证</el-tag>
            <el-tag v-else-if="detailDrawer.item.sellerAuthStatus === 'PENDING'" size="small" type="warning" effect="plain" class="auth-tag">审核中</el-tag>
            <el-tag v-else size="small" type="info" effect="plain" class="auth-tag">未认证</el-tag>
          </div>
          <div class="detail-seller-score" v-if="detailDrawer.item.sellerScore">信誉分: {{ detailDrawer.item.sellerScore.toFixed(1) }}</div>
          <el-button text type="primary" size="small" @click="goSeller(detailDrawer.item.sellerId); detailDrawer.visible = false">
            进入店铺 →
          </el-button>
        </div>
      </div>

      <!-- 相关推荐 -->
      <div class="related-section">
        <div class="detail-sec-title">相关推荐</div>
        <div v-loading="relatedLoading" class="related-grid">
          <div
            v-for="rel in relatedProducts"
            :key="rel.id"
            class="related-card"
            @click="switchDetail(rel)"
          >
            <div class="related-img-wrap">
              <img :src="rel.images?.[0]" class="related-img" />
              <div class="recommend-reason" v-if="getRecommendReason(rel)">{{ getRecommendReason(rel) }}</div>
            </div>
            <div class="related-info">
              <div class="related-title">{{ rel.title }}</div>
              <div class="related-price">¥{{ rel.price }}</div>
            </div>
          </div>
          <el-empty v-if="!relatedLoading && !relatedProducts.length" :image-size="40" description="暂无推荐" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer-btns">
        <el-button @click="addToCart(detailDrawer.item.id)">加入购物车</el-button>
        <el-button type="primary" plain @click="goSeller(detailDrawer.item.sellerId); detailDrawer.visible = false">进店咨询</el-button>
        <el-button type="primary" @click="buyNow(detailDrawer.item.id)">立即购买</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { orderApi, productApi, recommendApi, favoriteApi } from '../../api/modules'

const router = useRouter()
const route = useRoute()
const productList = ref([])
const loading = ref(false)
const detailDrawer = reactive({ visible: false, item: null })
const relatedProducts = ref([])
const relatedLoading = ref(false)
const categoriesTree = ref([])
const reportedExposures = new Map()
const favoriteIds = ref(new Set())

const reportExpose = (productId) => {
  if (!productId) return
  const now = Date.now()
  const last = reportedExposures.get(productId) || 0
  if (now - last < 10000) return
  reportedExposures.set(productId, now)
  recommendApi.recordEvent({
    productId,
    eventType: 'RECOMMEND_EXPOSE',
    eventScore: 0.4
  }).catch(() => {})
}

const filters = reactive({
  keyword: '',
  minPrice: null,
  maxPrice: null,
  categoryPath: [],
  categoryId: null,
  region: '',
  tradeMethod: '',
  conditionLevel: '',
  functionStatus: '',
  sortBy: 'COMPREHENSIVE'
})

const tradeMethods = [
  { label: '全部', value: '' },
  { label: '快递', value: '快递邮寄' },
  { label: '自提', value: '同城自提' }
]

const conditionOptions = [
  { label: '全部', value: '' },
  { label: '全新', value: 'NEW' },
  { label: '九成新', value: 'LIKE_NEW' },
  { label: '七成新', value: 'GOOD' },
  { label: '一般', value: 'FAIR' }
]

const functionOptions = [
  { label: '全部', value: '' },
  { label: '功能完好', value: 'FULLY_FUNCTIONAL' },
  { label: '轻微问题', value: 'MINOR_ISSUE' },
  { label: '需要维修', value: 'NEEDS_REPAIR' }
]

const sortOptions = [
  { label: '综合', value: 'COMPREHENSIVE' },
  { label: '最新', value: 'LATEST' },
  { label: '销量', value: 'SALES_DESC' },
  { label: '价格↑', value: 'PRICE_ASC' },
  { label: '价格↓', value: 'PRICE_DESC' }
]

const setTrade = (v) => { filters.tradeMethod = v; loadProducts() }
const setCondition = (v) => { filters.conditionLevel = v; loadProducts() }
const setFunction = (v) => { filters.functionStatus = v; loadProducts() }
const setSort = (v) => { filters.sortBy = v; loadProducts() }

const handleCategoryChange = (val) => {
  filters.categoryId = Array.isArray(val) && val.length > 0 ? val[val.length - 1] : null
  loadProducts()
  // 上报分类筛选行为 (带上 categoryId)
  recommendApi.recordEvent({
    productId: null,
    categoryId: filters.categoryId,
    eventType: 'CATEGORY_FILTER',
    eventScore: 0.8
  }).catch(() => {})
}

const findCategoryPath = (tree, targetId, path = []) => {
  if (!tree) return null
  for (const node of tree) {
    if (node.id === targetId) return [...path, node.id]
    if (node.children && node.children.length > 0) {
      const found = findCategoryPath(node.children, targetId, [...path, node.id])
      if (found) return found
    }
  }
  return null
}

const syncFiltersFromQuery = () => {
  const { keyword, categoryId } = route.query
  let changed = false

  const k = keyword === undefined ? '' : String(keyword)
  if (filters.keyword !== k) {
    filters.keyword = k
    changed = true
  }

  const cid = categoryId === undefined ? null : (Number(categoryId) || null)
  if (filters.categoryId !== cid) {
    filters.categoryId = cid
    if (cid && categoriesTree.value.length) {
      filters.categoryPath = findCategoryPath(categoriesTree.value, cid) || []
    } else {
      filters.categoryPath = []
    }
    changed = true
  }

  return changed
}

const fetchCategories = async () => {
  try {
    categoriesTree.value = await productApi.categoryTree()
    if (route.query.categoryId) {
      const cid = Number(route.query.categoryId)
      filters.categoryPath = findCategoryPath(categoriesTree.value, cid) || []
    }
  } catch {
    categoriesTree.value = []
  }
}

const fetchFavorites = async () => {
  try {
    const ids = await favoriteApi.ids()
    favoriteIds.value = new Set(ids || [])
  } catch {
    // ignore
  }
}

const isFavorited = (id) => favoriteIds.value.has(id)

const toggleFavorite = async (item) => {
  const id = item.id
  try {
    if (isFavorited(id)) {
      await favoriteApi.remove(id)
      favoriteIds.value.delete(id)
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.add(id)
      favoriteIds.value.add(id)
      ElMessage.success('已加入收藏')
    }
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.minPrice = null
  filters.maxPrice = null
  filters.categoryPath = []
  filters.categoryId = null
  filters.region = ''
  filters.tradeMethod = ''
  filters.conditionLevel = ''
  filters.functionStatus = ''
  filters.sortBy = 'COMPREHENSIVE'
  loadProducts()
}

const openDetail = (item) => {
  detailDrawer.item = item
  detailDrawer.visible = true
  loadRelated(item.id)
}

const loadRelated = async (productId) => {
  relatedLoading.value = true
  try {
    relatedProducts.value = await recommendApi.related(productId)
  } catch {
    relatedProducts.value = []
  } finally {
    relatedLoading.value = false
  }
}

const switchDetail = (item) => {
  detailDrawer.item = item
  loadRelated(item.id)
  // 上报点击推荐行为
  recommendApi.recordEvent({
    productId: item.id,
    eventType: 'RECOMMEND_CLICK',
    eventScore: 1.2
  }).catch(() => {})
}

const getRecommendReason = (rel) => {
  if (!detailDrawer.item) return ''
  const current = detailDrawer.item
  if (rel.sellerId === current.sellerId) return '同店好物'
  if (rel.brand && rel.brand === current.brand) return '同品牌推荐'
  if (rel.categoryId && rel.categoryId === current.categoryId) return '同类热门'
  if (Math.abs(rel.price - current.price) < 200) return '价位临近'
  return '猜你喜欢'
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
      categoryId: filters.categoryId,
      region: filters.region || null,
      tradeMethod: filters.tradeMethod || null,
      conditionLevel: filters.conditionLevel || null,
      functionStatus: filters.functionStatus || null,
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

const buyNow = async (productId) => {
  try {
    await orderApi.addCart(productId)
    detailDrawer.visible = false
    router.push({ path: '/orders/workbench', query: { tab: 'checkout' } })
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const goSeller = (sellerId) => { router.push(`/users/${sellerId}`) }
const avatarText = (name) => (name || '卖').slice(0, 2)

// 响应来自首页/导航栏的查询参数变化
watch(() => route.query, () => {
  if (syncFiltersFromQuery()) {
    loadProducts()
  }
}, { deep: true })

// 监听相关推荐加载，做曝光上报
watch(relatedProducts, (list) => {
  if (list && list.length > 0) {
    list.forEach(p => reportExpose(p.id))
  }
}, { immediate: true })

onMounted(async () => {
  await fetchCategories()
  fetchFavorites()
  syncFiltersFromQuery()
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
  gap: 12px;
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
  padding: 0 10px;
  border: 1.5px solid var(--c-border);
  border-radius: var(--radius-full);
  background: transparent;
  font-size: 11px;
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
  height: 16px;
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

.fav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255,255,255,0.8);
  border: none;
  display: grid;
  place-items: center;
  cursor: pointer;
  color: var(--c-text-tertiary);
  transition: all 0.2s;
}
.fav-btn:hover { transform: scale(1.1); background: #fff; }
.fav-btn.active { color: #ff9800; background: #fff; }

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

.card-seller .seller-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.seller-score {
  color: #faad14;
  font-weight: 700;
  margin-left: 2px;
  flex-shrink: 0;
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

.detail-video-wrap {
  margin-top: 4px;
}
.detail-sec-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-tertiary);
  margin-bottom: 6px;
  text-transform: uppercase;
}
.detail-video {
  width: 100%;
  max-height: 240px;
  background: #000;
  border-radius: var(--radius-sm);
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

.detail-seller-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.detail-seller-name {
  font-weight: 600;
  color: var(--c-text-primary);
}

.auth-tag { font-size: 10px; height: 18px; padding: 0 4px; line-height: 16px; }

.detail-seller-score { font-size: 12px; color: #faad14; font-weight: 600; margin-bottom: 4px; }

/* Drawer Footer */
.drawer-footer-btns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 10px;
  width: 100%;
}
.drawer-footer-btns .el-button {
  margin: 0 !important;
}

.related-section {
  border-top: 1px solid var(--c-border-soft);
  padding-top: 16px;
  margin-top: 8px;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 10px;
}

.related-card {
  border: 1px solid var(--c-border-soft);
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
}

.related-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.related-img-wrap {
  position: relative;
  width: 100%;
  height: 100px;
}

.related-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #f5f5f5;
}

.recommend-reason {
  position: absolute;
  top: 4px;
  left: 4px;
  background: rgba(var(--c-primary-rgb), 0.85);
  color: #fff;
  font-size: 9px;
  padding: 1px 5px;
  border-radius: 4px;
  backdrop-filter: blur(2px);
}

.related-info {
  padding: 6px 8px;
}

.related-title {
  font-size: 11px;
  color: var(--c-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.related-price {
  font-size: 13px;
  font-weight: 700;
  color: var(--c-primary);
}

/* 响应式 */
@media (max-width: 760px) {
  .market-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .filter-search-row { gap: 8px; }
  .price-range { display: none; }
}
</style>
