<template>
  <el-card class="page-card" shadow="never">
    <template #header><span>售后服务</span></template>

    <el-empty v-if="!selectedOrder" description="请先在订单面板选择订单" />
    <template v-else>
      <el-alert type="info" :closable="false" show-icon>
        当前上下文订单：#{{ selectedOrder.id }}。所有售后动作都自动携带订单上下文，不再手工输入 ID。
      </el-alert>

      <div class="action-group" style="margin-top: 12px;">
        <el-button type="warning" @click="emitOpen('refund')">申请/处理退款</el-button>
        <el-button type="danger" @click="emitOpen('dispute')">发起投诉</el-button>
        <el-button type="primary" plain @click="emitOpen('evaluate')">提交评价</el-button>
      </div>
    </template>
  </el-card>
</template>

<script setup>
const props = defineProps({
  selectedOrder: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['open-drawer'])

const emitOpen = (drawer) => {
  if (!props.selectedOrder) return
  emit('open-drawer', { drawer, order: props.selectedOrder })
}
</script>
