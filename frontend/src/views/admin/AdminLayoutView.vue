<template>
  <div class="admin-layout">
    <!-- 管理后台头部 -->
    <div class="admin-header">
      <div class="admin-header-inner">
        <div class="admin-title-group">
          <span class="admin-icon">⚙️</span>
          <span class="admin-title">管理后台</span>
          <span class="admin-badge">PRO</span>
        </div>
        <!-- Tab 导航 -->
        <nav class="admin-tabs">
          <router-link
            v-for="tab in tabs"
            :key="tab.path"
            :to="tab.path"
            class="admin-tab"
            :class="{ active: is(tab.path) }"
          >
            <span class="tab-icon">{{ tab.icon }}</span>
            {{ tab.label }}
          </router-link>
        </nav>
      </div>
    </div>

    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
const is = (path) => route.path === path

const tabs = [
  { path: '/admin/overview',   icon: '📊', label: '概览趋势' },
  { path: '/admin/monitor',    icon: '🖥️', label: '系统监控' },
  { path: '/admin/governance', icon: '🏛️', label: '治理审计' },
  { path: '/admin/risk',       icon: '🛡️', label: '风控用户' },
]
</script>

<style scoped>
.admin-layout {
  min-height: calc(100vh - 100px);
  background: #f7f8fa;
}

.admin-header {
  background: #fff;
  border-bottom: 2px solid #f0ebe8;
  border-radius: 14px 14px 0 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  margin-bottom: 0;
}

.admin-header-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
  height: 58px;
}

.admin-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.admin-icon {
  font-size: 20px;
}

.admin-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

.admin-badge {
  background: linear-gradient(135deg, #FF5722, #FF8C00);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.admin-tabs {
  display: flex;
  gap: 4px;
  align-items: center;
  height: 100%;
}

.admin-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 18px;
  border-radius: 8px;
  font-size: 14px;
  color: #555;
  text-decoration: none;
  transition: all 0.2s;
  white-space: nowrap;
}

.admin-tab:hover {
  background: #fff3ee;
  color: var(--c-primary, #FF5722);
}

.admin-tab.active {
  background: var(--c-primary, #FF5722);
  color: #fff;
  font-weight: 600;
  border-radius: 8px;
}

.tab-icon {
  font-size: 14px;
}

.admin-content {
  padding: 16px 0 0;
}
</style>
