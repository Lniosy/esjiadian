<template>
  <div class="order-layout">
    <!-- 顶部标题栏 -->
    <div class="order-header">
      <div class="order-header-inner">
        <div class="order-title-group">
          <span class="order-icon">🛍️</span>
          <span class="order-title">订单中心</span>
        </div>
        <!-- Tab 导航 -->
        <nav class="order-tabs">
          <router-link
            v-for="tab in tabs"
            :key="tab.path"
            :to="tab.path"
            class="order-tab"
            :class="{ active: is(tab.path) }"
          >
            <span class="tab-icon">{{ tab.icon }}</span>
            {{ tab.label }}
          </router-link>
        </nav>
      </div>
    </div>

    <div class="order-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
const is = (path) => route.path === path

const tabs = [
  { path: '/orders/workbench', icon: '工作台', label: '订单工作台' }
]
</script>

<style scoped>
.order-layout {
  min-height: calc(100vh - 100px);
  background: var(--c-bg-page, #f5f5f5);
}

.order-header {
  background: #fff;
  border-bottom: 1px solid #f0ebe8;
  border-radius: 14px 14px 0 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.order-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 32px;
  height: 56px;
}

.order-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.order-icon {
  font-size: 20px;
}

.order-title {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

/* Tab 导航 */
.order-tabs {
  display: flex;
  gap: 4px;
  align-items: center;
  height: 100%;
}

.order-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  text-decoration: none;
  transition: all 0.2s;
  white-space: nowrap;
}

.order-tab:hover {
  background: #fff3ee;
  color: var(--c-primary, #FF5722);
}

.order-tab.active {
  background: var(--c-primary, #FF5722);
  color: #fff;
  font-weight: 600;
}

.tab-icon {
  font-size: 14px;
}

.order-content {
  padding: 16px 0 0;
}
</style>
