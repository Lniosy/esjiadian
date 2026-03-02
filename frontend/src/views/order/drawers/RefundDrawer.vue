<template>
  <el-drawer v-model="visible" title="退款与退货" size="46%">
    <el-empty v-if="!order" description="请先选择订单" />
    <template v-else>
      <el-alert type="info" :closable="false" show-icon>
        当前订单：#{{ order.id }}，商品 #{{ order.productId }}，退款状态：{{ refundStatusText(currentStatus) }}
      </el-alert>

      <div v-loading="loading" style="margin-top: 14px;">
        <el-card class="inner-card" shadow="never">
          <template #header>
            <div class="section-head">
              <span>售后状态</span>
              <el-tag>{{ refundStatusText(currentStatus) }}</el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="退款原因">{{ refundDetail?.reason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="拒绝原因">{{ refundDetail?.rejectReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="退货物流">
              {{ refundDetail?.returnCompanyCode || '-' }} {{ refundDetail?.returnTrackingNo || '' }}
            </el-descriptions-item>
            <el-descriptions-item label="模式">{{ viewModeLabel }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card v-if="currentStatus === 'NONE' && !isSellerMode" class="inner-card" shadow="never">
          <template #header><span>申请退款</span></template>
          <el-form label-width="92px">
            <el-form-item label="退款原因">
              <el-select
                v-model="applyForm.preset"
                style="width:100%;"
                @change="(v) => { if (v !== '__other__') applyForm.reason = v }"
              >
                <el-option label="商品与描述不符" value="商品与描述不符" />
                <el-option label="收到商品已损坏" value="收到商品已损坏" />
                <el-option label="卖家未按时发货" value="卖家未按时发货" />
                <el-option label="误拍，不想要了" value="误拍，不想要了" />
                <el-option label="其他（自定义）" value="__other__" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="applyForm.preset === '__other__'" label="补充说明">
              <el-input v-model="applyForm.reason" type="textarea" :rows="2" maxlength="120" show-word-limit />
            </el-form-item>
            <el-form-item label="凭证图片">
              <ImageUploader
                v-model="applyForm.images"
                :max="9"
                :allow-external-url="true"
                add-label="上传凭证"
              />
            </el-form-item>
          </el-form>
          <div class="action-group">
            <el-button type="primary" :loading="submitting" :disabled="!applyReason" @click="applyRefund">
              提交退款申请
            </el-button>
          </div>
        </el-card>

        <el-card v-if="currentStatus === 'PENDING' && isSellerMode" class="inner-card" shadow="never">
          <template #header><span>卖家处理</span></template>
          <div class="action-group" style="margin-bottom: 10px;">
            <el-button type="success" :loading="submitting" @click="approveRefund">同意退款</el-button>
          </div>
          <el-form label-width="88px">
            <el-form-item label="拒绝原因">
              <el-input v-model="rejectReason" type="textarea" :rows="2" maxlength="120" show-word-limit />
            </el-form-item>
          </el-form>
          <div class="action-group">
            <el-button type="danger" :loading="submitting" :disabled="!rejectReason.trim()" @click="rejectRefund">
              拒绝退款
            </el-button>
          </div>
        </el-card>

        <el-card v-if="currentStatus === 'RETURN_REQUIRED' && !isSellerMode" class="inner-card" shadow="never">
          <template #header><span>填写退货物流</span></template>
          <el-form label-width="88px">
            <el-form-item label="物流公司">
              <el-input v-model="returnForm.companyCode" placeholder="如 SF" />
            </el-form-item>
            <el-form-item label="运单号">
              <el-input v-model="returnForm.trackingNo" placeholder="请输入退货运单号" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="returnForm.note" type="textarea" :rows="2" maxlength="120" show-word-limit />
            </el-form-item>
          </el-form>
          <div class="action-group">
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!returnForm.companyCode.trim() || !returnForm.trackingNo.trim()"
              @click="shipReturn"
            >
              提交退货物流
            </el-button>
          </div>
        </el-card>

        <el-card v-if="currentStatus === 'BUYER_SHIPPED' && isSellerMode" class="inner-card" shadow="never">
          <template #header><span>确认收货</span></template>
          <div class="action-group">
            <el-button type="success" :loading="submitting" @click="confirmReturn">确认收到退货并完成退款</el-button>
          </div>
        </el-card>
      </div>
    </template>

    <template #footer>
      <div class="action-group">
        <el-button @click="visible = false">关闭</el-button>
        <el-button :loading="loading" @click="loadDetail">刷新状态</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { refundApi } from '../../../api/modules'
import ImageUploader from '../../../components/common/ImageUploader.vue'
import { refundStatusText } from '../../../utils/display'

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

const emit = defineEmits(['update:modelValue', 'success'])

const loading = ref(false)
const submitting = ref(false)
const refundDetail = ref(null)
const rejectReason = ref('')
const returnForm = reactive({
  companyCode: 'SF',
  trackingNo: '',
  note: '买家已寄回'
})
const applyForm = reactive({
  preset: '商品与描述不符',
  reason: '商品与描述不符',
  images: []
})

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const currentStatus = computed(() => String(refundDetail.value?.status || 'NONE').toUpperCase())
const applyReason = computed(() => {
  const reason = applyForm.preset === '__other__' ? applyForm.reason : applyForm.preset
  return String(reason || '').trim()
})
const isSellerMode = computed(() => props.viewMode === 'seller')

const viewModeLabel = computed(() => {
  if (props.viewMode === 'seller') return '卖家视图'
  if (props.viewMode === 'all') return '全部视图'
  return '买家视图'
})

const resetForms = () => {
  applyForm.preset = '商品与描述不符'
  applyForm.reason = '商品与描述不符'
  applyForm.images = []
  rejectReason.value = ''
  returnForm.companyCode = 'SF'
  returnForm.trackingNo = ''
  returnForm.note = '买家已寄回'
}

const loadDetail = async () => {
  if (!props.order?.id) {
    refundDetail.value = null
    return
  }
  loading.value = true
  try {
    refundDetail.value = await refundApi.detail(props.order.id)
  } catch {
    refundDetail.value = null
  } finally {
    loading.value = false
  }
}

const applyRefund = async () => {
  if (!props.order?.id) return
  if (!applyReason.value) {
    ElMessage.warning('请填写退款原因')
    return
  }
  submitting.value = true
  try {
    await refundApi.apply(props.order.id, {
      reason: applyReason.value,
      images: applyForm.images
    })
    ElMessage.success('退款申请已提交')
    await loadDetail()
    emit('success')
  } finally {
    submitting.value = false
  }
}

const approveRefund = async () => {
  if (!props.order?.id) return
  submitting.value = true
  try {
    await refundApi.approve(props.order.id)
    ElMessage.success('已同意退款')
    await loadDetail()
    emit('success')
  } finally {
    submitting.value = false
  }
}

const rejectRefund = async () => {
  if (!props.order?.id) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  submitting.value = true
  try {
    await refundApi.reject(props.order.id, { rejectReason: rejectReason.value.trim() })
    ElMessage.success('已拒绝退款')
    await loadDetail()
    emit('success')
  } finally {
    submitting.value = false
  }
}

const shipReturn = async () => {
  if (!props.order?.id) return
  if (!returnForm.companyCode.trim() || !returnForm.trackingNo.trim()) {
    ElMessage.warning('请填写完整退货物流信息')
    return
  }
  submitting.value = true
  try {
    await refundApi.shipReturn(props.order.id, {
      companyCode: returnForm.companyCode.trim(),
      trackingNo: returnForm.trackingNo.trim(),
      note: returnForm.note || '买家已寄回'
    })
    ElMessage.success('退货物流已提交')
    await loadDetail()
    emit('success')
  } finally {
    submitting.value = false
  }
}

const confirmReturn = async () => {
  if (!props.order?.id) return
  submitting.value = true
  try {
    await refundApi.confirmReturn(props.order.id)
    ElMessage.success('退款流程已完成')
    await loadDetail()
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    resetForms()
    loadDetail().catch(() => {})
  }
)
</script>

<style scoped>
.inner-card {
  margin-bottom: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
