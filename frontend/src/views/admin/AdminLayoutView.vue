<template>
  <div class="page-stack">
    <section class="hero-panel admin-hero">
      <h1 class="hero-title">管理后台</h1>
      <p class="hero-subtitle">运营指标、系统监控、治理审计、风控管理一体化工作台。</p>
      <div class="subnav">
        <router-link to="/admin/overview"><el-button :type="is('/admin/overview') ? 'primary' : 'default'">概览与趋势</el-button></router-link>
        <router-link to="/admin/monitor"><el-button :type="is('/admin/monitor') ? 'primary' : 'default'">系统监控</el-button></router-link>
        <router-link to="/admin/governance"><el-button :type="is('/admin/governance') ? 'primary' : 'default'">治理与审计</el-button></router-link>
        <router-link to="/admin/risk"><el-button :type="is('/admin/risk') ? 'primary' : 'default'">风控与用户</el-button></router-link>
      </div>
    </section>
    <router-view />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
const is = (path) => route.path === path
</script>

<style scoped>
.admin-hero {
  background:
    linear-gradient(145deg, rgba(19, 35, 55, 0.94), rgba(37, 62, 95, 0.9)),
    radial-gradient(circle at 88% 8%, rgba(255, 255, 255, 0.25), transparent 36%);
}
</style>
