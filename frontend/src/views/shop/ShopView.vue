<template>
  <div style="display:grid;gap:16px;">
    <el-card>
      <template #header><h3 style="margin:0;">我的店铺</h3></template>
      <el-form label-width="100px">
        <el-form-item label="店铺名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Logo URL"><el-input v-model="form.logoUrl" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro" type="textarea" /></el-form-item>
        <el-form-item label="类目"><el-input v-model="form.categories" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="地区"><el-input v-model="form.region" /></el-form-item>
        <el-form-item><el-button type="primary" @click="save">保存</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="overview">
      <template #header><h3 style="margin:0;">店铺聚合概览</h3></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="评分">{{ Number(overview.rating || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="评价数">{{ overview.evaluationCount }}</el-descriptions-item>
        <el-descriptions-item label="在售商品数">{{ overview.onShelfProductCount }}</el-descriptions-item>
        <el-descriptions-item label="店铺地区">{{ overview.shop?.region || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="overview">
      <template #header><h3 style="margin:0;">在售商品</h3></template>
      <el-table :data="overview.products || []">
        <el-table-column prop="id" label="商品ID" width="90" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="price" label="价格" width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="图片" width="260">
          <template #default="scope">
            <a v-if="scope.row.image" :href="scope.row.image" target="_blank">{{ scope.row.image }}</a>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="overview">
      <template #header><h3 style="margin:0;">历史评价</h3></template>
      <el-table :data="overview.evaluations || []">
        <el-table-column prop="orderId" label="订单ID" width="90" />
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column prop="score" label="评分" width="80" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="图片" width="220">
          <template #default="scope">
            <span v-if="!(scope.row.images || []).length">-</span>
            <a v-for="(img, idx) in (scope.row.images || [])" :key="idx" :href="img" target="_blank" style="display:block;">图片{{ idx + 1 }}</a>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="ratingDetail">
      <template #header><h3 style="margin:0;">评分分布与标签云</h3></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总评价数">{{ ratingDetail.total }}</el-descriptions-item>
        <el-descriptions-item label="平均分">{{ Number(ratingDetail.rating || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="5星">{{ ratingDetail.distribution?.[5] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="4星">{{ ratingDetail.distribution?.[4] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="3星">{{ ratingDetail.distribution?.[3] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="2星">{{ ratingDetail.distribution?.[2] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="1星">{{ ratingDetail.distribution?.[1] || 0 }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:10px;display:flex;flex-wrap:wrap;gap:8px;">
        <el-tag v-for="t in (ratingDetail.tagCloud || [])" :key="t.tag" type="info">{{ t.tag }} x{{ t.count }}</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluationApi, shopApi } from '../../api/modules'

const form = reactive({ id: null, name: '', logoUrl: '', intro: '', categories: '', region: '' })
const overview = ref(null)
const ratingDetail = ref(null)

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

onMounted(load)
</script>
