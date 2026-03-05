const mapText = (value, map, fallback = '-') => {
  const key = value === null || value === undefined ? '' : String(value)
  return map[key] || key || fallback
}

export const orderStatusText = (value) => mapText(value, {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIPMENT: '待发货',
  PENDING_RECEIPT: '待收货',
  PENDING_REVIEW: '待评价',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  CLOSED: '已关闭',
  REFUNDING: '退款中',
  REFUNDED: '已退款'
})

export const refundStatusText = (value) => mapText(value, {
  NONE: '无',
  PENDING: '待处理',
  APPROVED: '已同意',
  REJECTED: '已拒绝',
  RETURN_REQUIRED: '待买家退货',
  BUYER_SHIPPED: '买家已寄回',
  FINISHED: '已完成',
  COMPLETED: '已完成',
  CANCELED: '已取消'
})

export const disputeStatusText = (value) => mapText(value, {
  OPEN: '待处理',
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已结案',
  REJECTED: '已驳回'
})

export const authStatusText = (value) => mapText(value, {
  APPROVED: '已认证',
  PENDING: '待审核',
  REJECTED: '已驳回',
  NOT_SUBMITTED: '未提交'
})

export const productStatusText = (value) => mapText(value, {
  ON_SHELF: '在售',
  OFF_SHELF: '已下架',
  SOLD: '已售出',
  DELETED: '已删除'
})

export const reportStatusText = (value) => mapText(value, {
  PENDING: '待处理',
  CONFIRMED: '已确认违规',
  REJECTED: '已驳回'
})

export const alertStatusText = (value) => mapText(value, {
  OPEN: '未确认',
  ACKED: '已确认',
  RESOLVED: '已恢复'
})

export const alertLevelText = (value) => mapText(value, {
  ERROR: '严重',
  WARN: '预警',
  CRITICAL: '严重',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  INFO: '提示'
})

export const yesNoText = (ok) => (ok ? '正常' : '异常')

export const roleText = (roles) => {
  const one = (v) => mapText(v, {
    ROLE_ADMIN: '管理员',
    ROLE_BUYER: '买家',
    ROLE_SELLER: '卖家'
  }, '普通用户')
  if (Array.isArray(roles)) return roles.map(one).join('、')
  return String(roles || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map(one)
    .join('、')
}

export const notifyTypeText = (value) => mapText(value, {
  MONITOR_ALERT: '监控告警',
  MONITOR_ALERT_ERROR: '监控严重告警',
  ORDER: '订单通知',
  CHAT: '聊天通知',
  DISPUTE: '纠纷通知',
  SYSTEM: '系统通知'
})

export const messageTypeText = (value) => mapText(value, {
  TEXT: '文本',
  IMAGE: '图片',
  PRODUCT_CARD: '商品卡片'
})

export const providerText = (value) => mapText(value, {
  MOCK: '模拟通道',
  SMS_MOCK: '模拟短信',
  INTERNAL: '站内'
})

export const sendStatusText = (value) => mapText(value, {
  SUCCESS: '成功',
  FAILED: '失败',
  PENDING: '处理中',
  SENT: '已发送'
})

export const opActionText = (value) => mapText(value, {
  RECOMPUTE_SELLER_SCORE: '重算卖家信誉分',
  ENABLE_USER: '启用用户',
  DISABLE_USER: '禁用用户',
  ACK_MONITOR_ALERT: '确认监控告警',
  RESOLVE_DISPUTE: '处理纠纷',
  RESOLVE_CHAT_REPORT: '处理聊天举报'
})

export const targetTypeText = (value) => mapText(value, {
  SYSTEM: '系统',
  USER: '用户',
  ORDER: '订单',
  CHAT: '聊天',
  DISPUTE: '纠纷'
})

export const logisticsStatusText = (value) => mapText(value, {
  CREATED: '已创建',
  SHIPPED: '已发货',
  IN_TRANSIT: '运输中',
  OUT_FOR_DELIVERY: '派送中',
  DELIVERED: '已签收',
  SIGNED: '已签收',
  RETURNING: '退回中',
  RETURNED: '已退回',
  EXCEPTION: '运输异常'
})

export const formatDateTimeText = (value) => {
  const ts = Number(value || 0)
  if (!ts) return '-'
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return '-'
  const yyyy = d.getFullYear()
  const mm = `${d.getMonth() + 1}`.padStart(2, '0')
  const dd = `${d.getDate()}`.padStart(2, '0')
  const hh = `${d.getHours()}`.padStart(2, '0')
  const mi = `${d.getMinutes()}`.padStart(2, '0')
  const ss = `${d.getSeconds()}`.padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`
}

export const formatDurationText = (ms) => {
  const total = Math.max(0, Math.floor(Number(ms || 0) / 1000))
  const d = Math.floor(total / 86400)
  const h = Math.floor((total % 86400) / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  if (d > 0) return `${d}天${h}小时${m}分`
  if (h > 0) return `${h}小时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

export const remainDurationText = (expireAt) => {
  const ts = Number(expireAt || 0)
  if (!ts) return '-'
  const left = ts - Date.now()
  if (left <= 0) return '已过期'
  return formatDurationText(left)
}
