<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>我的店铺配置</h3>
          <el-button type="primary" @click="save">保存信息</el-button>
        </div>
      </template>

      <el-form label-width="100px" class="shop-form">
        <el-form-item label="店铺名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="店铺图标链接"><el-input v-model="form.logoUrl" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="类目"><el-input v-model="form.categories" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="地区"><el-input v-model="form.region" /></el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="overview" class="page-card">
      <template #header><div class="page-head"><h3>店铺经营概览</h3></div></template>
      <div class="kpi-grid">
        <article class="kpi-item">
          <div class="kpi-label">店铺评分</div>
          <div class="kpi-value">{{ Number(overview.rating || 0).toFixed(2) }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">评价总数</div>
          <div class="kpi-value">{{ overview.evaluationCount }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">在售商品</div>
          <div class="kpi-value">{{ overview.onShelfProductCount }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">店铺地区</div>
          <div class="kpi-value">{{ overview.shop?.region || '-' }}</div>
        </article>
      </div>
    </el-card>

    <el-card v-if="overview" class="page-card">
      <template #header><div class="page-head"><h3>在售商品</h3></div></template>
      <el-table empty-text="暂无数据" :data="overview.products || []">
        <el-table-column prop="id" label="商品编号" width="90" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="price" label="价格" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag>{{ productStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="260">
          <template #default="scope">
            <a v-if="scope.row.image" :href="scope.row.image" target="_blank">查看图片</a>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="overview" class="page-card">
      <template #header><div class="page-head"><h3>历史评价</h3></div></template>
      <el-table empty-text="暂无数据" :data="overview.evaluations || []">
        <el-table-column prop="orderId" label="订单编号" width="90" />
        <el-table-column prop="productId" label="商品编号" width="90" />
        <el-table-column prop="score" label="评分" width="80" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="图片" width="160">
          <template #default="scope">
            <span v-if="!(scope.row.images || []).length">-</span>
            <el-tag v-for="(img, idx) in (scope.row.images || [])" :key="idx" class="img-tag">
              <a :href="img" target="_blank">图{{ idx + 1 }}</a>
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="ratingDetail" class="page-card">
      <template #header><div class="page-head"><h3>评分分布与标签</h3></div></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总评价数">{{ ratingDetail.total }}</el-descriptions-item>
        <el-descriptions-item label="平均分">{{ Number(ratingDetail.rating || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="5星">{{ ratingDetail.distribution?.[5] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="4星">{{ ratingDetail.distribution?.[4] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="3星">{{ ratingDetail.distribution?.[3] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="2星">{{ ratingDetail.distribution?.[2] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="1星">{{ ratingDetail.distribution?.[1] || 0 }}</el-descriptions-item>
      </el-descriptions>
      <div class="rating-bars">
        <div class="bar-row" v-for="item in ratingBars" :key="item.star">
          <span class="star-label">{{ item.star }}星</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: `${item.percent}%` }"></div>
          </div>
          <span class="bar-value">{{ item.count }}</span>
        </div>
      </div>
      <div class="tag-wrap">
        <el-tag v-for="t in (ratingDetail.tagCloud || [])" :key="t.tag" type="info">{{ t.tag }} · {{ t.count }}</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluationApi, shopApi } from '../../api/modules'
import { productStatusText } from '../../utils/display'

const form = reactive({ id: null, name: '', logoUrl: '', intro: '', categories: '', region: '' })
const overview = ref(null)
const ratingDetail = ref(null)
const ratingBars = computed(() => {
  const total = Number(ratingDetail.value?.total || 0)
  return [5, 4, 3, 2, 1].map((star) => {
    const count = Number(ratingDetail.value?.distribution?.[star] || 0)
    return {
      star,
      count,
      percent: total > 0 && count > 0 ? Math.max(4, Math.round((count / total) * 100)) : 0
    }
  })
})

const loadOverview = async () => {
  if (!form.id) return
  overview.value = await shopApi.overview(form.id)
  ratingDetail.value = await evaluationApi.shopRatingDetail(form.id).catch(() => null)
}

const load = async () => {
  const data = await shopApi.myShop()
  if (data) {
    Object.assign(form, data)
    await loadOverview()
  }
}

const save = async () => {
  const data = await shopApi.upsertMyShop({
    name: form.name,
    logoUrl: form.logoUrl,
    intro: form.intro,
    categories: form.categories,
    region: form.region
  })
  Object.assign(form, data)
  ElMessage.success('店铺信息已保存')
  await loadOverview()
}

onMounted(() => {
  load().catch(() => {})
})
</script>

<style scoped>
.shop-form {
  max-width: 920px;
}

.img-tag {
  margin-right: 6px;
}

.tag-wrap {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rating-bars {
  margin-top: 12px;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  padding: 10px 12px;
  background: linear-gradient(180deg, #f8fbff, #ffffff);
  display: grid;
  gap: 8px;
}

.bar-row {
  display: grid;
  grid-template-columns: 40px 1fr 30px;
  align-items: center;
  gap: 8px;
}

.star-label,
.bar-value {
  color: #5f6b7a;
  font-size: 12px;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: #edf2f8;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #26b48a, #0b785e);
}
</style>
