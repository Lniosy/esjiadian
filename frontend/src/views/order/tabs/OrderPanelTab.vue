<template>
  <el-card class="page-card" shadow="never">
    <template #header>
      <div class="panel-header">
        <span>订单面板</span>
        <el-tag type="info">{{ viewLabel }}</el-tag>
      </div>
    </template>

    <el-table :data="orders" empty-text="暂无订单数据" @row-click="selectRow">
      <el-table-column prop="id" label="订单编号" width="100" />
      <el-table-column prop="productId" label="商品编号" width="100" />
      <el-table-column label="状态" width="160">
        <template #default="scope">
          <el-tag>{{ orderStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="260">
        <template #default="scope">
          <div class="action-group">
            <el-button size="small" @click.stop="emitOpen('detail', scope.row)">详情</el-button>
            <el-button
              size="small"
              type="primary"
              :disabled="!canShip(scope.row)"
              @click.stop="emitOpen('shipment', scope.row)"
            >
              发货
            </el-button>
            <el-button size="small" type="warning" @click.stop="emitOpen('refund', scope.row)">退款</el-button>
            <el-button size="small" type="danger" @click.stop="emitOpen('dispute', scope.row)">投诉</el-button>
            <el-button size="small" :disabled="!canContact(scope.row)" @click.stop="emitOpen('quickChat', scope.row)">联系用户</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { orderStatusText } from '../../../utils/display'

const props = defineProps({
  orders: {
    type: Array,
    default: () => []
  },
  viewMode: {
    type: String,
    default: 'buyer'
  }
})

const emit = defineEmits(['select-order', 'open-drawer'])

const viewLabel = computed(() => {
  if (props.viewMode === 'seller') return '卖家视图'
  if (props.viewMode === 'all') return '全部视图'
  return '买家视图'
})

const selectRow = (row) => {
  emit('select-order', row)
}

const emitOpen = (drawer, row) => {
  emit('select-order', row)
  emit('open-drawer', { drawer, order: row })
}

const canShip = (row) => String(row?.status || '').toUpperCase() === 'PENDING_SHIPMENT'
const canContact = (row) => Number(row?.buyerId || 0) > 0 || Number(row?.sellerId || 0) > 0
</script>

<style scoped>
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
