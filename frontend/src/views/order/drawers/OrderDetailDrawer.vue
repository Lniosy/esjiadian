<template>
  <el-drawer v-model="visible" title="订单详情" size="46%">
    <el-empty v-if="!order" description="请先选择订单" />
    <template v-else>
      <el-alert type="info" :closable="false" show-icon>
        当前订单：#{{ order.id }}，视图：{{ viewModeLabel }}
      </el-alert>

      <el-tabs v-model="activeTab" style="margin-top: 12px;">
        <el-tab-pane label="基础信息" name="basic">
          <el-card shadow="never">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="订单编号">{{ detailOrder?.id || order.id }}</el-descriptions-item>
              <el-descriptions-item label="订单号">{{ detailOrder?.orderNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="商品编号">{{ detailOrder?.productId || order.productId }}</el-descriptions-item>
              <el-descriptions-item label="订单状态">
                <el-tag>{{ orderStatusText(detailOrder?.status || order.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="买家">{{ buyerName || `用户${detailOrder?.buyerId || order.buyerId || '-'}` }}</el-descriptions-item>
              <el-descriptions-item label="卖家">{{ sellerName || `用户${detailOrder?.sellerId || order.sellerId || '-'}` }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="物流轨迹" name="logistics">
          <el-card shadow="never" v-loading="loadingLogistics">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="物流状态">
                {{ logisticsStatusText(logisticsDetail?.status) }}
              </el-descriptions-item>
              <el-descriptions-item label="物流公司">
                {{ logisticsDetail?.companyCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="运单号" :span="2">
                {{ logisticsDetail?.trackingNo || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="最新轨迹" :span="2">
                {{ logisticsDetail?.latestTrack || '暂无轨迹' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="track-timeline" style="margin-top: 12px;">
              <div v-if="trackItems.length">
                <div v-for="(item, idx) in trackItems" :key="`${idx}-${item.description}`" class="track-item">
                  <div class="track-dot" :class="{ active: idx === 0 }"></div>
                  <div class="track-content">
                    <div class="track-desc">{{ item.description }}</div>
                    <div class="track-time">{{ item.time || '' }}</div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无轨迹明细" :image-size="72" />
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="售后进度" name="afterSales">
          <el-card shadow="never" v-loading="loadingAfterSales">
            <el-descriptions :column="2" border size="small" style="margin-bottom: 12px;">
              <el-descriptions-item label="退款状态">
                <el-tag>{{ refundStatusText(refundDetail?.status || 'NONE') }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="投诉状态">
                <el-tag :type="orderDispute ? 'warning' : 'info'">
                  {{ orderDispute ? disputeStatusText(orderDispute.status) : '无' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="退款原因" :span="2">
                {{ refundDetail?.reason || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="投诉原因" :span="2">
                {{ orderDispute?.reason || '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <el-steps :active="refundStep" finish-status="success" align-center>
              <el-step title="发起申请" />
              <el-step title="平台处理中" />
              <el-step title="退货阶段" />
              <el-step title="售后完成" />
            </el-steps>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #footer>
      <div class="action-group">
        <el-button @click="visible = false">关闭</el-button>
        <el-button :loading="loadingAny" @click="refreshAll">刷新详情</el-button>
        <el-button type="primary" @click="emitAction('shipment')">去发货</el-button>
        <el-button type="warning" @click="emitAction('refund')">去退款</el-button>
        <el-button type="danger" @click="emitAction('dispute')">去投诉</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { disputeApi, logisticsApi, orderApi, refundApi, userApi, shopApi } from '../../../api/modules'
import { disputeStatusText, logisticsStatusText, orderStatusText, refundStatusText } from '../../../utils/display'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: null
  },
  viewMode: {
    type: String,
    default: 'buyer'
  }
})

const emit = defineEmits(['update:modelValue', 'open-drawer'])

const activeTab = ref('basic')
const detailOrder = ref(null)
const logisticsDetail = ref(null)
const refundDetail = ref(null)
const disputes = ref([])
const buyerName = ref('')
const sellerName = ref('')

const loadingOrder = ref(false)
const loadingLogistics = ref(false)
const loadingAfterSales = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const viewModeLabel = computed(() => {
  if (props.viewMode === 'seller') return '卖家视图'
  if (props.viewMode === 'all') return '全部视图'
  return '买家视图'
})

const orderDispute = computed(() => {
  const orderId = Number(props.order?.id || 0)
  if (!orderId) return null
  return disputes.value.find((it) => Number(it.orderId) === orderId) || null
})

const trackItems = computed(() => {
  const list = Array.isArray(logisticsDetail.value?.tracks) ? logisticsDetail.value.tracks : []
  if (list.length) return list
  const fallback = String(logisticsDetail.value?.latestTrack || '').trim()
  if (!fallback) return []
  return [{ description: fallback, time: '', status: logisticsDetail.value?.status || '' }]
})

const refundStep = computed(() => {
  const status = String(refundDetail.value?.status || 'NONE').toUpperCase()
  if (status === 'NONE') return 0
  if (['PENDING', 'APPROVED', 'REJECTED'].includes(status)) return 1
  if (['RETURN_REQUIRED', 'BUYER_SHIPPED'].includes(status)) return 2
  if (['FINISHED', 'CANCELED'].includes(status)) return 3
  return 1
})

const loadingAny = computed(() => loadingOrder.value || loadingLogistics.value || loadingAfterSales.value)

const refreshAll = async () => {
  if (!props.order?.id) return

  // 解析买家/卖家名称（优先店铺名）
  const resolvedOrder = detailOrder.value || props.order
  if (resolvedOrder?.buyerId) {
    userApi.publicProfile(resolvedOrder.buyerId)
      .then(p => { buyerName.value = p?.nickname || '' })
      .catch(() => {})
  }
  if (resolvedOrder?.sellerId) {
    Promise.all([
      userApi.publicProfile(resolvedOrder.sellerId).catch(() => null),
      shopApi.byUser(resolvedOrder.sellerId).catch(() => null)
    ]).then(([profile, shop]) => {
      sellerName.value = shop?.name || profile?.nickname || ''
    })
  }

  loadingOrder.value = true
  orderApi.getOrder(props.order.id)
    .then((res) => { detailOrder.value = res || null })
    .catch(() => { detailOrder.value = null })
    .finally(() => { loadingOrder.value = false })

  loadingLogistics.value = true
  logisticsApi.tracks(props.order.id)
    .then((res) => { logisticsDetail.value = res || null })
    .catch(() => { logisticsDetail.value = null })
    .finally(() => { loadingLogistics.value = false })

  loadingAfterSales.value = true
  Promise.all([
    refundApi.detail(props.order.id).catch(() => null),
    disputeApi.myList().catch(() => [])
  ])
    .then(([refundRes, disputeRes]) => {
      refundDetail.value = refundRes
      disputes.value = Array.isArray(disputeRes) ? disputeRes : []
    })
    .finally(() => { loadingAfterSales.value = false })
}

const emitAction = (drawer) => {
  if (!props.order) return
  emit('open-drawer', { drawer, order: props.order })
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    activeTab.value = 'basic'
    refreshAll().catch(() => {})
  }
)
</script>

<style scoped>
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
</style>
