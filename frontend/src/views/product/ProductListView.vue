<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h3 style="margin:0;">商品列表</h3>
        <el-button size="small" @click="loadProducts">刷新</el-button>
      </div>
    </template>

    <div style="display:flex;gap:8px;align-items:center;margin-bottom:10px;flex-wrap:wrap;">
      <el-input v-model="filters.keyword" placeholder="关键词" style="width:180px;" />
      <el-input-number v-model="filters.minPrice" :min="0" :step="50" placeholder="最低价" />
      <el-input-number v-model="filters.maxPrice" :min="0" :step="50" placeholder="最高价" />
      <el-select v-model="filters.tradeMethod" placeholder="交易方式" style="width:140px;">
        <el-option label="全部" value="" />
        <el-option label="同城自提" value="同城自提" />
        <el-option label="快递邮寄" value="快递邮寄" />
      </el-select>
      <el-select v-model="filters.sortBy" placeholder="排序" style="width:160px;">
        <el-option label="综合排序" value="COMPREHENSIVE" />
        <el-option label="发布时间" value="LATEST" />
        <el-option label="价格升序" value="PRICE_ASC" />
        <el-option label="价格降序" value="PRICE_DESC" />
        <el-option label="销量优先" value="SALES_DESC" />
      </el-select>
      <el-button type="primary" @click="loadProducts">筛选</el-button>
    </div>

    <el-table :data="products.list || []" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column prop="salesCount" label="销量" width="90" />
      <el-table-column prop="status" label="状态" width="160" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button size="small" @click="addToCart(scope.row.id)">加入购物车</el-button>
          <el-button size="small" type="success" v-if="scope.row.status !== 'ON_SHELF'" @click="onShelf(scope.row.id)">上架</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi, productApi } from '../../api/modules'

const products = ref({ list: [] })
const loading = ref(false)
const filters = reactive({
  keyword: '',
  minPrice: null,
  maxPrice: null,
  tradeMethod: '',
  sortBy: 'COMPREHENSIVE'
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

const onShelf = async (id) => {
  await productApi.onShelf(id)
  ElMessage.success('上架成功')
  await loadProducts()
}

onMounted(loadProducts)
</script>
