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
        <h2 class="card-title">欢迎登录</h2>

        <el-form @submit.prevent label-position="top" class="login-form">
          <el-form-item label="账号">
            <el-input
              v-model="account"
              placeholder="邮箱或手机号"
              size="large"
              prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              @keyup.enter="doLogin"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="btn-login"
            :loading="loading"
            @click="doLogin"
          >登 录</el-button>
        </el-form>

        <div class="demo-hint">
          <el-icon color="#bbb"><InfoFilled /></el-icon>
          演示账号：<code>admin@example.com</code> / <code>Admin1234</code>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'

const account = ref('')
const password = ref('')
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()

const doLogin = async () => {
  if (!account.value || !password.value) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(account.value, password.value)
    router.push('/')
  } catch (err) {
    ElMessage.error(err?.message || '登录失败，请检查账号或密码')
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
  max-width: 400px;
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

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0 0 20px;
  text-align: center;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.btn-login {
  width: 100%;
  margin-top: 8px;
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
  margin-top: 16px;
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
