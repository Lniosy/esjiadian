<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>违规举报处理</h3>
          <div class="actions">
            <el-select v-model="reportFilter.status" style="width:130px;">
              <el-option label="待处理" value="PENDING" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-input v-model="reportFilter.keyword" placeholder="关键词筛选" style="width:220px;" />
            <el-button @click="loadChatReports">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="chatReports">
        <el-table-column prop="id" label="举报编号" width="90" />
        <el-table-column prop="messageId" label="消息编号" width="90" />
        <el-table-column prop="reporterName" label="举报人" width="120" />
        <el-table-column prop="reportedUserName" label="被举报人" width="120" />
        <el-table-column prop="reason" label="举报原因" width="180" />
        <el-table-column label="类型" width="90">
          <template #default="scope">
            {{ messageTypeText(scope.row.messageType) }}
          </template>
        </el-table-column>
        <el-table-column prop="messageContent" label="消息内容" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag>{{ reportStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="decisionNote" label="处理说明" width="160" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="danger" :disabled="scope.row.status !== 'PENDING'" @click="resolveChatReport(scope.row, 'CONFIRMED')">判定违规</el-button>
              <el-button size="small" :disabled="scope.row.status !== 'PENDING'" @click="resolveChatReport(scope.row, 'REJECTED')">驳回</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header><h3>用户管理</h3></template>
      <div class="actions" style="margin-bottom: 10px;">
        <el-input v-model="userFilter.keyword" placeholder="昵称/邮箱/手机号" style="width:220px;" />
        <el-select v-model="userFilter.enabled" placeholder="启用状态" style="width:120px;">
          <el-option label="全部" value="" />
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
        <el-select v-model="userFilter.role" placeholder="角色" style="width:140px;">
          <el-option label="全部" value="" />
          <el-option label="买家" value="ROLE_BUYER" />
          <el-option label="卖家" value="ROLE_SELLER" />
          <el-option label="管理员" value="ROLE_ADMIN" />
        </el-select>
        <el-button type="primary" @click="loadUsers">筛选</el-button>
      </div>
      <el-table empty-text="暂无数据" :data="users">
        <el-table-column prop="userId" label="用户编号" width="90" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column label="角色" width="180">
          <template #default="scope">
            {{ roleText(scope.row.roles) }}
          </template>
        </el-table-column>
        <el-table-column label="实名状态" width="130">
          <template #default="scope">
            {{ authStatusText(scope.row.authStatus) }}
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'danger'">{{ scope.row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="success" @click="enableUser(scope.row)" :disabled="scope.row.enabled">启用</el-button>
              <el-button size="small" type="danger" @click="disableUser(scope.row)" :disabled="!scope.row.enabled">禁用</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>订单管理</h3>
          <el-button type="primary" @click="exportOrders">导出订单数据</el-button>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="orders">
        <el-table-column prop="id" label="订单编号" width="90" />
        <el-table-column prop="orderNo" label="订单号" width="210" />
        <el-table-column prop="buyerId" label="买家编号" width="90" />
        <el-table-column prop="sellerId" label="卖家编号" width="90" />
        <el-table-column prop="productId" label="商品编号" width="90" />
        <el-table-column prop="paidAmount" label="金额" width="120" />
        <el-table-column label="状态" width="140">
          <template #default="scope">
            <el-tag>{{ orderStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/modules'
import { authStatusText, messageTypeText, orderStatusText, reportStatusText, roleText } from '../../utils/display'

const users = ref([])
const orders = ref([])
const chatReports = ref([])

const userFilter = reactive({
  keyword: '',
  enabled: '',
  role: ''
})

const reportFilter = reactive({
  status: 'PENDING',
  keyword: ''
})

const loadUsers = async () => {
  users.value = await adminApi.users({
    keyword: userFilter.keyword || null,
    enabled: userFilter.enabled === '' ? null : userFilter.enabled,
    role: userFilter.role || null
  })
}

const enableUser = async (row) => {
  await adminApi.enableUser(row.userId)
  ElMessage.success('用户已启用')
  await loadUsers()
}

const disableUser = async (row) => {
  await adminApi.disableUser(row.userId)
  ElMessage.success('用户已禁用')
  await loadUsers()
}

const loadChatReports = async () => {
  chatReports.value = await adminApi.chatReports({
    status: reportFilter.status,
    keyword: reportFilter.keyword || null
  })
}

const resolveChatReport = async (row, status) => {
  await adminApi.resolveChatReport(row.id, {
    status,
    note: status === 'CONFIRMED' ? '消息内容违规' : '证据不足，驳回举报'
  })
  ElMessage.success('举报处理完成')
  await loadChatReports()
}

const loadOrders = async () => {
  orders.value = await adminApi.orders()
}

const exportOrders = async () => {
  const token = localStorage.getItem('token') || ''
  const resp = await fetch('/api/admin/orders/export', {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
  if (!resp.ok) {
    throw new Error('导出失败')
  }
  const blob = await resp.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '订单数据.csv'
  a.click()
  window.URL.revokeObjectURL(url)
}

onMounted(async () => {
  try {
    await loadChatReports()
    await loadUsers()
    await loadOrders()
  } catch {}
})
</script>

<style scoped>
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
