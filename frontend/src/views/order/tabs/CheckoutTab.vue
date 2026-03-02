<template>
  <div class="page-stack">
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="page-head">
          <span>地址簿</span>
          <div class="action-group">
            <el-button @click="emit('refresh')">刷新</el-button>
            <el-button type="primary" @click="addressDialogVisible = true">新增地址</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!addresses.length" description="暂无地址，请先新增地址" />
      <div v-else class="address-list">
        <article
          v-for="item in addresses"
          :key="item.id"
          class="address-item"
          :class="{ active: Number(selectedAddressId || 0) === Number(item.id) }"
        >
          <div>
            <div class="name">{{ item.receiverName }} {{ item.receiverPhone }}</div>
            <div class="detail">{{ item.province }}{{ item.city }}{{ item.district }}{{ item.detailAddress }}</div>
          </div>
          <el-button
            size="small"
            :type="Number(selectedAddressId || 0) === Number(item.id) ? 'primary' : 'default'"
            @click="selectAddress(item.id)"
          >
            {{ Number(selectedAddressId || 0) === Number(item.id) ? '当前下单地址' : '设为下单地址' }}
          </el-button>
        </article>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="page-head">
          <span>购物车结算</span>
          <el-tag type="info">{{ selectedAddressLabel }}</el-tag>
        </div>
      </template>

      <el-form inline>
        <el-form-item label="交易方式">
          <el-radio-group v-model="orderOptions.tradeMethod">
            <el-radio label="快递邮寄">快递邮寄</el-radio>
            <el-radio label="同城自提">同城自提</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="买家备注">
          <el-input v-model="orderOptions.buyerNote" style="width: 260px;" maxlength="100" />
        </el-form-item>
      </el-form>

      <el-table :data="cartItems" empty-text="购物车为空">
        <el-table-column prop="productId" label="商品编号" width="100" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="price" label="价格" width="110" />
        <el-table-column label="状态" width="140">
          <template #default="scope">
            <el-tag :type="scope.row.valid ? 'success' : 'danger'">
              {{ scope.row.valid ? '可下单' : (scope.row.invalidReason || '不可下单') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="primary" :disabled="!scope.row.valid" @click="createOrder(scope.row.productId)">
                创建订单
              </el-button>
              <el-button size="small" @click="removeCart(scope.row.productId)">移除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <el-dialog v-model="addressDialogVisible" title="新增收货地址" width="540px">
    <el-form label-width="80px">
      <el-form-item label="收件人"><el-input v-model="addressForm.receiverName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="addressForm.receiverPhone" /></el-form-item>
      <el-form-item label="省"><el-input v-model="addressForm.province" /></el-form-item>
      <el-form-item label="市"><el-input v-model="addressForm.city" /></el-form-item>
      <el-form-item label="区"><el-input v-model="addressForm.district" /></el-form-item>
      <el-form-item label="详细地址"><el-input v-model="addressForm.detailAddress" type="textarea" :rows="2" /></el-form-item>
    </el-form>
    <template #footer>
      <div class="action-group">
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingAddress" :disabled="!canSubmitAddress" @click="createAddress">
          保存地址
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi, userApi } from '../../../api/modules'

const props = defineProps({
  addresses: {
    type: Array,
    default: () => []
  },
  cartItems: {
    type: Array,
    default: () => []
  },
  selectedAddressId: {
    type: [Number, String, null],
    default: null
  }
})

const emit = defineEmits(['refresh', 'update:selected-address-id'])
const addressDialogVisible = ref(false)
const submittingAddress = ref(false)
const orderOptions = reactive({
  tradeMethod: '快递邮寄',
  buyerNote: ''
})
const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: true
})

const selectedAddressLabel = computed(() => {
  const id = Number(props.selectedAddressId || 0)
  if (!id) return '未设置'
  const hit = props.addresses.find((it) => Number(it.id) === id)
  if (!hit) return `#${id}`
  return `${hit.receiverName} ${hit.receiverPhone}`
})

const canSubmitAddress = computed(() => {
  return Boolean(addressForm.receiverName.trim()) &&
    Boolean(addressForm.receiverPhone.trim()) &&
    Boolean(addressForm.province.trim()) &&
    Boolean(addressForm.city.trim()) &&
    Boolean(addressForm.district.trim()) &&
    Boolean(addressForm.detailAddress.trim())
})

const selectAddress = (id) => {
  emit('update:selected-address-id', Number(id))
  ElMessage.success('下单地址已切换')
}

const removeCart = async (productId) => {
  await orderApi.removeCart(productId)
  ElMessage.success('已移除商品')
  emit('refresh')
}

const createOrder = async (productId) => {
  const addressId = Number(props.selectedAddressId || 0)
  if (!addressId) {
    ElMessage.warning('请先选择下单地址')
    return
  }
  await orderApi.createOrder({
    productId,
    addressId,
    tradeMethod: orderOptions.tradeMethod,
    buyerNote: orderOptions.buyerNote || ''
  })
  ElMessage.success('订单已创建')
  emit('refresh')
}

const resetAddressForm = () => {
  addressForm.receiverName = ''
  addressForm.receiverPhone = ''
  addressForm.province = ''
  addressForm.city = ''
  addressForm.district = ''
  addressForm.detailAddress = ''
  addressForm.isDefault = true
}

const createAddress = async () => {
  if (!canSubmitAddress.value) return
  submittingAddress.value = true
  try {
    await userApi.addAddress({ ...addressForm })
    ElMessage.success('地址已新增')
    addressDialogVisible.value = false
    resetAddressForm()
    emit('refresh')
  } finally {
    submittingAddress.value = false
  }
}
</script>

<style scoped>
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

.address-item.active {
  border-color: var(--c-primary);
  background: var(--c-primary-bg);
}

.name {
  color: var(--app-text-strong);
  font-weight: 600;
}

.detail {
  margin-top: 4px;
  color: #627080;
}
</style>
