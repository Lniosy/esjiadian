<template>
  <div class="page-stack">
    <!-- 个人资料卡 -->
    <el-card class="page-card" v-loading="loading">
      <template #header>
        <div class="page-head">
          <h3>个人中心</h3>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        </div>
      </template>

      <div class="profile-layout">
        <!-- 左侧头像 -->
        <div class="avatar-section">
          <el-avatar :size="100" :src="form.avatarUrl || ''" class="big-avatar">
            {{ (form.nickname || '我').slice(0, 2) }}
          </el-avatar>
          <ImageUploader
            v-model="avatarImages"
            :max="1"
            :allow-external-url="true"
            add-label="更换头像"
            external-placeholder="粘贴头像外链"
          />
        </div>

        <!-- 右侧表单 -->
        <el-form :model="form" label-width="90px" class="profile-form">
          <el-form-item label="用户ID">
            <el-input :model-value="String(auth.userId)" disabled />
          </el-form-item>
          <el-form-item label="昵称" required>
            <el-input v-model="form.nickname" maxlength="20" show-word-limit placeholder="设置你的昵称" />
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input
              v-model="form.bio"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="介绍一下你自己"
            />
          </el-form-item>
          <el-form-item label="认证状态">
            <el-tag :type="authTagType" size="large">{{ authStatusLabel }}</el-tag>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 实名认证 -->
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>实名认证</h3>
          <el-tag v-if="authStatusRaw === 'APPROVED'" type="success">已认证</el-tag>
          <el-tag v-else-if="authStatusRaw === 'PENDING'" type="warning">审核中</el-tag>
        </div>
      </template>

      <template v-if="authStatusRaw === 'APPROVED'">
        <el-result icon="success" title="实名认证已通过" sub-title="你已获得卖家权限，可以发布商品。" />
      </template>
      <template v-else-if="authStatusRaw === 'PENDING'">
        <el-result icon="warning" title="认证审核中" sub-title="你的实名认证正在审核中，请耐心等待。" />
      </template>
      <template v-else>
        <p class="auth-tip">完成实名认证后即可获得卖家权限，发布二手家电商品。</p>
        <el-form :model="verifyForm" label-width="90px" class="verify-form">
          <el-form-item label="真实姓名" required>
            <el-input v-model="verifyForm.realName" placeholder="请输入真实姓名" maxlength="20" />
          </el-form-item>
          <el-form-item label="身份证号" required>
            <el-input v-model="verifyForm.idCard" placeholder="18位身份证号码" maxlength="18" />
          </el-form-item>
          <el-form-item label="手机号" required>
            <el-input v-model="verifyForm.phone" placeholder="与注册手机号一致" maxlength="11" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="verifying" @click="submitVerify">提交认证</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>

    <!-- 收货地址管理 -->
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>收货地址 <span class="count-tip">（{{ addresses.length }} 个）</span></h3>
          <el-button type="primary" @click="openAddressDialog(null)">＋ 新增地址</el-button>
        </div>
      </template>

      <div v-if="addresses.length" class="address-list">
        <div v-for="addr in addresses" :key="addr.id" class="address-item" :class="{ 'is-default': addr.isDefault }">
          <div class="address-info">
            <div class="address-head">
              <span class="address-name">{{ addr.receiverName }}</span>
              <span class="address-phone">{{ addr.receiverPhone }}</span>
              <el-tag v-if="addr.isDefault" size="small" type="warning" round>默认</el-tag>
            </div>
            <div class="address-detail">
              {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detailAddress }}
            </div>
          </div>
          <div class="address-actions">
            <el-button text type="primary" size="small" @click="openAddressDialog(addr)">编辑</el-button>
            <el-button text type="danger" size="small" @click="removeAddress(addr.id)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无收货地址" :image-size="60" />
    </el-card>

    <!-- 新增/编辑地址弹窗 -->
    <el-dialog
      v-model="addrDialog.visible"
      :title="addrDialog.editId ? '编辑地址' : '新增地址'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="addrForm" label-width="90px">
        <el-form-item label="收货人" required>
          <el-input v-model="addrForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="addrForm.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="省份" required>
              <el-input v-model="addrForm.province" placeholder="省" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市" required>
              <el-input v-model="addrForm.city" placeholder="市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县" required>
              <el-input v-model="addrForm.district" placeholder="区" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" required>
          <el-input v-model="addrForm.detailAddress" type="textarea" :rows="2" placeholder="街道、门牌号等" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addrForm.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="savingAddr" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { userApi } from '../../api/modules'
import ImageUploader from '../../components/common/ImageUploader.vue'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const verifying = ref(false)
const savingAddr = ref(false)

