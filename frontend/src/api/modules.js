import http from './http'

export const authApi = {
  sendCode: (payload) => http.post('/api/auth/code/send', payload),
  register: (payload) => http.post('/api/auth/register', payload),
  loginPassword: (payload) => http.post('/api/auth/login/password', payload),
  loginCode: (payload) => http.post('/api/auth/login/code', payload)
}

export const userApi = {
  me: () => http.get('/api/users/me'),
  publicProfile: (id) => http.get(`/api/users/${id}/public`),
  updateMe: (payload) => http.put('/api/users/me', payload),
  addresses: () => http.get('/api/users/me/addresses'),
  addAddress: (payload) => http.post('/api/users/me/addresses', payload),
  updateAddress: (id, payload) => http.put(`/api/users/me/addresses/${id}`, payload),
  deleteAddress: (id) => http.delete(`/api/users/me/addresses/${id}`),
  applyRealName: (payload) => http.post('/api/verification/real-name', payload),
  verificationStatus: () => http.get('/api/verification/status')
}

export const productApi = {
  list: (params) => http.get('/api/products', { params }),
  detail: (id) => http.get(`/api/products/${id}`),
  create: (payload) => http.post('/api/products', payload),
  update: (id, payload) => http.put(`/api/products/${id}`, payload),
  onShelf: (id) => http.post(`/api/products/${id}/on-shelf`),
  offShelf: (id) => http.post(`/api/products/${id}/off-shelf`)
}

export const shopApi = {
  myShop: () => http.get('/api/shops/me'),
  byUser: (userId) => http.get(`/api/shops/by-user/${userId}`),
  upsertMyShop: (payload) => http.put('/api/shops/me', payload),
  detail: (id) => http.get(`/api/shops/${id}`),
  overview: (id) => http.get(`/api/shops/${id}/overview`)
}

export const orderApi = {
  cartList: () => http.get('/api/cart/items'),
  addCart: (productId) => http.post('/api/cart/items', null, { params: { productId } }),
  updateCart: (payload) => http.put('/api/cart/items', payload),
  removeCart: (productId) => http.delete('/api/cart/items', { params: { productId } }),
  createOrder: (payload) => http.post('/api/orders', payload),
  listOrders: (view = 'buyer') => http.get('/api/orders', { params: { view } }),
  getOrder: (id) => http.get(`/api/orders/${id}`),
  cancel: (id) => http.post(`/api/orders/${id}/cancel`),
  ship: (id, payload) => http.post(`/api/orders/${id}/ship`, payload),
  confirmReceipt: (id) => http.post(`/api/orders/${id}/confirm-receipt`)
}

export const refundApi = {
  detail: (orderId) => http.get(`/api/orders/${orderId}/refund`),
  apply: (orderId, payload) => http.post(`/api/orders/${orderId}/refund/apply`, payload),
  approve: (orderId) => http.post(`/api/orders/${orderId}/refund/approve`),
  reject: (orderId, payload) => http.post(`/api/orders/${orderId}/refund/reject`, payload),
  shipReturn: (orderId, payload) => http.post(`/api/orders/${orderId}/refund/ship-return`, payload),
  confirmReturn: (orderId) => http.post(`/api/orders/${orderId}/refund/confirm-return`)
}

export const paymentApi = {
  alipay: (orderId) => http.post(`/api/payments/${orderId}/alipay`),
  wechat: (orderId) => http.post(`/api/payments/${orderId}/wechat`),
  callback: (channel, params) => http.post(`/api/payments/callback/${channel}`, null, { params })
}

export const logisticsApi = {
  save: (orderId, params) => http.post(`/api/orders/${orderId}/logistics`, null, { params }),
  tracks: (orderId) => http.get(`/api/orders/${orderId}/logistics/tracks`)
}

export const evaluationApi = {
  create: (orderId, payload) => http.post(`/api/orders/${orderId}/evaluations`, payload),
  shopRatingDetail: (shopId) => http.get(`/api/shops/${shopId}/rating-detail`)
}

export const disputeApi = {
  create: (payload) => http.post('/api/disputes', payload),
  myList: () => http.get('/api/disputes'),
  all: () => http.get('/api/admin/disputes'),
  resolve: (id, payload) => http.post(`/api/admin/disputes/${id}/resolve`, payload)
}

export const notificationApi = {
  list: () => http.get('/api/notifications'),
  read: (id) => http.post(`/api/notifications/${id}/read`),
  smsLogs: (limit = 30) => http.get('/api/notifications/sms-logs', { params: { limit } })
}

export const chatApi = {
  conversations: () => http.get('/api/chat/conversations'),
  messages: (conversationId) => http.get('/api/chat/messages', { params: { conversationId } }),
  send: (payload) => http.post('/api/chat/messages', payload),
  read: (payload) => http.post('/api/chat/read', payload),
  report: (id, payload) => http.post(`/api/chat/messages/${id}/report`, payload)
}

export const fileApi = {
  uploadImage: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/api/files/upload/image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const adminApi = {
  stats: () => http.get('/api/admin/stats/overview'),
  trends: (days = 7) => http.get('/api/admin/stats/trends', { params: { days } }),
  monitor: () => http.get('/api/admin/monitor'),
  monitorAlerts: (status = 'OPEN') => http.get('/api/admin/monitor/alerts', { params: { status } }),
  monitorSlowQueries: (limit = 20) => http.get('/api/admin/monitor/mysql/slow-queries', { params: { limit } }),
  ackMonitorAlert: (id) => http.post(`/api/admin/monitor/alerts/${id}/ack`),
  smsLogs: (params) => http.get('/api/admin/notifications/sms-logs', { params }),
  operationLogs: (limit = 100) => http.get('/api/admin/operation-logs', { params: { limit } }),
  chatReports: (params) => http.get('/api/admin/chat-reports', { params }),
  resolveChatReport: (id, payload) => http.post(`/api/admin/chat-reports/${id}/resolve`, payload),
  recomputeSellerScores: () => http.post('/api/admin/stats/recompute-seller-scores'),
  users: (params) => http.get('/api/admin/users', { params }),
  enableUser: (userId) => http.post(`/api/admin/users/${userId}/enable`),
  disableUser: (userId) => http.post(`/api/admin/users/${userId}/disable`),
  orders: () => http.get('/api/admin/orders')
}
