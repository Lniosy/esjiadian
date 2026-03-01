<template>
  <el-card class="page-card">
    <template #header>
      <div class="page-head">
        <h3>订单流程与售后</h3>
        <div class="actions">
          <el-select v-model="orderView" style="width:130px;">
            <el-option label="买家视图" value="buyer" />
            <el-option label="卖家视图" value="seller" />
            <el-option label="全部" value="all" />
          </el-select>
          <el-button @click="loadOrders">刷新</el-button>
        </div>
      </div>
    </template>

    <div class="kpi-grid" style="margin-bottom: 12px;">
      <article class="kpi-item">
        <div class="kpi-label">订单总数</div>
        <div class="kpi-value">{{ orders.length }}</div>
      </article>
      <article class="kpi-item">
        <div class="kpi-label">待支付</div>
        <div class="kpi-value">{{ statusCount('PENDING_PAYMENT') }}</div>
      </article>
      <article class="kpi-item">
        <div class="kpi-label">待发货</div>
        <div class="kpi-value">{{ statusCount('PENDING_SHIPMENT') }}</div>
      </article>
      <article class="kpi-item">
        <div class="kpi-label">待收货</div>
        <div class="kpi-value">{{ statusCount('PENDING_RECEIPT') }}</div>
      </article>
    </div>

    <el-table empty-text="暂无数据" :data="orders">
      <el-table-column prop="id" label="订单编号" width="90" />
      <el-table-column prop="productId" label="商品编号" width="90" />
      <el-table-column label="订单状态" width="170">
        <template #default="scope">
          <el-tag>{{ orderStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="退款状态" width="140">
        <template #default="scope">
          {{ refundStatusText(refundMap[scope.row.id]?.status) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="380">
        <template #default="scope">
          <div class="action-group">
            <el-button size="small" @click="cancel(scope.row)" v-if="scope.row.status === 'PENDING_PAYMENT'">取消</el-button>
            <el-button size="small" type="success" @click="pay(scope.row, 'alipay')" v-if="scope.row.status === 'PENDING_PAYMENT'">支付宝支付</el-button>
            <el-button size="small" type="success" @click="pay(scope.row, 'wechat')" v-if="scope.row.status === 'PENDING_PAYMENT'">微信支付</el-button>
            <el-button size="small" @click="openShipDialog(scope.row)" v-if="scope.row.status === 'PENDING_SHIPMENT'">发货</el-button>
            <el-button size="small" @click="confirmReceipt(scope.row)" v-if="scope.row.status === 'PENDING_RECEIPT'">确认收货</el-button>
            <el-button size="small" @click="openEvalDialog(scope.row)" v-if="scope.row.status === 'PENDING_REVIEW'">评价</el-button>
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
            <el-button size="small" type="warning" @click="openRefundDialog(scope.row)" v-if="scope.row.status === 'PENDING_SHIPMENT' || scope.row.status === 'PENDING_RECEIPT'">申请退款</el-button>
            <el-button size="small" type="success" @click="approveRefund(scope.row)" v-if="orderView === 'seller' && refundMap[scope.row.id]?.status === 'PENDING'">同意退款</el-button>
            <el-button size="small" type="danger" @click="openRejectDialog(scope.row)" v-if="orderView === 'seller' && refundMap[scope.row.id]?.status === 'PENDING'">拒绝退款</el-button>
            <el-button size="small" type="primary" @click="shipBackRefund(scope.row)" v-if="orderView !== 'seller' && refundMap[scope.row.id]?.status === 'RETURN_REQUIRED'">填写退货物流</el-button>
            <el-button size="small" type="success" @click="confirmReturnRefund(scope.row)" v-if="orderView === 'seller' && refundMap[scope.row.id]?.status === 'BUYER_SHIPPED'">确认收到退货</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 发货弹窗 -->
  <el-dialog v-model="shipDialog.visible" title="填写发货信息" width="420px" :close-on-click-modal="false">
    <el-form label-width="90px">
      <el-form-item label="物流公司">
        <el-select v-model="shipDialog.company" style="width:100%;">
          <el-option label="顺丰速运 (SF)" value="SF" />
          <el-option label="圆通速递 (YTO)" value="YTO" />
          <el-option label="中通快递 (ZTO)" value="ZTO" />
          <el-option label="韵达快递 (YD)" value="YD" />
          <el-option label="申通快递 (STO)" value="STO" />
          <el-option label="极兔速递 (JT)" value="JT" />
        </el-select>
      </el-form-item>
      <el-form-item label="运单号">
        <el-input v-model="shipDialog.trackingNo" placeholder="请输入运单号" clearable />
      </el-form-item>
      <el-form-item label="发货备注">
        <el-input v-model="shipDialog.note" placeholder="可不填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="shipDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="shipDialog.submitting" :disabled="!shipDialog.company || !shipDialog.trackingNo" @click="confirmShip">确认发货</el-button>
    </template>
  </el-dialog>

  <!-- 评价弹窗 -->
  <el-dialog v-model="evalDialog.visible" title="评价此次交易" width="500px" :close-on-click-modal="false" @closed="resetEvalDialog">
    <el-form label-width="80px">
      <el-form-item label="评分">
        <el-rate v-model="evalDialog.score" show-score score-template="{value} 分" />
      </el-form-item>
      <el-form-item label="评价内容">
        <el-input v-model="evalDialog.content" type="textarea" :rows="3" placeholder="分享你的交易体验…" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="评价标签">
        <el-checkbox-group v-model="evalDialog.tags">
          <el-checkbox v-for="tag in evalTagOptions" :key="tag" :label="tag">{{ tag }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="上传图片">
        <el-upload :http-request="onEvalUpload" accept="image/*" :show-file-list="false" :limit="6" :before-upload="() => evalDialog.images.length < 6">
          <el-button size="small">选择图片（已选 {{ evalDialog.images.length }}/6）</el-button>
        </el-upload>
        <div class="eval-images" v-if="evalDialog.images.length">
          <img v-for="(src, i) in evalDialog.images" :key="i" :src="src" class="eval-thumb" />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="evalDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="evalDialog.submitting" :disabled="evalDialog.score === 0" @click="confirmEvaluate">提交评价</el-button>
    </template>
  </el-dialog>

  <!-- 申请退款弹窗 -->
  <el-dialog v-model="refundDialog.visible" title="申请退款" width="420px" :close-on-click-modal="false">
    <el-form label-width="80px">
      <el-form-item label="退款原因">
        <el-select v-model="refundDialog.preset" style="width:100%;" @change="(v) => { if (v !== '__other__') refundDialog.reason = v }">
          <el-option label="商品与描述不符" value="商品与描述不符" />
          <el-option label="收到商品已损坏" value="收到商品已损坏" />
          <el-option label="卖家未按时发货" value="卖家未按时发货" />
          <el-option label="误拍，不想要了" value="误拍，不想要了" />
          <el-option label="其他（自定义）" value="__other__" />
        </el-select>
      </el-form-item>
      <el-form-item label="补充说明" v-if="refundDialog.preset === '__other__'">
        <el-input v-model="refundDialog.reason" type="textarea" :rows="2" placeholder="请说明退款原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="refundDialog.visible = false">取消</el-button>
      <el-button type="primary" @click="confirmApplyRefund">提交申请</el-button>
    </template>
  </el-dialog>

  <!-- 拒绝退款弹窗 -->
  <el-dialog v-model="rejectDialog.visible" title="拒绝退款申请" width="420px" :close-on-click-modal="false">
    <el-form label-width="80px">
      <el-form-item label="拒绝原因">
        <el-input v-model="rejectDialog.reason" type="textarea" :rows="3" placeholder="请告知买家拒绝原因" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectDialog.visible = false">取消</el-button>
      <el-button type="danger" :disabled="!rejectDialog.reason" @click="confirmRejectRefund">确认拒绝</el-button>
    </template>
  </el-dialog>

  <!-- 物流轨迹弹窗 -->
  <el-dialog v-model="trackDialog.visible" :title="'物流轨迹 - 订单 #' + (trackDialog.orderId || '')" width="500px">
    <div v-if="trackDialog.data">
      <el-descriptions :column="2" border size="small" style="margin-bottom:12px;">
        <el-descriptions-item label="物流状态">{{ logisticsStatusText(trackDialog.data.status) }}</el-descriptions-item>
        <el-descriptions-item label="物流公司">{{ trackDialog.data.companyCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运单号" :span="2">{{ trackDialog.data.trackingNo || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="track-timeline">
        <div v-for="(t, i) in (trackDialog.data.tracks || [])" :key="i" class="track-item">
          <div class="track-dot" :class="{ active: i === 0 }"></div>
          <div class="track-content">
            <div class="track-desc">{{ t.description || t }}</div>
            <div class="track-time">{{ t.time || '' }}</div>
          </div>
        </div>
        <div v-if="!(trackDialog.data.tracks || []).length" class="track-empty">
          <span>{{ trackDialog.data.latestTrack || '暂无轨迹信息' }}</span>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无物流信息" />
    <template #footer><el-button @click="trackDialog.visible = false">关闭</el-button></template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { disputeApi, evaluationApi, fileApi, logisticsApi, orderApi, paymentApi, refundApi } from '../../api/modules'
import { logisticsStatusText, orderStatusText, refundStatusText } from '../../utils/display'

const router = useRouter()
const orders = ref([])
const orderView = ref('buyer')
const refundMap = reactive({})
const disputeMap = reactive({})
// 发货对话框
const shipDialog = reactive({ visible: false, row: null, company: 'SF', trackingNo: '', note: '', submitting: false })
// 评价对话框
const evalTagOptions = ['成色好', '发货快', '描述符实', '卖家服务好', '包装精心', '價格实惠']
const evalDialog = reactive({ visible: false, row: null, score: 5, content: '', tags: [], images: [], submitting: false })
// 申请退款对话框
const refundDialog = reactive({ visible: false, row: null, preset: '商品与描述不符', reason: '商品与描述不符' })
// 拒绝退款对话框
const rejectDialog = reactive({ visible: false, row: null, reason: '' })
// 物流轨迹对话框
const trackDialog = reactive({ visible: false, orderId: null, data: null })

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

const statusCount = (status) => orders.value.filter((o) => o.status === status).length

const pay = async (row, channel) => {
  const payInfo = channel === 'alipay' ? await paymentApi.alipay(row.id) : await paymentApi.wechat(row.id)
  await paymentApi.callback(channel, { orderId: row.id, outTradeNo: payInfo.outTradeNo, success: true })
  ElMessage.success('支付回调完成，订单已更新')
  await loadOrders()
}

// 取消订单 - 加二次确认
const cancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确认取消订单 #${row.id}？取消后不可恢复。`, '取消订单', {
      confirmButtonText: '确认取消', cancelButtonText: '再想想',
      confirmButtonClass: 'el-button--danger', type: 'warning'
    })
    await orderApi.cancel(row.id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch { /* 用户点击'再想想' */ }
}

// 发货 - 弹窗输入物流信息
const openShipDialog = (row) => {
  shipDialog.row = row
  shipDialog.company = 'SF'
  shipDialog.trackingNo = ''
  shipDialog.note = ''
  shipDialog.visible = true
}

const confirmShip = async () => {
  if (!shipDialog.company || !shipDialog.trackingNo.trim()) {
    ElMessage.warning('请先选择物流公司并填写运单号')
    return
  }
  shipDialog.submitting = true
  try {
    const no = shipDialog.trackingNo.trim()
    const note = shipDialog.note || '正常发货'
    await orderApi.ship(shipDialog.row.id, { logisticsCompany: shipDialog.company, trackingNo: no, shipNote: note })
    await logisticsApi.save(shipDialog.row.id, { companyCode: shipDialog.company, trackingNo: no, shipNote: note })
    ElMessage.success('发货成功')
    shipDialog.visible = false
    await loadOrders()
  } finally {
    shipDialog.submitting = false
  }
}

// 确认收货 - 加二次确认
const confirmReceipt = async (row) => {
  try {
    await ElMessageBox.confirm('确认已收到商品？此操作不可撤销，确认后将自动到达评价环节。', '确认收货', {
      confirmButtonText: '已收到', cancelButtonText: '再等等', type: 'success'
    })
    await orderApi.confirmReceipt(row.id)
    ElMessage.success('已确认收货')
    await loadOrders()
  } catch { /* 用户点击'再等等' */ }
}

// 评价 - 弹窗输入
const openEvalDialog = (row) => {
  evalDialog.row = row
  evalDialog.score = 5
  evalDialog.content = ''
  evalDialog.tags = []
  evalDialog.images = []
  evalDialog.visible = true
}

const resetEvalDialog = () => {
  evalDialog.row = null
}

const onEvalUpload = async ({ file }) => {
  if (evalDialog.images.length >= 6) { ElMessage.warning('最多上传6张图片'); return false }
  const uploaded = await fileApi.uploadImage(file)
  evalDialog.images.push(uploaded.url)
  return false
}

const confirmEvaluate = async () => {
  if (evalDialog.score === 0) { ElMessage.warning('请选择评分'); return }
  evalDialog.submitting = true
  try {
    await evaluationApi.create(evalDialog.row.id, {
      productId: evalDialog.row.productId,
      score: evalDialog.score,
      content: evalDialog.content || '商品符合描述，交易顺利',
      images: evalDialog.images,
      tags: evalDialog.tags
    })
    ElMessage.success('评价成功')
    evalDialog.visible = false
    await loadOrders()
  } finally {
    evalDialog.submitting = false
  }
}

// 物流轨迹 - 弹窗展示
const showTracks = async (row) => {
  trackDialog.orderId = row.id
  trackDialog.data = null
  trackDialog.visible = true
  try {
    trackDialog.data = await logisticsApi.tracks(row.id)
  } catch {
    trackDialog.data = { status: null, tracks: [], latestTrack: '暂无物流信息' }
  }
}

// 申请退款 - 弹窗选择原因
const openRefundDialog = (row) => {
  refundDialog.row = row
  refundDialog.preset = '商品与描述不符'
  refundDialog.reason = '商品与描述不符'
  refundDialog.visible = true
}

const confirmApplyRefund = async () => {
  const reason = refundDialog.preset === '__other__' ? refundDialog.reason : refundDialog.preset
  if (!reason) { ElMessage.warning('请填写退款原因'); return }
  await refundApi.apply(refundDialog.row.id, { reason })
  ElMessage.success('退款申请已提交')
  refundDialog.visible = false
  await loadOrders()
}

const approveRefund = async (row) => {
  try {
    await ElMessageBox.confirm('确认同意退款申请？同意后将按退款流程处理。', '同意退款', {
      confirmButtonText: '确认同意', cancelButtonText: '再想想', type: 'warning'
    })
    await refundApi.approve(row.id)
    ElMessage.success('已同意退款')
    await loadOrders()
  } catch { /* 用户取消 */ }
}

// 拒绝退款 - 弹窗填写原因
const openRejectDialog = (row) => {
  rejectDialog.row = row
  rejectDialog.reason = ''
  rejectDialog.visible = true
}

const confirmRejectRefund = async () => {
  if (!rejectDialog.reason.trim()) { ElMessage.warning('请填写拒绝原因'); return }
  await refundApi.reject(rejectDialog.row.id, { rejectReason: rejectDialog.reason })
  ElMessage.success('已拒绝退款')
  rejectDialog.visible = false
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

onMounted(() => {
  loadOrders().catch(() => {})
})
</script>

<style scoped>
.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px 0;
}

/* 评价图片预览 */
.eval-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.eval-thumb {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

/* 物流时间轴 */
.track-timeline {
  display: grid;
  gap: 0;
}

.track-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-left: 2px solid #e2e8f0;
  margin-left: 7px;
  padding-left: 16px;
  position: relative;
}

.track-item:last-child {
  border-left-color: transparent;
}

.track-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #c5cdd8;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #c5cdd8;
  position: absolute;
  left: -8px;
  top: 12px;
  flex-shrink: 0;
}

.track-dot.active {
  background: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary);
}

.track-content {
  flex: 1;
  padding-bottom: 4px;
}

.track-desc {
  font-size: 14px;
  color: #1f2937;
  line-height: 1.5;
}

.track-time {
  font-size: 12px;
  color: #8a94a3;
  margin-top: 2px;
}

.track-empty {
  padding: 16px;
  color: #8a94a3;
  font-size: 14px;
  text-align: center;
}
</style>
