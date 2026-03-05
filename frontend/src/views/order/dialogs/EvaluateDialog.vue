<template>
  <el-dialog v-model="visible" title="交易评价" width="560px" @closed="resetForm">
    <el-empty v-if="!order" description="请先选择订单" />
    <template v-else>
      <el-alert type="info" :closable="false" show-icon>
        当前订单：#{{ order.id }}，商品 #{{ order.productId }}
      </el-alert>

      <el-form label-width="82px" style="margin-top: 14px;">
        <el-form-item label="评分">
          <el-rate v-model="form.score" show-score score-template="{value} 分" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="分享本次交易体验"
          />
        </el-form-item>
        <el-form-item label="评价标签">
          <el-checkbox-group v-model="form.tags">
            <el-checkbox v-for="tag in tagOptions" :key="tag" :label="tag">{{ tag }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="上传图片">
          <ImageUploader
            v-model="form.images"
            :max="6"
            :allow-external-url="true"
            add-label="选择评价图片"
          />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <div class="action-group">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">
          提交评价
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluationApi } from '../../../api/modules'
import ImageUploader from '../../../components/common/ImageUploader.vue'

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

const tagOptions = ['成色好', '发货快', '描述符实', '卖家服务好', '包装精心', '价格实惠']
const submitting = ref(false)
const form = reactive({
  score: 5,
  content: '',
  tags: [],
  images: []
})

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const canSubmit = computed(() => Boolean(props.order?.id) && form.score > 0)

const resetForm = () => {
  form.score = 5
  form.content = ''
  form.tags = []
  form.images = []
}

const submit = async () => {
  if (!props.order?.id) {
    ElMessage.warning('请先选择订单')
    return
  }
  if (form.score <= 0) {
    ElMessage.warning('请先评分')
    return
  }
  submitting.value = true
  try {
    await evaluationApi.create(props.order.id, {
      productId: props.order.productId,
      score: form.score,
      content: form.content || '商品符合描述，交易顺利',
      images: form.images,
      tags: form.tags
    })
    ElMessage.success('评价成功')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>
