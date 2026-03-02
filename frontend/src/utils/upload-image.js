import { ElMessage } from 'element-plus'
import { fileApi } from '../api/modules'

export const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
const ALLOWED_EXT_RE = /\.(jpe?g|png|webp|gif)$/i

function isImageType(file) {
  if (!file) return false
  if (typeof file.type === 'string' && file.type.toLowerCase().startsWith('image/')) {
    return true
  }
  return typeof file.name === 'string' && ALLOWED_EXT_RE.test(file.name)
}

export function validateImageFile(file) {
  if (!file) {
    return { ok: false, message: '请选择图片文件' }
  }
  if (!isImageType(file)) {
    return { ok: false, message: '仅支持 jpg/jpeg/png/webp/gif 图片' }
  }
  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return { ok: false, message: '图片大小不能超过 5MB' }
  }
  return { ok: true, message: '' }
}

export async function uploadImageFile(file) {
  const checked = validateImageFile(file)
  if (!checked.ok) {
    ElMessage.warning(checked.message)
    throw new Error(checked.message)
  }
  try {
    const uploaded = await fileApi.uploadImage(file)
    if (!uploaded?.url) {
      throw new Error('上传返回结果无效')
    }
    return uploaded
  } catch (error) {
    const msg = error?.message || '图片上传失败'
    ElMessage.error(msg)
    throw error
  }
}
