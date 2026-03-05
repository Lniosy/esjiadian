<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>订单工作台</h3>
          <div class="action-group">
            <el-select v-model="orderFlow.viewMode" style="width: 130px" @change="refreshAll">
              <el-option label="买家视图" value="buyer" />
              <el-option label="卖家视图" value="seller" />
              <el-option label="全部" value="all" />
            </el-select>
            <el-button :loading="loading" @click="refreshAll">刷新数据</el-button>
          </div>
        </div>
      </template>

      <div class="kpi-grid">
        <article class="kpi-item">
          <div class="kpi-label">订单总数</div>
          <div class="kpi-value">{{ orders.length }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">购物车商品</div>
          <div class="kpi-value">{{ cartItems.length }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">地址数</div>
          <div class="kpi-value">{{ addresses.length }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">当前上下文订单</div>
          <div class="kpi-value">{{ orderFlow.selectedOrder?.id || '-' }}</div>
        </article>
      </div>
    </el-card>

    <el-card class="page-card">
      <el-tabs v-model="orderFlow.activeTab" class="workbench-tabs">
        <el-tab-pane label="订单面板" name="orders">
          <OrderPanelTab
            :orders="orders"
            :view-mode="orderFlow.viewMode"
            @select-order="orderFlow.setSelectedOrder"
            @open-drawer="handleOpenDrawer"
            @success="handleActionSuccess"
          />
        </el-tab-pane>

        <el-tab-pane label="物流与履约" name="fulfillment">
          <FulfillmentTab
            :selected-order="orderFlow.selectedOrder"
            @open-drawer="handleOpenDrawer"
          />
        </el-tab-pane>

        <el-tab-pane label="售后服务" name="afterSales">
          <AfterSalesTab
            :selected-order="orderFlow.selectedOrder"
            @open-drawer="handleOpenDrawer"
          />
        </el-tab-pane>

        <el-tab-pane label="地址与结算" name="checkout">
          <CheckoutTab
            :addresses="addresses"
            :cart-items="cartItems"
            :selected-address-id="orderFlow.selectedAddressId"
            @refresh="refreshAll"
            @update:selected-address-id="updateSelectedAddress"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <OrderDetailDrawer
      v-model="orderFlow.drawers.detail"
      :order="orderFlow.selectedOrder"
      :view-mode="orderFlow.viewMode"
      @open-drawer="handleOpenDrawer"
      @success="handleActionSuccess"
    />

    <ShipmentDrawer
      v-model="orderFlow.drawers.shipment"
      :order="orderFlow.selectedOrder"
      @success="handleActionSuccess"
    />

    <RefundDrawer
      v-model="orderFlow.drawers.refund"
      :order="orderFlow.selectedOrder"
      :view-mode="orderFlow.viewMode"
      @success="handleActionSuccess"
    />

    <DisputeDrawer
      v-model="orderFlow.drawers.dispute"
      :order="orderFlow.selectedOrder"
      @success="handleActionSuccess"
    />

    <ReturnLogisticsDrawer
      v-model="orderFlow.drawers.returnLogistics"
      :order="orderFlow.selectedOrder"
      @success="handleActionSuccess"
    />

    <QuickChatDrawer
      v-model="orderFlow.drawers.quickChat"
      :order="orderFlow.selectedOrder"
      @success="handleActionSuccess"
    />

    <EvaluateDialog
      v-model="orderFlow.drawers.evaluate"
      :order="orderFlow.selectedOrder"
      @success="handleActionSuccess"
    />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi, userApi } from '../../api/modules'
import { useOrderFlowStore } from '../../stores/orderFlow'
import DisputeDrawer from './drawers/DisputeDrawer.vue'
import OrderDetailDrawer from './drawers/OrderDetailDrawer.vue'
import QuickChatDrawer from './drawers/QuickChatDrawer.vue'
import RefundDrawer from './drawers/RefundDrawer.vue'
import ReturnLogisticsDrawer from './drawers/ReturnLogisticsDrawer.vue'
import ShipmentDrawer from './drawers/ShipmentDrawer.vue'
import EvaluateDialog from './dialogs/EvaluateDialog.vue'
import AfterSalesTab from './tabs/AfterSalesTab.vue'
import CheckoutTab from './tabs/CheckoutTab.vue'
import FulfillmentTab from './tabs/FulfillmentTab.vue'
import OrderPanelTab from './tabs/OrderPanelTab.vue'

const orderFlow = useOrderFlowStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const orders = ref([])
const addresses = ref([])
const cartItems = ref([])
const validTabs = new Set(['orders', 'fulfillment', 'afterSales', 'checkout'])

const refreshAll = async () => {
  loading.value = true
  try {
    const [orderResp, addressResp, cartResp] = await Promise.all([
      orderApi.listOrders(orderFlow.viewMode),
      userApi.addresses().catch(() => []),
      orderApi.cartList().catch(() => [])
    ])

    orders.value = orderResp || []
    addresses.value = addressResp || []
    cartItems.value = cartResp || []

    const selectedId = Number(orderFlow.selectedOrder?.id || 0)
    if (selectedId > 0) {
      const latest = orders.value.find((it) => Number(it.id) === selectedId) || null
      if (latest) {
        orderFlow.setSelectedOrder(latest)
      } else if (orders.value.length) {
        orderFlow.setSelectedOrder(orders.value[0])
      } else {
        orderFlow.setSelectedOrder(null)
      }
    } else if (orders.value.length) {
      orderFlow.setSelectedOrder(orders.value[0])
    }
    const hasSelectedAddress = addresses.value.some((it) => Number(it.id) === Number(orderFlow.selectedAddressId || 0))
    if ((!orderFlow.selectedAddressId || !hasSelectedAddress) && addresses.value.length) {
      orderFlow.setSelectedAddressId(addresses.value[0].id)
    }
    if (!addresses.value.length) {
      orderFlow.setSelectedAddressId(null)
    }
  } catch {
    ElMessage.error('订单工作台数据加载失败')
  } finally {
    loading.value = false
  }
}

const handleOpenDrawer = ({ drawer, order }) => {
  if (!drawer) return
  if (drawer !== 'detail') {
    orderFlow.closeDrawer('detail')
  }
  orderFlow.openDrawer(drawer, order)
}

const handleActionSuccess = async () => {
  await refreshAll()
}

const updateSelectedAddress = (id) => {
  orderFlow.setSelectedAddressId(id)
}

onMounted(() => {
  const tab = String(route.query.tab || '')
  if (validTabs.has(tab)) {
    orderFlow.setActiveTab(tab)
  }
  refreshAll().catch(() => {})
})

watch(
  () => route.query.tab,
  (tab) => {
    const next = String(tab || '')
    if (validTabs.has(next) && next !== orderFlow.activeTab) {
      orderFlow.setActiveTab(next)
    }
  }
)

watch(
  () => orderFlow.activeTab,
  (tab) => {
    const q = String(route.query.tab || '')
    if (q === tab) return
    router.replace({ query: { ...route.query, tab } })
  }
)
</script>

<style scoped>
.workbench-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
