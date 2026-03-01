<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand">二手家电交易系统</div>
      <el-menu mode="horizontal" :default-active="activeTop" :ellipsis="false" class="top-menu" @select="go">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/products">商品</el-menu-item>
        <el-menu-item index="/orders/list">订单</el-menu-item>
        <el-menu-item index="/shop">店铺</el-menu-item>
        <el-menu-item index="/chat">聊天</el-menu-item>
        <el-menu-item index="/notifications">通知</el-menu-item>
        <el-menu-item index="/disputes">纠纷</el-menu-item>
        <el-menu-item index="/admin/overview">管理后台</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <div class="page-wrap">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeTop = computed(() => {
  const p = route.path
  if (p.startsWith('/orders')) return '/orders/list'
  if (p.startsWith('/admin')) return '/admin/overview'
  return p
})

const go = (path) => router.push(path)
</script>

<style scoped>
.brand {
  font-size: 18px;
  font-weight: 700;
  color: var(--app-text-strong);
  margin-right: 18px;
  white-space: nowrap;
}

.top-menu {
  flex: 1;
  min-width: 0;
  background: transparent;
}
</style>
