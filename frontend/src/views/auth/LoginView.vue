<template>
  <el-card style="max-width: 420px; margin: 80px auto;">
    <h2>登录</h2>
    <el-form @submit.prevent>
      <el-form-item label="账号"><el-input v-model="account" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="password" type="password" /></el-form-item>
      <el-button type="primary" @click="doLogin">登录</el-button>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const account = ref('admin@example.com')
const password = ref('Admin1234')
const auth = useAuthStore()
const router = useRouter()

const doLogin = async () => {
  await auth.login(account.value, password.value)
  router.push('/')
}
</script>
