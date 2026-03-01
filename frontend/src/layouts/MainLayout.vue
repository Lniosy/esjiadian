<template>
  <el-container class="app-shell">
    <el-header class="app-header" :height="'auto'">
      <!-- 品牌 Logo -->
      <router-link to="/" class="brand-logo">
        <span class="brand-icon">♻</span>
        <span class="brand-name">闲家电</span>
      </router-link>

      <!-- 中部搜索栏 -->
      <div class="header-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索家电品牌、型号或关键词…"
          clearable
          size="large"
          class="search-input"
          @keydown.enter="doSearch"
        >
          <template #prefix>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </template>
          <template #append>
            <el-button class="search-btn" @click="doSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <!-- 右侧操作区 -->
      <div class="header-right">
        <router-link to="/orders/cart" class="header-icon-btn" title="购物车">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
          <span class="icon-label">购物车</span>
        </router-link>
        <router-link to="/notifications" class="header-icon-btn" title="通知">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
          <span class="icon-label">通知</span>
        </router-link>

        <el-divider direction="vertical" style="height:20px;" />

        <el-dropdown trigger="click">
          <div class="user-trigger">
            <el-avatar :size="30" style="background:var(--c-primary);font-size:13px;font-weight:700;">
              {{ userAvatarText }}
            </el-avatar>
            <span class="user-name">{{ userLabel }}</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goShop">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                我的店铺
              </el-dropdown-item>
              <el-dropdown-item @click="$router.push('/orders/list')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                我的订单
              </el-dropdown-item>
              <el-dropdown-item @click="$router.push('/chat')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                消息聊天
              </el-dropdown-item>
              <el-dropdown-item divided @click="$router.push('/admin/overview')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                管理后台
              </el-dropdown-item>
              <el-dropdown-item divided @click="doLogout">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#F44336" stroke-width="2" style="margin-right:6px;"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                <span style="color:#F44336">退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 二级导航 -->
    <div class="sub-nav-bar">
      <div class="sub-nav-inner">
        <el-menu mode="horizontal" :default-active="activeTop" :ellipsis="false" @select="go">
          <el-menu-item index="/">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>
            首页
          </el-menu-item>
          <el-menu-item index="/products">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
            商品市场
          </el-menu-item>
          <el-menu-item index="/orders/list">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            我的订单
          </el-menu-item>
          <el-menu-item index="/shop">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            我的店铺
          </el-menu-item>
          <el-menu-item index="/chat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            消息
          </el-menu-item>
          <el-menu-item index="/disputes">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            纠纷
          </el-menu-item>
        </el-menu>
      </div>
    </div>

    <el-main class="app-main">
      <div class="page-wrap">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const searchKeyword = ref('')

const activeTop = computed(() => {
  const p = route.path
  if (p.startsWith('/orders')) return '/orders/list'
  if (p.startsWith('/admin')) return '/admin/overview'
  if (p.startsWith('/users')) return '/products'
  return p
})

const go = (path) => router.push(path)

const userLabel = computed(() => {
  return `用户${auth.userId || ''}`
})

const userAvatarText = computed(() => {
  return String(auth.userId || '我').slice(-2)
})

const goShop = () => router.push('/shop')

const doSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/products', query: { keyword: searchKeyword.value.trim() } })
  } else {
    router.push('/products')
  }
}

const doLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出登录？', '退出', {
      confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning'
    })
    auth.logout()
    router.push('/login')
  } catch { /* 取消 */ }
}
</script>

<style scoped>
/* 品牌 Logo */
.brand-logo {
  display: flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  flex-shrink: 0;
  margin-right: 24px;
}

.brand-icon {
  font-size: 22px;
  line-height: 1;
}

.brand-name {
  font-size: 20px;
  font-weight: 900;
  background: linear-gradient(135deg, #FF5722, #FF8A65);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

/* 搜索栏 */
.header-search {
  flex: 1;
  max-width: 500px;
}

:deep(.search-input .el-input__wrapper) {
  border-radius: 999px !important;
  padding-left: 14px;
  border: 1.5px solid #EBEBEB;
  box-shadow: none !important;
}

:deep(.search-input .el-input-group__append) {
  border-radius: 0 999px 999px 0 !important;
  background: var(--c-primary);
  border: none;
  padding: 0 16px;
}

.search-btn {
  background: transparent !important;
  border: none !important;
  color: #fff !important;
  font-size: 13px;
  font-weight: 600;
  padding: 0;
}

/* 右侧区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 20px;
  flex-shrink: 0;
}

.header-icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: var(--c-text-secondary);
  transition: color 0.16s, background 0.16s;
  cursor: pointer;
}

.header-icon-btn:hover {
  color: var(--c-primary);
  background: var(--c-primary-bg);
}

.icon-label {
  font-size: 11px;
  line-height: 1;
}

/* 用户下拉 */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.16s;
}

.user-trigger:hover {
  background: var(--c-primary-bg);
}

.user-name {
  font-size: 13px;
  color: var(--c-text-secondary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 二级导航栏 */
.sub-nav-bar {
  background: #fff;
  border-bottom: 1px solid var(--c-border-soft);
  position: sticky;
  top: 56px;
  z-index: 90;
}

.sub-nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 16px;
}

:deep(.sub-nav-bar .el-menu--horizontal) {
  gap: 0;
}

:deep(.sub-nav-bar .el-menu-item) {
  display: flex;
  align-items: center;
  font-size: 14px !important;
  height: 44px !important;
  line-height: 44px !important;
  padding: 0 14px !important;
  border-bottom-width: 2px !important;
}
</style>
