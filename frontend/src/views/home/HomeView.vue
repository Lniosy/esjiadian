<template>
  <div class="home-page">
    <!-- 主 Banner -->
    <section class="home-banner">
      <div class="banner-content">
        <div class="banner-text">
          <h1>闲置家电，<br>让它继续发光</h1>
          <p>放心买、放心卖，闲家电让二手交易更简单</p>
          <div class="banner-actions">
            <router-link to="/products">
              <el-button type="primary" size="large" round>逛逛市场</el-button>
            </router-link>
            <router-link to="/shop">
              <el-button size="large" round class="banner-btn-outline">发布闲置</el-button>
            </router-link>
          </div>
        </div>
        <div class="banner-illustration">
          <div class="illus-circle c1"></div>
          <div class="illus-circle c2"></div>
          <div class="illus-icon">♻</div>
        </div>
      </div>
    </section>

    <!-- 分类导航 -->
    <section class="category-section">
      <div class="category-grid">
        <button
          v-for="cat in categories"
          :key="cat.label"
          class="category-item"
          @click="goCategory(cat)"
        >
          <div class="cat-icon" :style="{ background: cat.bg }">{{ cat.icon }}</div>
          <span class="cat-label">{{ cat.label }}</span>
        </button>
      </div>
    </section>

    <!-- 最新上架 -->
    <section class="section-block">
      <div class="section-header">
        <h2 class="section-title">最新上架</h2>
        <router-link to="/products" class="section-more">查看更多 →</router-link>
      </div>
      <div v-if="loading" class="products-skeleton">
        <div v-for="i in 8" :key="i" class="skeleton-card"></div>
      </div>
      <div v-else-if="latestProducts.length" class="products-grid">
        <article
          v-for="item in latestProducts"
          :key="item.id"
          class="product-card"
          @click="openProduct(item)"
        >
          <div class="card-img-wrap">
            <img v-if="item.images?.[0]" :src="item.images[0]" :alt="item.title" class="card-img" />
            <div v-else class="card-img no-img">暂无图片</div>
            <div class="card-trade-tag">{{ item.tradeMethods }}</div>
          </div>
          <div class="card-body">
            <p class="card-title">{{ item.title }}</p>
            <div class="card-meta">{{ item.brand }} · {{ item.region }}</div>
            <div class="card-footer">
              <span class="card-price">¥{{ item.price }}</span>
              <button class="card-seller" @click.stop="goSeller(item.sellerId)">
                <el-avatar :size="20" style="font-size:10px;background:var(--c-primary);">
                  {{ (item.sellerShopName || item.sellerNickname || '').slice(0,2) || '卖' }}
                </el-avatar>
                <span>{{ item.sellerShopName || item.sellerNickname || `用户${item.sellerId}` }}</span>
              </button>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无商品，快去发布你的闲置吧" />
    </section>

    <!-- 商品详情 Drawer（复用） -->
    <el-drawer v-model="detailVisible" direction="rtl" size="460px" :title="detailItem?.title || '商品详情'">
      <div v-if="detailItem" class="detail-body">
        <el-carousel height="240px" v-if="(detailItem.images || []).length" arrow="always">
          <el-carousel-item v-for="(src, i) in detailItem.images" :key="i">
            <img :src="src" class="detail-img" />
          </el-carousel-item>
        </el-carousel>
        <div v-else class="detail-no-img">暂无图片</div>
        <div class="detail-price-row">
          <span class="detail-price">¥{{ detailItem.price }}</span>
          <el-tag size="small">已售 {{ detailItem.salesCount || 0 }}</el-tag>
        </div>
        <el-descriptions :column="2" border size="small" class="detail-specs">
          <el-descriptions-item label="品牌">{{ detailItem.brand || '-' }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ detailItem.model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ detailItem.condition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地区">{{ detailItem.region || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易方式" :span="2">{{ detailItem.tradeMethods || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detailItem.description" class="detail-desc-text">
          <div class="detail-label">商品描述</div>
          <p>{{ detailItem.description }}</p>
        </div>
        <div class="detail-seller-row">
          <el-avatar :size="36" style="background:var(--c-primary);font-size:14px;">
            {{ (detailItem.sellerShopName || detailItem.sellerNickname || '').slice(0,2) || '卖' }}
          </el-avatar>
          <div>
            <div class="detail-seller-name">{{ detailItem.sellerShopName || detailItem.sellerNickname || `卖家${detailItem.sellerId}` }}</div>
            <el-button text type="primary" size="small" @click="goSeller(detailItem.sellerId); detailVisible = false">
              进入店铺 →
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <div style="display:flex;gap:10px;">
          <el-button style="flex:1" @click="addToCart(detailItem.id)">加入购物车</el-button>
          <el-button type="primary" style="flex:1" @click="goSeller(detailItem.sellerId); detailVisible = false">进店咨询</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi, productApi } from '../../api/modules'

const router = useRouter()
const latestProducts = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const detailItem = ref(null)

const categories = [
  { icon: '📺', label: '电视', bg: '#FFF3E0', keyword: '电视' },
  { icon: '❄️', label: '冰箱', bg: '#E3F2FD', keyword: '冰箱' },
  { icon: '🌀', label: '洗衣机', bg: '#E8F5E9', keyword: '洗衣机' },
  { icon: '💨', label: '空调', bg: '#E0F7FA', keyword: '空调' },
  { icon: '🖥️', label: '电脑', bg: '#EDE7F6', keyword: '电脑' },
  { icon: '📱', label: '手机', bg: '#FCE4EC', keyword: '手机' },
  { icon: '🎵', label: '音响', bg: '#F3E5F5', keyword: '音响' },
  { icon: '🔌', label: '其他', bg: '#FFF9C4', keyword: '' }
]

const goCategory = (cat) => {
  router.push({ path: '/products', query: cat.keyword ? { keyword: cat.keyword } : {} })
}

const goSeller = (sellerId) => {
  router.push(`/users/${sellerId}`)
}

const openProduct = (item) => {
  detailItem.value = item
  detailVisible.value = true
}

const addToCart = async (productId) => {
  await orderApi.addCart(productId)
  ElMessage.success('已加入购物车')
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await productApi.list({ pageNum: 1, pageSize: 12, sortBy: 'LATEST' })
    latestProducts.value = res.list || []
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 16px;
}

/* ---- Banner ---- */
.home-banner {
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #FF5722 0%, #FF8A65 60%, #FFAB91 100%);
  padding: 32px 36px;
  color: #fff;
  overflow: hidden;
  position: relative;
  box-shadow: 0 12px 32px rgba(255, 87, 34, 0.25);
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.banner-text h1 {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 900;
  line-height: 1.3;
  letter-spacing: -0.5px;
}

.banner-text p {
  margin: 0 0 20px;
  font-size: 14px;
  opacity: 0.9;
  line-height: 1.7;
}

.banner-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.banner-actions .el-button--primary {
  background: #fff !important;
  color: var(--c-primary) !important;
  border-color: transparent !important;
  font-weight: 700;
  box-shadow: 0 4px 14px rgba(0,0,0,0.12);
}

.banner-actions .el-button--primary:hover {
  box-shadow: 0 6px 18px rgba(0,0,0,0.16) !important;
  background: #fff !important;
}

/* 发布闲置按钮：白色边框 + 半透明底，确保在橙色背景上可见 */
.banner-btn-outline {
  color: #fff !important;
  border-color: rgba(255,255,255,0.7) !important;
  background: rgba(255,255,255,0.15) !important;
  font-weight: 600;
  backdrop-filter: blur(4px);
}
.banner-btn-outline:hover {
  background: rgba(255,255,255,0.28) !important;
  border-color: #fff !important;
  box-shadow: 0 4px 14px rgba(0,0,0,0.12);
}

.banner-illustration {
  position: relative;
  width: 140px;
  height: 120px;
  flex-shrink: 0;
}

.illus-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.25;
}
.c1 { width: 100px; height: 100px; background: #fff; top: 10px; right: 0; }
.c2 { width: 60px; height: 60px; background: #fff; bottom: 0; left: 10px; }

.illus-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 52px;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.15));
}

/* ---- 分类 ---- */
.category-section {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 18px 20px;
  border: 1px solid var(--c-border-soft);
  box-shadow: var(--shadow-card);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 10px 6px;
  border-radius: var(--radius-sm);
  transition: background 0.16s;
}

.category-item:hover { background: var(--c-primary-bg); }

.cat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 22px;
  transition: transform 0.2s;
}

