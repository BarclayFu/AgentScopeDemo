<template>
  <div class="dashboard-view">
    <div class="page-header">
      <h1>📊 仪表盘</h1>
      <p class="subtitle">系统运行摘要、健康状态与最近一次检查结果</p>
      <div class="page-actions">
        <button class="refresh-btn" :disabled="isLoading" @click="fetchStats">
          {{ isLoading ? '刷新中...' : '手动刷新' }}
        </button>
        <span class="checked-at">{{ checkedAtText }}</span>
      </div>
    </div>

    <div class="stats-grid">
      <div v-for="card in statsCards" :key="card.label" class="stat-card">
        <div class="stat-icon">{{ card.icon }}</div>
        <div class="stat-info">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="status-panel">
      <div class="status-badge" :class="serviceStatus.status === 'UP' ? 'up' : 'down'">
        <span class="status-dot"></span>
        <span>{{ serviceStatus.service || 'Customer Service Agent' }} / {{ serviceStatus.status || 'UNKNOWN' }}</span>
      </div>
      <div class="status-meta">
        <span>最近消息: {{ formatDate(summary.lastMessageAt) }}</span>
        <span>最近错误: {{ formatDate(summary.lastErrorAt) }}</span>
      </div>
    </div>

    <div v-if="errorMessage" class="state-card error-state">
      <h2>监控数据加载失败</h2>
      <p>{{ errorMessage }}</p>
      <button class="refresh-btn" @click="fetchStats">重试</button>
    </div>

    <div v-else-if="isLoading && !hasLoadedOnce" class="state-card">
      <h2>正在加载仪表盘</h2>
      <p>系统正在拉取最新监控数据，请稍候。</p>
    </div>

    <div v-else-if="isEmpty" class="state-card">
      <h2>暂无监控数据</h2>
      <p>系统还没有处理过消息，等第一轮对话发生后，这里会显示真实指标。</p>
    </div>

    <div v-else class="details-grid">
      <div class="detail-card">
        <h2>运行摘要</h2>
        <p>当前活跃会话数会随着用户建立独立上下文而变化。</p>
        <p>平均响应时间基于已完成消息统计，用于快速判断主链路是否变慢。</p>
      </div>
      <div class="detail-card">
        <h2>建议关注</h2>
        <p v-if="summary.errorCount > 0">最近发生过错误，建议结合日志排查工具调用或模型链路。</p>
        <p v-else>当前未记录错误，可以继续观察响应时延和工具调用增长趋势。</p>
        <p>如果需要更强的运维监控，可以在后续阶段接入 ARMS 或 OpenTelemetry。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getActiveSessionCount, getMonitoringStats, getSystemStatus } from '@/api'

const REFRESH_INTERVAL_MS = 15000
const summary = ref({
  activeSessions: 0,
  totalMessages: 0,
  totalToolCalls: 0,
  errorCount: 0,
  avgResponseTimeMs: 0,
  lastMessageAt: null,
  lastErrorAt: null
})
const serviceStatus = ref({
  service: '',
  status: 'UNKNOWN',
  checkedAt: null
})
const checkedAt = ref(null)
const isLoading = ref(false)
const hasLoadedOnce = ref(false)
const errorMessage = ref('')
let refreshTimer = null

const statsCards = computed(() => ([
  { icon: '💬', label: '活跃会话数', value: summary.value.activeSessions },
  { icon: '📝', label: '消息总数', value: summary.value.totalMessages },
  { icon: '🛠️', label: '工具调用数', value: summary.value.totalToolCalls },
  { icon: '⚠️', label: '错误数', value: summary.value.errorCount },
  { icon: '⏱️', label: '平均响应时间', value: `${summary.value.avgResponseTimeMs}ms` }
]))

const isEmpty = computed(() => summary.value.totalMessages === 0 && summary.value.totalToolCalls === 0)

const checkedAtText = computed(() => {
  if (!checkedAt.value) {
    return '尚未检查'
  }
  return `最近检查: ${formatDate(checkedAt.value)}`
})

async function fetchStats() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const [statsData, statusData, sessionData] = await Promise.all([
      getMonitoringStats(),
      getSystemStatus(),
      getActiveSessionCount()
    ])

    summary.value = parseSummary(statsData, sessionData)
    checkedAt.value = statsData?.checkedAt || statusData?.checkedAt || Date.now()
    serviceStatus.value = parseStatus(statusData)
    hasLoadedOnce.value = true
  } catch (error) {
    console.error('Failed to fetch stats:', error)
    errorMessage.value = error?.message || '请检查后端服务或监控接口是否可用。'
  } finally {
    isLoading.value = false
  }
}

function parseSummary(statsData, sessionData) {
  if (statsData?.summary) {
    return {
      ...summary.value,
      ...statsData.summary
    }
  }

  if (typeof statsData === 'string') {
    const messageMatch = statsData.match(/Total Messages:\s*(\d+)/i)
    const toolMatch = statsData.match(/Total Tool Calls:\s*(\d+)/i)
    return {
      ...summary.value,
      activeSessions: sessionData?.activeSessions || 0,
      totalMessages: Number.parseInt(messageMatch?.[1] || '0', 10),
      totalToolCalls: Number.parseInt(toolMatch?.[1] || '0', 10),
      errorCount: 0,
      avgResponseTimeMs: 0,
      lastMessageAt: null,
      lastErrorAt: null
    }
  }

  return {
    ...summary.value,
    activeSessions: sessionData?.activeSessions || 0
  }
}

function parseStatus(statusData) {
  if (statusData && typeof statusData === 'object') {
    return {
      service: statusData.service || 'Customer Service Agent',
      status: statusData.status || 'UNKNOWN',
      checkedAt: statusData.checkedAt || Date.now()
    }
  }

  if (typeof statusData === 'string') {
    return {
      service: 'Customer Service Agent',
      status: statusData.toLowerCase().includes('running') ? 'UP' : 'UNKNOWN',
      checkedAt: Date.now()
    }
  }

  return {
    service: 'Customer Service Agent',
    status: 'UNKNOWN',
    checkedAt: Date.now()
  }
}

function formatDate(timestamp) {
  if (!timestamp) {
    return '暂无'
  }
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  fetchStats()
  refreshTimer = window.setInterval(fetchStats, REFRESH_INTERVAL_MS)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.dashboard-view {
  flex: 1;
  width: 100%;
  min-width: 0;
  padding: 30px;
  overflow-y: auto;
}

.page-header {
  margin-bottom: 30px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0 0 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refresh-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  background: #667eea;
  color: white;
  cursor: pointer;
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.checked-at {
  font-size: 13px;
  color: #667085;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.stat-card {
  background: white;
  padding: 25px;
  border-radius: 15px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  font-size: 40px;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
}

.status-panel {
  background: white;
  border-radius: 15px;
  padding: 20px 24px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  padding: 8px 14px;
  font-weight: 600;
}

.status-badge.up {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.status-badge.down {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
}

.status-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  color: #667085;
  font-size: 13px;
}

.state-card,
.detail-card {
  background: white;
  padding: 28px 24px;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.state-card {
  text-align: center;
}

.state-card h2,
.detail-card h2 {
  color: #333;
  margin-bottom: 10px;
}

.state-card p,
.detail-card p {
  color: #666;
  line-height: 1.7;
}

.error-state {
  border: 1px solid rgba(220, 38, 38, 0.12);
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}
</style>
