<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>短信通知日志（模拟）</h3>
          <el-button @click="loadSmsLogs">刷新</el-button>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="smsLogs">
        <el-table-column label="时间" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column label="类型" width="110">
          <template #default="scope">
            {{ notifyTypeText(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户编号" width="90" />
        <el-table-column prop="phoneMasked" label="手机号" width="130" />
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="通道" width="100">
          <template #default="scope">
            {{ providerText(scope.row.provider) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            {{ sendStatusText(scope.row.status) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>关键操作审计日志</h3>
          <el-button @click="loadOperationLogs">刷新</el-button>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="operationLogs">
        <el-table-column label="时间" width="180">
          <template #default="scope">
            {{ formatDateTimeText(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="operatorId" label="操作人编号" width="90" />
        <el-table-column label="角色" width="90">
          <template #default="scope">
            {{ roleText(scope.row.operatorRole) }}
          </template>
        </el-table-column>
        <el-table-column label="动作" width="210">
          <template #default="scope">
            {{ opActionText(scope.row.action) }}
          </template>
        </el-table-column>
        <el-table-column label="目标类型" width="100">
          <template #default="scope">
            {{ targetTypeText(scope.row.targetType) }}
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标编号" width="100" />
        <el-table-column prop="detail" label="详情" />
      </el-table>
    </el-card>

    <el-card class="page-card">
      <template #header><h3>纠纷处理</h3></template>
      <el-table empty-text="暂无数据" :data="disputes">
        <el-table-column prop="id" label="纠纷编号" width="90" />
        <el-table-column prop="orderId" label="订单编号" width="90" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag>{{ disputeStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultNote" label="结果" width="220" />
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="success" @click="resolve(scope.row, 'RESOLVED')">结案</el-button>
              <el-button size="small" @click="resolve(scope.row, 'PROCESSING')">处理中</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { disputeApi, adminApi } from '../../api/modules'
import { disputeStatusText, formatDateTimeText, notifyTypeText, opActionText, providerText, roleText, sendStatusText, targetTypeText } from '../../utils/display'

const disputes = ref([])
const smsLogs = ref([])
const operationLogs = ref([])

const loadDisputes = async () => {
  disputes.value = await disputeApi.all()
}

const loadSmsLogs = async () => {
  smsLogs.value = await adminApi.smsLogs({ limit: 100 })
}

const loadOperationLogs = async () => {
  operationLogs.value = await adminApi.operationLogs(100)
}

const resolve = async (row, status) => {
  await disputeApi.resolve(row.id, { status, resultNote: status === 'RESOLVED' ? '平台已处理' : '已介入处理中' })
  await loadDisputes()
}

onMounted(async () => {
  try {
    await loadSmsLogs()
    await loadOperationLogs()
    await loadDisputes()
  } catch {}
})
</script>
