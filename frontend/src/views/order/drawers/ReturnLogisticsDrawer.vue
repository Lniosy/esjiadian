<template>
  <el-drawer v-model="visible" title="填写退货物流" size="40%">
    <el-empty v-if="!order" description="请先选择订单" />
    <template v-else>
      <el-alert type="info" :closable="false" show-icon>
        当前订单：#{{ order.id }}，仅当退款状态为“待买家退货”时可提交。
      </el-alert>

      <el-card shadow="never" style="margin-top: 14px;">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="退款状态">
            {{ refundStatusText(refundStatus) }}
          </el-descriptions-item>
          <el-descriptions-item label="退款原因">
            {{ refundDetail?.reason || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-form label-width="88px" style="margin-top: 14px;">
        <el-form-item label="物流公司">
          <el-input v-model="form.companyCode" placeholder="如 SF" />
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="form.trackingNo" placeholder="请输入退货运单号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" type="textarea" :rows="2" maxlength="120" show-word-limit />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <div class="action-group">
        <el-button @click="visible = false">取消</el-button>
        <el-button :loading="loading" @click="loadRefundDetail">刷新状态</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="submit"
        >
          提交退货物流
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { refundApi } from '../../../api/modules'
import { refundStatusText } from '../../../utils/display'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const loading = ref(false)
const submitting = ref(false)
const refundDetail = ref(null)
const form = reactive({
  companyCode: 'SF',
  trackingNo: '',
  note: '买家已寄回'
})

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const refundStatus = computed(() => String(refundDetail.value?.status || 'NONE').toUpperCase())

const canSubmit = computed(() => {
  return Boolean(props.order?.id) &&
    refundStatus.value === 'RETURN_REQUIRED' &&
    Boolean(form.companyCode.trim()) &&
    Boolean(form.trackingNo.trim())
})

const resetForm = () => {
  form.companyCode = 'SF'
  form.trackingNo = ''
  form.note = '买家已寄回'
}

const loadRefundDetail = async () => {
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

const submit = async () => {
  if (!props.order?.id) return
  if (refundStatus.value !== 'RETURN_REQUIRED') {
    ElMessage.warning('当前退款状态不可填写退货物流')
    return
  }
  if (!form.companyCode.trim() || !form.trackingNo.trim()) {
    ElMessage.warning('请填写完整物流信息')
    return
  }

  submitting.value = true
  try {
    await refundApi.shipReturn(props.order.id, {
      companyCode: form.companyCode.trim(),
      trackingNo: form.trackingNo.trim(),
      note: form.note || '买家已寄回'
    })
    ElMessage.success('退货物流已提交')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    resetForm()
    loadRefundDetail().catch(() => {})
  }
)
</script>
