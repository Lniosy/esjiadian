import { defineStore } from 'pinia'

const defaultDrawers = () => ({
  detail: false,
  shipment: false,
  refund: false,
  dispute: false,
  returnLogistics: false,
  quickChat: false,
  evaluate: false
})

const readSelectedAddressId = () => {
  const raw = Number(localStorage.getItem('selectedAddressId') || 0)
  return raw > 0 ? raw : null
}

export const useOrderFlowStore = defineStore('order-flow', {
  state: () => ({
    activeTab: 'orders',
    viewMode: 'buyer',
    selectedOrder: null,
    selectedAddressId: readSelectedAddressId(),
    drawers: defaultDrawers()
  }),
  actions: {
    setActiveTab(tab) {
      this.activeTab = tab
    },
    setViewMode(mode) {
      this.viewMode = mode
    },
    setSelectedOrder(order) {
      this.selectedOrder = order || null
    },
    setSelectedAddressId(id) {
      const next = Number(id || 0)
      this.selectedAddressId = next > 0 ? next : null
      if (this.selectedAddressId) {
        localStorage.setItem('selectedAddressId', String(this.selectedAddressId))
      } else {
        localStorage.removeItem('selectedAddressId')
      }
    },
    openDrawer(name, order = null) {
      if (!(name in this.drawers)) return
      if (order) this.selectedOrder = order
      this.drawers[name] = true
    },
    closeDrawer(name) {
      if (!(name in this.drawers)) return
      this.drawers[name] = false
    },
    closeAllDrawers() {
      this.drawers = defaultDrawers()
    }
  }
})
