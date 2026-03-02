<template>
  <el-card class="page-card" shadow="never">
    <template #header><span>物流与履约</span></template>

    <el-empty v-if="!selectedOrder" description="请先在订单面板选择订单" />
    <template v-else>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ selectedOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="商品编号">{{ selectedOrder.productId }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ selectedOrder.status }}</el-descriptions-item>
        <el-descriptions-item label="履约动作">当前订单可执行物流操作</el-descriptions-item>
      </el-descriptions>

      <div class="action-group" style="margin-top: 12px;">
        <el-button type="primary" @click="emitOpen('shipment')">填写发货</el-button>
        <el-button @click="emitOpen('detail')">查看轨迹</el-button>
        <el-button type="warning" @click="emitOpen('returnLogistics')">填写退货物流</el-button>
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
