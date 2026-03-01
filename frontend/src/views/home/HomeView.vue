<template>
  <div class="page-stack">
    <section class="hero-panel">
      <h1 class="hero-title">二手家电交易系统</h1>
      <p class="hero-subtitle">
        覆盖商品交易、店铺经营、即时沟通、售后争议与管理审计，适配毕设答辩演示的完整业务链路。
      </p>
      <div class="quick-row">
        <router-link to="/products"><el-button type="primary" size="large">进入商品市场</el-button></router-link>
        <router-link to="/orders/list"><el-button size="large">查看订单中心</el-button></router-link>
        <router-link to="/admin/overview"><el-button size="large">进入管理后台</el-button></router-link>
      </div>
    </section>

    <el-card class="page-card">
      <template #header><div class="page-head"><h3>核心能力</h3></div></template>
      <div class="cap-grid">
        <article class="cap-item" v-for="item in abilities" :key="item.title">
          <div class="cap-title">{{ item.title }}</div>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup>
const abilities = [
  { title: '交易闭环', desc: '商品发布、购物车、下单、支付回调、物流跟踪、收货评价全流程联通。' },
  { title: '会话沟通', desc: '从商品卖家主页发起聊天，支持文本、图片与商品卡片会话。' },
  { title: '店铺经营', desc: '店铺主页、在售商品、评分统计、历史评价与标签云统一展示。' },
  { title: '风险治理', desc: '举报处理、用户管理、纠纷协同、操作审计和系统监控集中管理。' }
]
</script>

<style scoped>
.quick-row {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cap-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.cap-item {
  border: 1px solid var(--app-border);
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, #fff, #f8fbff);
  padding: 14px;
}

.cap-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-strong);
  margin-bottom: 8px;
}

.cap-item p {
  margin: 0;
  color: #637286;
  line-height: 1.7;
}
</style>
