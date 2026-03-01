<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header><div class="page-head"><h3>发起纠纷</h3></div></template>
      <el-form inline class="actions-row">
        <el-form-item label="选择订单">
          <el-select v-model="form.orderId" placeholder="请选择订单" style="width: 320px">
            <el-option
              v-for="o in candidateOrders"
              :key="o.id"
              :value="o.id"
              :label="`订单${o.orderNo || o.id} · ${orderStatusText(o.status)} · 商品${o.productId}`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" style="width:320px" /></el-form-item>
        <el-form-item><el-button type="primary" :disabled="!form.orderId" @click="submit">提交纠纷</el-button></el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon style="margin-top: 8px;">
        纠纷需基于已存在订单发起，不支持手动输入订单编号。
      </el-alert>
    </el-card>

    <el-card class="page-card">
      <template #header><div class="page-head"><h3>我的纠纷记录</h3></div></template>
      <el-table empty-text="暂无数据" :data="items">
        <el-table-column prop="id" label="纠纷编号" width="90" />
        <el-table-column prop="orderId" label="订单编号" width="90" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag>{{ disputeStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultNote" label="处理结果" width="220" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { disputeApi, orderApi } from '../../api/modules'
import { disputeStatusText, orderStatusText } from '../../utils/display'

const route = useRoute()
const form = reactive({ orderId: null, reason: '商品与描述不符' })
const items = ref([])
const orders = ref([])

const blockedOrderIdSet = computed(() => {
  const set = new Set()
  items.value
    .filter((d) => ['OPEN', 'PENDING', 'PROCESSING'].includes(String(d.status || '').toUpperCase()))
    .forEach((d) => set.add(Number(d.orderId)))
  return set
})

const candidateOrders = computed(() => {
  const blocked = blockedOrderIdSet.value
  return (orders.value || []).filter((o) => {
    if (!o || !o.id) return false
    if (blocked.has(Number(o.id))) return false
    return !['PENDING_PAYMENT', 'CANCELLED', 'CLOSED'].includes(String(o.status || '').toUpperCase())
  })
})

const load = async () => {
  items.value = await disputeApi.myList()
  orders.value = await orderApi.listOrders('all')
  const qOrderId = Number(route.query.orderId || 0)
  if (qOrderId > 0 && candidateOrders.value.some((o) => Number(o.id) === qOrderId)) {
    form.orderId = qOrderId
    return
  }
  if (!form.orderId || !candidateOrders.value.some((o) => Number(o.id) === Number(form.orderId))) {
    form.orderId = candidateOrders.value.length ? candidateOrders.value[0].id : null
  }
}

const submit = async () => {
  if (!form.orderId) {
    ElMessage.warning('请先选择订单')
    return
  }
  await disputeApi.create(form)
  ElMessage.success('纠纷已提交')
  await load()
}

onMounted(() => {
  load().catch(() => {})
})
</script>