// ---- 个人资料 ----
const form = reactive({ nickname: '', avatarUrl: '', bio: '' })
const avatarImages = ref([])
const authStatusRaw = ref('')

// 同步头像图片列表 ↔ form.avatarUrl
watch(avatarImages, (imgs) => {
  form.avatarUrl = imgs.length ? imgs[0] : ''
})

const authTagType = computed(() => {
  if (authStatusRaw.value === 'APPROVED') return 'success'
  if (authStatusRaw.value === 'PENDING') return 'warning'
  return 'info'
})
const authStatusLabel = computed(() => {
  if (authStatusRaw.value === 'APPROVED') return '✓ 已认证'
  if (authStatusRaw.value === 'PENDING') return '审核中'
  return '未认证'
})

const loadProfile = async () => {
  loading.value = true
  try {
    const data = await userApi.me()
    form.nickname = data.nickname || ''
    form.avatarUrl = data.avatarUrl || ''
    form.bio = data.bio || ''
    authStatusRaw.value = data.authStatus || ''
    avatarImages.value = data.avatarUrl ? [data.avatarUrl] : []
  } finally {
    loading.value = false
  }
}

const saveProfile = async () => {
  if (!form.nickname?.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await userApi.updateMe({
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl || '',
      bio: form.bio || ''
    })
    ElMessage.success('资料已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ---- 实名认证 ----
const verifyForm = reactive({ realName: '', idCard: '', phone: '' })

const submitVerify = async () => {
  if (!verifyForm.realName.trim() || !verifyForm.idCard.trim() || !verifyForm.phone.trim()) {
    ElMessage.warning('请填写完整认证信息')
    return
  }
  verifying.value = true
  try {
    await userApi.applyRealName(verifyForm)
    ElMessage.success('认证申请已提交')
    authStatusRaw.value = 'PENDING'
  } catch {
    ElMessage.error('提交失败')
  } finally {
    verifying.value = false
  }
}

// ---- 收货地址 ----
const addresses = ref([])
const addrDialog = reactive({ visible: false, editId: null })
const addrForm = reactive({
  receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false
})

const loadAddresses = async () => {
  try {
    addresses.value = await userApi.addresses()
  } catch { /* 忽略 */ }
}

const openAddressDialog = (addr) => {
  if (addr) {
    addrDialog.editId = addr.id
    Object.assign(addrForm, {
      receiverName: addr.receiverName,
      receiverPhone: addr.receiverPhone,
      province: addr.province,
      city: addr.city,
      district: addr.district,
      detailAddress: addr.detailAddress,
      isDefault: addr.isDefault
    })
  } else {
    addrDialog.editId = null
    Object.assign(addrForm, {
      receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false
    })
  }
  addrDialog.visible = true
}

const saveAddress = async () => {
  if (!addrForm.receiverName.trim() || !addrForm.receiverPhone.trim() || !addrForm.detailAddress.trim()) {
    ElMessage.warning('请填写必要的地址信息')
    return
  }
  savingAddr.value = true
  try {
    if (addrDialog.editId) {
      await userApi.updateAddress(addrDialog.editId, { ...addrForm })
    } else {
      await userApi.addAddress({ ...addrForm })
    }
    ElMessage.success('地址已保存')
    addrDialog.visible = false
    await loadAddresses()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingAddr.value = false
  }
}

const removeAddress = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除此地址？', '删除', { type: 'warning' })
    await userApi.deleteAddress(id)
    ElMessage.success('已删除')
    await loadAddresses()
  } catch { /* 取消 */ }
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadAddresses()])
})
</script>

<style scoped>
/* 布局 */
.profile-layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.big-avatar {
  background: linear-gradient(135deg, #FF5722, #FF8C00);
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}

.profile-form {
  flex: 1;
  min-width: 0;
}

/* 认证 */
.auth-tip {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin: 0 0 16px;
}

.verify-form {
  max-width: 460px;
}

.count-tip {
  font-size: 13px;
  font-weight: 400;
  color: var(--c-text-tertiary);
}

/* 地址列表 */
.address-list {
  display: grid;
  gap: 10px;
}

.address-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid var(--c-border-soft);
  border-radius: var(--radius-sm);
  transition: border-color 0.2s;
}

.address-item:hover {
  border-color: var(--c-primary);
}

.address-item.is-default {
  border-color: var(--c-warning);
  background: #FFFBF0;
}

.address-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.address-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--c-text-primary);
}

.address-phone {
  font-size: 13px;
  color: var(--c-text-secondary);
}

.address-detail {
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 640px) {
  .profile-layout {
    flex-direction: column;
    align-items: stretch;
  }
  .avatar-section {
    flex-direction: row;
    gap: 16px;
  }
}
</style>
