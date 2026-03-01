<template>
  <div class="login-page">
    <section class="login-side hero-panel">
      <h1 class="hero-title">欢迎回来</h1>
      <p class="hero-subtitle">登录后可进入商品、订单、店铺、会话与管理模块，快速完成演示全流程。</p>
      <ul class="highlights">
        <li>支持买卖双方交易链路</li>
        <li>支持店铺运营与评价体系</li>
        <li>支持风控审计与系统监控</li>
      </ul>
    </section>

    <el-card class="login-card page-card">
      <template #header>
        <div class="page-head">
          <h3>账号登录</h3>
          <span class="muted">推荐使用演示管理员账号</span>
        </div>
      </template>

      <el-form @submit.prevent label-position="top" class="form">
        <el-form-item label="账号">
          <el-input v-model="account" placeholder="邮箱或手机号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" @click="doLogin">登录系统</el-button>
      </el-form>
      <div class="demo">演示账号：admin@example.com / Admin1234</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const account = ref('admin@example.com')
const password = ref('Admin1234')
const auth = useAuthStore()
const router = useRouter()

const doLogin = async () => {
  try {
    await auth.login(account.value, password.value)
    router.push('/')
  } catch (err) {
    ElMessage.error(err?.message || '登录失败，请检查账号或密码')
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 140px);
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 14px;
  align-items: stretch;
}

.login-side {
  display: grid;
  align-content: center;
}

.highlights {
  margin: 14px 0 0;
  padding-left: 18px;
  line-height: 1.9;
  font-size: 14px;
}

.login-card {
  align-self: center;
}

.form {
  margin-top: 8px;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.demo {
  margin-top: 12px;
  color: #708097;
  font-size: 13px;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
  }
}
</style>
