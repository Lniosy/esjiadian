<template>
  <el-drawer v-model="visible" title="发起投诉" size="42%">
    <el-empty v-if="!order" description="请先选择订单" />
    <template v-else>
      <el-alert type="warning" :closable="false" show-icon>
        当前订单：#{{ order.id }}。投诉将自动绑定此订单，无需手动输入订单 ID。
      </el-alert>

      <el-card class="inner-card" shadow="never" style="margin-top: 14px;">
        <template #header>
          <div class="section-head">
            <span>投诉表单</span>
            <el-tag :type="hasOpenDispute ? 'warning' : 'success'">
              {{ hasOpenDispute ? '已有处理中投诉' : '可提交' }}
            </el-tag>
          </div>
        </template>

        <el-form label-width="80px">
          <el-form-item label="投诉原因">
            <el-input
              v-model="reason"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="请输入投诉原因"
            />
          </el-form-item>
        </el-form>

        <div class="action-group">
          <el-button
            type="danger"
            :loading="submitting"
            :disabled="hasOpenDispute || !reason.trim()"
            @click="submit"
          >
            提交投诉
          </el-button>
        </div>
      </el-card>

      <el-card class="inner-card" shadow="never">
        <template #header><span>该订单投诉记录</span></template>
        <el-table :data="orderDisputes" empty-text="暂无记录">
          <el-table-column prop="id" label="投诉编号" width="92" />
          <el-table-column prop="reason" label="原因" min-width="220" />
          <el-table-column label="状态" width="110">
            <template #default="scope">
              <el-tag>{{ disputeStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resultNote" label="处理结果" min-width="180" />
        </el-table>
      </el-card>
    </template>

    <template #footer>
      <div class="action-group">
        <el-button @click="visible = false">关闭</el-button>
        <el-button :loading="loading" @click="loadDisputes">刷新投诉记录</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { disputeApi } from '../../../api/modules'
import { disputeStatusText } from '../../../utils/display'

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
const reason = ref('商品与描述不符')
const allDisputes = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const orderDisputes = computed(() => {
  const orderId = Number(props.order?.id || 0)
  if (!orderId) return []
  return allDisputes.value.filter((it) => Number(it.orderId) === orderId)
})

const hasOpenDispute = computed(() => {
  return orderDisputes.value.some((it) => ['OPEN', 'PENDING', 'PROCESSING'].includes(String(it.status || '').toUpperCase()))
})

const loadDisputes = async () => {
  loading.value = true
  try {
    allDisputes.value = await disputeApi.myList()
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!props.order?.id) {
    ElMessage.warning('请先选择订单')
    return
  }
  if (!reason.value.trim()) {
    ElMessage.warning('请填写投诉原因')
    return
  }
  if (hasOpenDispute.value) {
    ElMessage.warning('该订单已有处理中投诉')
    return
  }

  submitting.value = true
  try {
    await disputeApi.create({
      orderId: Number(props.order.id),
      reason: reason.value.trim()
    })
    ElMessage.success('投诉已提交')
    await loadDisputes()
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    reason.value = '商品与描述不符'
    loadDisputes().catch(() => {})
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
