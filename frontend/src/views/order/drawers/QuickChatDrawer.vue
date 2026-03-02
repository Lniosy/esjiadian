<template>
  <el-drawer v-model="visible" title="联系用户" size="40%">
    <el-empty v-if="!order" description="请先选择订单" />
    <template v-else>
      <el-alert type="info" :closable="false" show-icon>
        当前订单：#{{ order.id }}。将自动联系与该订单关联的用户（ID: {{ peerId || '-' }}）。
      </el-alert>

      <el-form label-width="84px" style="margin-top: 14px;">
        <el-form-item label="消息内容">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请输入要发送的消息"
          />
        </el-form-item>
      </el-form>

      <el-card shadow="never">
        <div class="muted" style="font-size: 13px; line-height: 1.7;">
          若需查看完整历史会话，可点击“进入完整聊天”打开聊天页。
        </div>
      </el-card>
    </template>

    <template #footer>
      <div class="action-group">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!canSend" @click="send">
          发送
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi } from '../../../api/modules'

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

const currentUserId = Number(localStorage.getItem('userId') || 0)
const draft = ref('')
const submitting = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const peerId = computed(() => {
  if (!props.order) return 0
  const buyerId = Number(props.order.buyerId || 0)
  const sellerId = Number(props.order.sellerId || 0)
  if (!currentUserId) return buyerId || sellerId
  if (buyerId === currentUserId) return sellerId
  if (sellerId === currentUserId) return buyerId
  return sellerId || buyerId
})

const canSend = computed(() => Boolean(peerId.value) && Boolean(draft.value.trim()))

const send = async () => {
  if (!canSend.value) {
    ElMessage.warning('请填写消息内容')
    return
  }
  submitting.value = true
  try {
    await chatApi.send({
      toUserId: peerId.value,
      contentType: 'TEXT',
      content: draft.value.trim()
    })
    ElMessage.success('消息已发送')
    draft.value = ''
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) draft.value = ''
  }
)
</script>
