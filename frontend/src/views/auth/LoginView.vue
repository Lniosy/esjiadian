<template>
  <div class="login-bg">
    <div class="login-wrap">
      <!-- 品牌 logo 区 -->
      <div class="brand-area">
        <div class="brand-icon-big">♻</div>
        <div class="brand-name">闲家电</div>
        <div class="brand-slogan">闲置家电，让它继续发光</div>
      </div>

      <!-- 登录卡片 -->
      <el-card class="login-card" shadow="always">
        <el-tabs v-model="activeTab" stretch class="login-tabs">
          <el-tab-pane label="密码登录" name="password">
            <el-form label-position="top" class="login-form">
              <el-form-item label="账号">
                <el-input v-model="form.account" placeholder="邮箱或手机号" size="large" prefix-icon="User" clearable />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" prefix-icon="Lock" @keyup.enter="handleLogin" />
              </el-form-item>
              <el-button type="primary" size="large" class="btn-login" :loading="loading" @click="handleLogin">登 录</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="验证码登录" name="code">
            <el-form label-position="top" class="login-form">
              <el-form-item label="账号">
                <el-input v-model="form.account" placeholder="邮箱或手机号" size="large" prefix-icon="User" />
              </el-form-item>
              <el-form-item label="验证码">
                <div class="code-input-group">
                  <el-input v-model="form.verifyCode" placeholder="6位验证码" size="large" prefix-icon="Ticket" />
                  <el-button :disabled="!!countdown" class="btn-send-code" @click="sendCode">
                    {{ countdown ? `${countdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-button type="primary" size="large" class="btn-login" :loading="loading" @click="handleLoginByCode">登 录 / 注 册</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form label-position="top" class="login-form">
              <el-form-item label="账号">
                <el-input v-model="form.account" placeholder="邮箱或手机号" size="large" prefix-icon="User" />
              </el-form-item>
              <el-form-item label="设置密码">
                <el-input v-model="form.password" type="password" show-password placeholder="长度≥8位，含字母和数字" size="large" prefix-icon="Lock" />
              </el-form-item>
              <el-form-item label="验证码">
                <div class="code-input-group">
                  <el-input v-model="form.verifyCode" placeholder="6位验证码" size="large" prefix-icon="Ticket" />
                  <el-button :disabled="!!countdown" class="btn-send-code" @click="sendCode">
                    {{ countdown ? `${countdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="form.agreed">
                  我已阅读并同意 <el-link type="primary" :underline="false">《用户协议》</el-link> 与 <el-link type="primary" :underline="false">《隐私政策》</el-link>
                </el-checkbox>
              </el-form-item>
              <el-button type="primary" size="large" class="btn-login" :loading="loading" :disabled="!form.agreed" @click="handleRegister">注 册</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="demo-hint" v-if="activeTab === 'password'">
          <el-icon color="#bbb"><InfoFilled /></el-icon>
          演示账号：<code>admin@example.com</code> / <code>Admin1234</code>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled, User, Lock, Ticket } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { authApi } from '../../api/modules'

const router = useRouter()
const auth = useAuthStore()
const activeTab = ref('password')
const loading = ref(false)
const countdown = ref(0)

const form = reactive({
  account: '',
  password: '',
  verifyCode: '',
  agreed: false
})

const sendCode = async () => {
  if (!form.account) {
    ElMessage.warning('请先输入账号')
    return
  }
  try {
    const res = await authApi.sendCode({ account: form.account })
    // 优先读取 res.code，兜底 res.verifyCode
    const displayCode = res.code || res.verifyCode
    ElMessage.success('验证码已发送（演示环境验证码：' + displayCode + '）')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (err) {
    ElMessage.error(err?.message || '验证码发送失败')
  }
}

const handleLogin = async () => {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.account, form.password)
    router.push('/')
  } catch (err) {
    ElMessage.error(err?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleLoginByCode = async () => {
  if (!form.account || !form.verifyCode) {
    ElMessage.warning('请输入账号和验证码')
    return
  }
  loading.value = true
  try {
    await auth.loginByCode(form.account, form.verifyCode)
    router.push('/')
  } catch (err) {
    ElMessage.error(err?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!form.account || !form.password || !form.verifyCode) {
    ElMessage.warning('请完整填写注册信息')
    return
  }
  if (!form.agreed) {
    ElMessage.warning('请阅读并同意用户协议')
    return
  }
  loading.value = true
  try {
    await auth.register(form.account, form.password, form.verifyCode, form.agreed)
    router.push('/')
  } catch (err) {
    ElMessage.error(err?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  background: linear-gradient(145deg, #fff5f0 0%, #ffe8dc 40%, #fff3ee 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-wrap {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

/* 品牌区 */
.brand-area {
  text-align: center;
}

.brand-icon-big {
  font-size: 56px;
  line-height: 1;
  margin-bottom: 8px;
  filter: drop-shadow(0 2px 8px rgba(255, 87, 34, 0.3));
}

.brand-name {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #FF5722, #FF8C00);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
}

.brand-slogan {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* 登录卡 */
.login-card {
  width: 100%;
  border-radius: 20px !important;
  padding: 8px 4px;
}

.login-tabs :deep(.el-tabs__item) {
  font-weight: 600;
  font-size: 15px;
}

.login-form {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.code-input-group {
  display: flex;
  gap: 10px;
  width: 100%;
}

.btn-send-code {
  width: 120px;
  flex-shrink: 0;
}

.btn-login {
  width: 100%;
  margin-top: 12px;
  border-radius: 24px !important;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
}

/* 演示提示 */
.demo-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 20px;
  padding: 10px 14px;
  background: #fff8f0;
  border-radius: 10px;
  border: 1px solid #ffe0cc;
  font-size: 12px;
  color: #888;
  flex-wrap: wrap;
}

.demo-hint code {
  background: #ffe8d8;
  color: #e65a15;
  padding: 1px 5px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
}
</style>
