<template>
  <div style="display:grid;gap:16px;">
    <el-card>
      <template #header><h3 style="margin:0;">地址管理</h3></template>
      <el-form inline>
        <el-form-item label="收件人"><el-input v-model="addressForm.receiverName" style="width:120px" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="addressForm.receiverPhone" style="width:140px" /></el-form-item>
        <el-form-item label="省"><el-input v-model="addressForm.province" style="width:100px" /></el-form-item>
        <el-form-item label="市"><el-input v-model="addressForm.city" style="width:100px" /></el-form-item>
        <el-form-item label="区"><el-input v-model="addressForm.district" style="width:100px" /></el-form-item>
        <el-form-item label="详细"><el-input v-model="addressForm.detailAddress" style="width:220px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="createAddress">新增地址</el-button></el-form-item>
      </el-form>
      <el-radio-group v-model="selectedAddressId">
        <el-radio v-for="a in addresses" :key="a.id" :label="a.id">
          {{ a.receiverName }} {{ a.receiverPhone }} {{ a.province }}{{ a.city }}{{ a.district }}{{ a.detailAddress }}
        </el-radio>
      </el-radio-group>
    </el-card>

    <el-card>
      <template #header><h3 style="margin:0;">购物车创建订单</h3></template>
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
            <el-button size="small" type="primary" :disabled="!scope.row.valid" @click="createOrder(scope.row.productId)">创建订单</el-button>
            <el-button size="small" @click="removeCart(scope.row.productId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3 style="margin:0;">订单列表</h3>
          <div>
            <el-select v-model="orderView" style="width:130px;margin-right:8px;">
              <el-option label="买家视图" value="buyer" />
              <el-option label="卖家视图" value="seller" />
              <el-option label="全部" value="all" />
            </el-select>
            <el-button @click="loadOrders">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="orders">
        <el-table-column prop="id" label="订单编号" width="90" />
        <el-table-column prop="productId" label="商品编号" width="90" />
        <el-table-column label="状态" width="170">
          <template #default="scope">
            <el-tag>{{ orderStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款状态" width="140">
          <template #default="scope">
            {{ refundStatusText(refundMap[scope.row.id]?.status) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="900">
          <template #default="scope">
            <el-button size="small" @click="cancel(scope.row)" v-if="scope.row.status === 'PENDING_PAYMENT'">取消</el-button>
            <el-button size="small" type="success" @click="pay(scope.row, 'alipay')" v-if="scope.row.status === 'PENDING_PAYMENT'">支付宝支付</el-button>
            <el-button size="small" type="success" @click="pay(scope.row, 'wechat')" v-if="scope.row.status === 'PENDING_PAYMENT'">微信支付</el-button>
            <el-button size="small" @click="ship(scope.row)" v-if="scope.row.status === 'PENDING_SHIPMENT'">发货</el-button>
            <el-button size="small" @click="confirmReceipt(scope.row)" v-if="scope.row.status === 'PENDING_RECEIPT'">确认收货</el-button>
            <el-button size="small" @click="evaluate(scope.row)" v-if="scope.row.status === 'PENDING_REVIEW'">评价</el-button>
            <el-button size="small" @click="showTracks(scope.row)">物流轨迹</el-button>
            <el-button
              size="small"
              type="warning"
              plain
              :disabled="hasOpenDispute(scope.row.id)"
              v-if="canCreateDispute(scope.row)"
              @click="goCreateDispute(scope.row)"
            >
              {{ hasOpenDispute(scope.row.id) ? '纠纷处理中' : '发起纠纷' }}
            </el-button>
            <el-button size="small" type="warning" @click="applyRefund(scope.row)" v-if="scope.row.status === 'PENDING_SHIPMENT' || scope.row.status === 'PENDING_RECEIPT'">申请退款</el-button>
            <el-button size="small" type="success" @click="approveRefund(scope.row)" v-if="orderView === 'seller' && refundMap[scope.row.id]?.status === 'PENDING'">同意退款</el-button>
            <el-button size="small" type="danger" @click="rejectRefund(scope.row)" v-if="orderView === 'seller' && refundMap[scope.row.id]?.status === 'PENDING'">拒绝退款</el-button>
            <el-button size="small" type="primary" @click="shipBackRefund(scope.row)" v-if="orderView !== 'seller' && refundMap[scope.row.id]?.status === 'RETURN_REQUIRED'">填写退货物流</el-button>
            <el-button size="small" type="success" @click="confirmReturnRefund(scope.row)" v-if="orderView === 'seller' && refundMap[scope.row.id]?.status === 'BUYER_SHIPPED'">确认收到退货</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <input ref="evalFileInput" type="file" multiple accept="image/*" style="display:none;" @change="onEvalFilesChange" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { disputeApi, evaluationApi, fileApi, logisticsApi, orderApi, paymentApi, refundApi, userApi } from '../../api/modules'
import { logisticsStatusText, orderStatusText, refundStatusText } from '../../utils/display'

const router = useRouter()
const addresses = ref([])
const selectedAddressId = ref(null)
const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: true
})

const cartProducts = ref([])
const orders = ref([])
const orderView = ref('buyer')
const refundMap = reactive({})
const disputeMap = reactive({})
const evalFileInput = ref(null)
const evalOrderRow = ref(null)

const loadAddresses = async () => {
  addresses.value = await userApi.addresses()
  if (!selectedAddressId.value && addresses.value.length) {
    selectedAddressId.value = addresses.value[0].id
  }
}

const createAddress = async () => {
  await userApi.addAddress(addressForm)
  ElMessage.success('地址已新增')
  await loadAddresses()
}

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
    ElMessage.error('请先选择地址')
    return
  }
  await orderApi.createOrder({
    productId,
    addressId: selectedAddressId.value,
    tradeMethod: '快递邮寄',
    buyerNote: '请尽快发货'
  })
  ElMessage.success('订单已创建')
  await loadCart()
  await loadOrders()
}

