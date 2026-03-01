<template>
  <el-card>
    <template #header><h3 style="margin:0;">我的纠纷</h3></template>
    <el-form inline>
      <el-form-item label="订单ID"><el-input-number v-model="form.orderId" :min="1" /></el-form-item>
      <el-form-item label="原因"><el-input v-model="form.reason" style="width:320px" /></el-form-item>
      <el-form-item><el-button type="primary" @click="submit">提交纠纷</el-button></el-form-item>
    </el-form>
    <el-table :data="items">
      <el-table-column prop="id" label="纠纷ID" width="90" />
      <el-table-column prop="orderId" label="订单ID" width="90" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="resultNote" label="处理结果" width="220" />
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { disputeApi } from '../../api/modules'

const form = reactive({ orderId: 1, reason: '商品与描述不符' })
const items = ref([])

const load = async () => {
  items.value = await disputeApi.myList()
}

const submit = async () => {
  await disputeApi.create(form)
  ElMessage.success('纠纷已提交')
  await load()
}

onMounted(load)
</script>
