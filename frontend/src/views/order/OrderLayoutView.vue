<template>
  <div class="page-stack">
    <section class="hero-panel">
      <h1 class="hero-title">订单中心</h1>
      <p class="hero-subtitle">统一管理下单、履约、售后与地址配置，演示买卖双方交易链路。</p>
      <div class="subnav">
        <router-link to="/orders/list"><el-button :type="is('/orders/list') ? 'primary' : 'default'">订单流程</el-button></router-link>
        <router-link to="/orders/cart"><el-button :type="is('/orders/cart') ? 'primary' : 'default'">购物车下单</el-button></router-link>
        <router-link to="/orders/address"><el-button :type="is('/orders/address') ? 'primary' : 'default'">地址管理</el-button></router-link>
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
