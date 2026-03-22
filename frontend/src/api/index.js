import axios from 'axios'
import { buildApiUrl } from '@/config/runtimeSettings'

// 创建axios实例
const api = axios.create({
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    if (config.url) {
      config.url = buildApiUrl(config.url)
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// ==================== 聊天API ====================

/**
 * 发送聊天消息（非流式）
 * @param {string} userId - 用户ID
 * @param {string} message - 消息内容
 */
export async function sendMessage(userId, message) {
  return api.post('/api/chat/message', { userId, message })
}

/**
 * 获取流式响应（SSE）
 * @param {string} userId - 用户ID
 * @param {string} message - 消息内容
 * @param {function} onMessage - 消息回调
 * @param {function} onError - 错误回调
 * @param {number} streamInterval - 流式间隔
 * @returns {EventSource}
 */
export function createStreamChat(userId, message, onMessage, onError, streamInterval = 30) {
  const params = new URLSearchParams({
    userId,
    message,
    stream: 'true',
    streamInterval: streamInterval.toString()
  })

  const eventSource = new EventSource(buildApiUrl(`/api/chat/stream?${params}`))

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]') {
      eventSource.close()
      if (onMessage) onMessage({ done: true })
    } else if (data.startsWith('{') || data.startsWith('error')) {
      try {
        const parsed = JSON.parse(data)
        eventSource.close()
        if (onError) onError(parsed.error || 'Unknown error')
      } catch (e) {
        eventSource.close()
        if (onError) onError(data)
      }
    } else {
      if (onMessage) onMessage({ content: data, done: false })
    }
  }

  eventSource.onerror = (error) => {
    eventSource.close()
    // SSE连接错误，传递一个友好的错误信息
    if (onError) onError('SSE连接失败，请检查网络或服务端是否正常运行')
  }

  return eventSource
}

/**
 * 获取活跃会话数量
 */
export async function getActiveSessionCount() {
  return api.get('/api/chat/sessions/count')
}

/**
 * 清除用户会话
 * @param {string} userId - 用户ID
 */
export async function clearUserSession(userId) {
  return api.delete(`/api/chat/session/${userId}`)
}

/**
 * 健康检查
 */
export async function healthCheck() {
  return api.get('/api/chat/health')
}

// ==================== 监控API ====================

/**
 * 获取监控统计
 */
export async function getMonitoringStats() {
  return api.get('/api/monitoring/stats')
}

/**
 * 重置监控统计
 */
export async function resetMonitoringStats() {
  return api.post('/api/monitoring/reset')
}

/**
 * 获取系统状态
 */
export async function getSystemStatus() {
  return api.get('/api/monitoring/status')
}

// ==================== 知识库管理 API ====================

export async function getKnowledgeEntries() {
  return api.get('/api/knowledge/entries')
}

export async function createKnowledgeEntry(payload) {
  return api.post('/api/knowledge/entries', payload)
}

export async function deleteKnowledgeEntry(entryId) {
  return api.delete(`/api/knowledge/entries/${entryId}`)
}

export async function rebuildKnowledgeBase() {
  return api.post('/api/knowledge/rebuild')
}

export async function getKnowledgeStatus() {
  return api.get('/api/knowledge/status')
}

// ==================== 图谱API ====================

/**
 * 获取图谱统计
 */
export async function getGraphStats() {
  return api.get('/api/graph/stats')
}

/**
 * 获取图谱节点
 * @param {number} limit - 限制数量
 * @param {number} offset - 偏移量
 */
export async function getGraphNodes(limit = 100, offset = 0) {
  return api.get('/api/graph/nodes', { params: { limit, offset } })
}

/**
 * 获取图谱边
 * @param {number} limit - 限制数量
 * @param {number} offset - 偏移量
 */
export async function getGraphEdges(limit = 100, offset = 0) {
  return api.get('/api/graph/edges', { params: { limit, offset } })
}

/**
 * 清除图谱
 */
export async function clearGraph() {
  return api.post('/api/graph/clear')
}

// ==================== 对比实验API ====================

/**
 * 对比搜索
 * @param {string} query - 查询内容
 * @param {number} limit - 限制数量
 */
export async function compareSearch(query, limit = 5) {
  return api.post('/api/compare/search', { query, limit })
}

/**
 * 预览三元组抽取
 * @param {string} title - 标题
 * @param {string} content - 内容
 */
export async function previewTripleExtraction(title, content) {
  return api.post('/api/compare/preview', { title, content })
}

export default api
