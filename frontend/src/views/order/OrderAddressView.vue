<template>
  <el-card class="page-card">
    <template #header>
      <div class="page-head">
        <h3>收货地址管理</h3>
        <el-button @click="loadAddresses">刷新</el-button>
      </div>
    </template>

    <el-form class="address-form" inline>
      <el-form-item label="收件人"><el-input v-model="addressForm.receiverName" style="width:120px" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="addressForm.receiverPhone" style="width:140px" /></el-form-item>
      <el-form-item label="省"><el-input v-model="addressForm.province" style="width:100px" /></el-form-item>
      <el-form-item label="市"><el-input v-model="addressForm.city" style="width:100px" /></el-form-item>
      <el-form-item label="区"><el-input v-model="addressForm.district" style="width:100px" /></el-form-item>
      <el-form-item label="详细"><el-input v-model="addressForm.detailAddress" style="width:220px" /></el-form-item>
      <el-form-item><el-button type="primary" @click="createAddress">新增地址</el-button></el-form-item>
    </el-form>

    <div class="kpi-grid" style="margin-bottom: 12px;">
      <article class="kpi-item">
        <div class="kpi-label">地址总数</div>
        <div class="kpi-value">{{ addresses.length }}</div>
      </article>
      <article class="kpi-item">
        <div class="kpi-label">默认下单地址</div>
        <div class="kpi-value">{{ selectedAddressId || '-' }}</div>
      </article>
    </div>

    <div class="address-list" v-if="addresses.length">
      <article class="address-item" v-for="a in addresses" :key="a.id">
        <div>
          <div class="name">{{ a.receiverName }} {{ a.receiverPhone }}</div>
          <div class="detail">{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detailAddress }}</div>
        </div>
        <el-button size="small" :type="selectedAddressId === a.id ? 'primary' : 'default'" @click="setDefaultAddress(a.id)">
          {{ selectedAddressId === a.id ? '当前下单地址' : '设为下单地址' }}
        </el-button>
      </article>
    </div>
    <el-empty v-else description="暂无地址，请先新增" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../../api/modules'

const addresses = ref([])
const selectedAddressId = ref(Number(localStorage.getItem('selectedAddressId') || 0) || null)
const addressForm = reactive({
  receiverName: '张三',
  receiverPhone: '13800000000',
  province: '上海市',
  city: '上海市',
  district: '浦东新区',
  detailAddress: '测试路100号',
  isDefault: true
})

const loadAddresses = async () => {
  addresses.value = await userApi.addresses()
  if (!selectedAddressId.value && addresses.value.length) {
    setDefaultAddress(addresses.value[0].id)
  }
}

const setDefaultAddress = (id) => {
  selectedAddressId.value = id
  localStorage.setItem('selectedAddressId', String(id))
  ElMessage.success('已设置为下单默认地址')
}

const createAddress = async () => {
  await userApi.addAddress(addressForm)
  ElMessage.success('地址已新增')
  await loadAddresses()
}

onMounted(() => {
  loadAddresses().catch(() => {})
})
</script>

<style scoped>
.address-form {
  margin-bottom: 12px;
}

.address-list {
  display: grid;
  gap: 10px;
}

.address-item {
  border: 1px solid var(--app-border);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.name {
  font-weight: 600;
  color: var(--app-text-strong);
}

.detail {
  color: #627080;
  margin-top: 4px;
}
</style>