const loadRefunds = async () => {
  const pairs = await Promise.all(orders.value.map(async (o) => {
    const r = await refundApi.detail(o.id).catch(() => null)
    return [o.id, r]
  }))
  Object.keys(refundMap).forEach((k) => { delete refundMap[k] })
  pairs.forEach(([id, data]) => { refundMap[id] = data })
}

const loadOrders = async () => {
  orders.value = await orderApi.listOrders(orderView.value)
  await loadRefunds()
  await loadDisputes()
}

const loadDisputes = async () => {
  const all = await disputeApi.myList().catch(() => [])
  Object.keys(disputeMap).forEach((k) => { delete disputeMap[k] })
  all.forEach((d) => {
    const key = Number(d.orderId)
    if (!key) return
    disputeMap[key] = d
  })
}

const hasOpenDispute = (orderId) => ['OPEN', 'PENDING', 'PROCESSING'].includes(String(disputeMap[Number(orderId)]?.status || '').toUpperCase())

const canCreateDispute = (row) => !['PENDING_PAYMENT', 'CANCELLED', 'CLOSED'].includes(String(row?.status || '').toUpperCase())

const goCreateDispute = (row) => {
  router.push({ path: '/disputes', query: { orderId: String(row.id) } })
}

const pay = async (row, channel) => {
  const payInfo = channel === 'alipay' ? await paymentApi.alipay(row.id) : await paymentApi.wechat(row.id)
  await paymentApi.callback(channel, { orderId: row.id, outTradeNo: payInfo.outTradeNo, success: true })
  ElMessage.success('支付回调完成，订单已更新')
  await loadOrders()
}

const cancel = async (row) => {
  await orderApi.cancel(row.id)
  ElMessage.success('订单已取消')
  await loadOrders()
}

const ship = async (row) => {
  const no = `SF${Date.now()}`
  await orderApi.ship(row.id, { logisticsCompany: 'SF', trackingNo: no, shipNote: '正常发货' })
  await logisticsApi.save(row.id, { companyCode: 'SF', trackingNo: no, shipNote: '正常发货' })
  ElMessage.success('发货成功')
  await loadOrders()
}

const confirmReceipt = async (row) => {
  await orderApi.confirmReceipt(row.id)
  ElMessage.success('已确认收货')
  await loadOrders()
}

const evaluate = async (row) => {
  evalOrderRow.value = row
  evalFileInput.value?.click()
}

const onEvalFilesChange = async (e) => {
  const row = evalOrderRow.value
  evalOrderRow.value = null
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (!row) return
  const images = []
  for (const file of files.slice(0, 6)) {
    const uploaded = await fileApi.uploadImage(file)
    images.push(uploaded.url)
  }
  await evaluationApi.create(row.id, {
    productId: row.productId,
    score: 5,
    content: '商品符合描述，交易顺利',
    images,
    tags: ['成色好', '发货快']
  })
  ElMessage.success('评价成功')
  await loadOrders()
}

const showTracks = async (row) => {
  const track = await logisticsApi.tracks(row.id)
  ElMessage.info(`物流状态：${logisticsStatusText(track.status)}，最新轨迹：${track.latestTrack || '暂无'}`)
}

const applyRefund = async (row) => {
  await refundApi.apply(row.id, { reason: '商品与描述不符，申请退款' })
  ElMessage.success('退款申请已提交')
  await loadOrders()
}

const approveRefund = async (row) => {
  await refundApi.approve(row.id)
  ElMessage.success('已同意退款')
  await loadOrders()
}

const rejectRefund = async (row) => {
  await refundApi.reject(row.id, { rejectReason: '证据不足，暂不支持退款' })
  ElMessage.success('已拒绝退款')
  await loadOrders()
}

const shipBackRefund = async (row) => {
  try {
    const company = await ElMessageBox.prompt('请输入退货物流公司编码（如 SF）', '退货物流', { inputValue: 'SF' })
    const tracking = await ElMessageBox.prompt('请输入退货运单号', '退货物流', { inputValue: `RET${Date.now()}` })
    await refundApi.shipReturn(row.id, {
      companyCode: company.value,
      trackingNo: tracking.value,
      note: '买家已寄回'
    })
    ElMessage.success('已提交退货物流')
    await loadOrders()
  } catch {
    // 用户取消输入时无需提示错误
  }
}

const confirmReturnRefund = async (row) => {
  await refundApi.confirmReturn(row.id)
  ElMessage.success('已确认收货并完成退款')
  await loadOrders()
}

onMounted(async () => {
  try {
    await loadAddresses()
    await loadCart()
    await loadOrders()
  } catch {}
})
</script>
