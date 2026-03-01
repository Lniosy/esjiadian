<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>购物车快速下单</h3>
          <div class="meta">当前地址编号：{{ selectedAddressId || '未设置' }}</div>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon>
        请先在“地址管理”设置默认地址，再在这里创建订单。
      </el-alert>
      <!-- 下单选项 -->
      <div class="order-options">
        <div class="option-row">
          <span class="option-label">交易方式</span>
          <el-radio-group v-model="orderOptions.tradeMethod">
            <el-radio label="快递邮寄">快递邮寄</el-radio>
            <el-radio label="同城自提">同城自提</el-radio>
          </el-radio-group>
        </div>
        <div class="option-row">
          <span class="option-label">买家备注</span>
          <el-input v-model="orderOptions.buyerNote" placeholder="如：请尽快发货（可不填）" style="flex:1;" maxlength="100" />
        </div>
      </div>
      <div class="kpi-grid" style="margin-top: 12px;">
        <article class="kpi-item">
          <div class="kpi-label">购物车商品</div>
          <div class="kpi-value">{{ cartProducts.length }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">可下单商品</div>
          <div class="kpi-value">{{ validCount }}</div>
        </article>
      </div>
    </el-card>

    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>购物车商品</h3>
          <el-button @click="loadCart">刷新</el-button>
        </div>
      </template>

      <el-table empty-text="暂无数据" :data="cartProducts">
        <el-table-column prop="productId" label="商品编号" width="100" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="price" label="价格" width="100" />
        <el-table-column label="状态" width="180">
          <template #default="scope">
            <el-tag :type="scope.row.valid ? 'success' : 'danger'">
              {{ scope.row.valid ? '可下单' : (scope.row.invalidReason || '不可下单') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="primary" :disabled="!scope.row.valid" @click="createOrder(scope.row.productId)">创建订单</el-button>
              <el-button size="small" @click="removeCart(scope.row.productId)">移除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi } from '../../api/modules'

const cartProducts = ref([])
const selectedAddressId = computed(() => Number(localStorage.getItem('selectedAddressId') || 0) || null)
const validCount = computed(() => cartProducts.value.filter((i) => i.valid).length)
const orderOptions = reactive({ tradeMethod: '快递邮寄', buyerNote: '' })

const loadCart = async () => {
  cartProducts.value = await orderApi.cartList()
}

const removeCart = async (productId) => {
  await orderApi.removeCart(productId)
  ElMessage.success('已移除')
  await loadCart()
}

const createOrder = async (productId) => {
  if (!selectedAddressId.value) {
    ElMessage.error('请先到地址管理设置下单地址')
    return
  }
  await orderApi.createOrder({
    productId,
    addressId: selectedAddressId.value,
    tradeMethod: orderOptions.tradeMethod,
    buyerNote: orderOptions.buyerNote || ''
  })
  ElMessage.success('订单已创建')
  await loadCart()
}

onMounted(() => {
  loadCart().catch(() => {})
})
</script>

<style scoped>
.meta {
  color: #6f7c8c;
  font-size: 13px;
}

.order-options {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-label {
  width: 70px;
  font-size: 13px;
  color: #637286;
  flex-shrink: 0;
}
</style>
