<template>
  <div class="image-uploader">
    <div class="uploader-head">
      <el-upload
        accept="image/*"
        :http-request="onUpload"
        :show-file-list="false"
        :disabled="!canAddMore"
      >
        <el-button size="small" :disabled="!canAddMore || uploading">{{ addLabel }}</el-button>
      </el-upload>
      <span class="uploader-count">{{ valueList.length }}/{{ max }}</span>
    </div>

    <div class="external-input" v-if="allowExternalUrl && canAddMore">
      <el-input v-model="externalUrl" :placeholder="externalPlaceholder" clearable @keydown.enter.prevent="appendExternal" />
      <el-button size="small" @click="appendExternal">添加外链</el-button>
    </div>

    <div class="thumb-list" v-if="valueList.length">
      <div v-for="(src, idx) in valueList" :key="`${src}-${idx}`" class="thumb-item">
        <a :href="src" target="_blank" rel="noreferrer">
          <img :src="src" alt="图片" class="thumb-img" />
        </a>
        <el-button type="danger" text size="small" @click="removeAt(idx)">删除</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImageFile } from '../../utils/upload-image'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  max: {
    type: Number,
    default: 9
  },
  allowExternalUrl: {
    type: Boolean,
    default: true
  },
  addLabel: {
    type: String,
    default: '上传图片'
  },
  externalPlaceholder: {
    type: String,
    default: '粘贴图片 URL（http/https 或 /uploads/）'
  }
})

const emit = defineEmits(['update:modelValue'])
const externalUrl = ref('')
const uploading = ref(false)

const valueList = computed(() => (Array.isArray(props.modelValue) ? props.modelValue.filter(Boolean) : []))
const canAddMore = computed(() => valueList.value.length < props.max)

const emitList = (list) => {
  emit('update:modelValue', list)
}

const removeAt = (idx) => {
  const next = [...valueList.value]
  next.splice(idx, 1)
  emitList(next)
}

const appendExternal = () => {
  const url = String(externalUrl.value || '').trim()
  if (!url) return
  if (!/^(https?:\/\/|\/uploads\/)/i.test(url)) {
    ElMessage.warning('仅支持 http/https 外链或 /uploads/ 开头地址')
    return
  }
  if (!canAddMore.value) {
    ElMessage.warning(`最多上传 ${props.max} 张图片`)
    return
  }
  emitList([...valueList.value, url])
  externalUrl.value = ''
}

const onUpload = async ({ file }) => {
  if (!canAddMore.value) {
    ElMessage.warning(`最多上传 ${props.max} 张图片`)
    return false
  }
  uploading.value = true
  try {
    const uploaded = await uploadImageFile(file)
    emitList([...valueList.value, uploaded.url])
  } finally {
    uploading.value = false
  }
  return false
}
</script>

<style scoped>
.image-uploader {
  width: 100%;
  display: grid;
  gap: 8px;
}

.uploader-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploader-count {
  color: #888;
  font-size: 12px;
}

.external-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.thumb-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 10px;
}

.thumb-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 6px;
  display: grid;
  gap: 6px;
  background: #fff;
}

.thumb-img {
  width: 100%;
  height: 72px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
  background: #f4f4f5;
}
</style>
