<template>
  <el-drawer v-model="visible" title="发货操作" size="40%">
    <el-empty v-if="!order" description="请先选择订单" />
    <template v-else>
      <el-alert type="info" :closable="false" show-icon>
        当前订单：#{{ order.id }}，商品 #{{ order.productId }}
      </el-alert>

      <el-form label-width="88px" style="margin-top: 14px;">
        <el-form-item label="物流公司">
          <el-select v-model="form.companyCode" style="width: 100%;">
            <el-option label="顺丰速运 (SF)" value="SF" />
            <el-option label="圆通速递 (YTO)" value="YTO" />
            <el-option label="中通快递 (ZTO)" value="ZTO" />
            <el-option label="韵达快递 (YD)" value="YD" />
            <el-option label="申通快递 (STO)" value="STO" />
            <el-option label="极兔速递 (JT)" value="JT" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="form.trackingNo" clearable placeholder="请输入运单号" />
        </el-form-item>
        <el-form-item label="发货备注">
          <el-input v-model="form.shipNote" type="textarea" :rows="2" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <div class="action-group">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">
          确认发货
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { logisticsApi, orderApi } from '../../../api/modules'

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

const submitting = ref(false)
const form = reactive({
  companyCode: 'SF',
  trackingNo: '',
  shipNote: ''
})

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const canSubmit = computed(() => {
  return Boolean(props.order?.id) && Boolean(form.companyCode) && Boolean(form.trackingNo.trim())
})

const resetForm = () => {
  form.companyCode = 'SF'
  form.trackingNo = ''
  form.shipNote = ''
}

const submit = async () => {
  if (!props.order?.id) {
    ElMessage.warning('请先选择订单')
    return
  }
  if (!canSubmit.value) {
    ElMessage.warning('请完整填写发货信息')
    return
  }

  submitting.value = true
  try {
    const payload = {
      logisticsCompany: form.companyCode,
      trackingNo: form.trackingNo.trim(),
      shipNote: form.shipNote || '正常发货'
    }
    await orderApi.ship(props.order.id, payload)
    await logisticsApi.save(props.order.id, {
      companyCode: form.companyCode,
      trackingNo: form.trackingNo.trim(),
      shipNote: form.shipNote || '正常发货'
    })
    ElMessage.success('发货成功')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) resetForm()
  }
)
</script>