.category-item:hover .cat-icon { transform: scale(1.1); }

.cat-label {
  font-size: 12px;
  color: var(--c-text-secondary);
  font-weight: 500;
  white-space: nowrap;
}

/* ---- 内容区 ---- */
.section-block {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 18px 20px;
  border: 1px solid var(--c-border-soft);
  box-shadow: var(--shadow-card);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.section-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.section-more {
  font-size: 13px;
  color: var(--c-primary);
  text-decoration: none;
  font-weight: 500;
}

.section-more:hover { text-decoration: underline; }

/* ---- 商品网格 ---- */
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.product-card {
  border: 1px solid var(--c-border-soft);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.card-img-wrap { position: relative; }

.card-img {
  width: 100%;
  height: 170px;
  object-fit: cover;
  display: block;
  background: #f5f5f5;
}

.no-img {
  height: 170px;
  display: grid;
  place-items: center;
  background: #f5f5f5;
  color: var(--c-text-tertiary);
  font-size: 12px;
}

.card-trade-tag {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0,0,0,0.55);
  color: #fff;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: var(--radius-full);
  backdrop-filter: blur(4px);
}

.card-body { padding: 10px 12px 12px; }

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

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 6px;
}

.card-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-primary);
}

.card-seller {
  display: flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  font-size: 11px;
  color: var(--c-text-secondary);
  max-width: 80px;
  overflow: hidden;
}

.card-seller span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Skeleton */
.products-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.skeleton-card {
  height: 260px;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* Detail Drawer */
.detail-body { display: grid; gap: 14px; }

.detail-img {
  width: 100%;
  height: 240px;
  object-fit: contain;
  background: #f5f5f5;
  display: block;
}

.detail-no-img {
  height: 160px;
  display: grid;
  place-items: center;
  background: #f5f5f5;
  border-radius: var(--radius-sm);
  color: var(--c-text-tertiary);
}

.detail-price-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-price {
  font-size: 26px;
  font-weight: 700;
  color: var(--c-primary);
}

.detail-specs { margin-top: 2px; }

.detail-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-tertiary);
  text-transform: uppercase;
  margin-bottom: 6px;
  letter-spacing: 0.05em;
}

.detail-desc-text p {
  margin: 0;
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-seller-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border-soft);
}

.detail-seller-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--c-text-primary);
}

/* 响应式 */
@media (max-width: 900px) {
  .banner-text h1 { font-size: 22px; }
  .banner-illustration { display: none; }
  .category-grid { grid-template-columns: repeat(4, 1fr); }
  .products-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 480px) {
  .home-banner { padding: 20px; }
  .category-grid { grid-template-columns: repeat(4, 1fr); }
}
</style>
