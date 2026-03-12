<template>
  <div class="dashboard-view">
    <div class="page-header">
      <h1>📊 仪表盘</h1>
      <p class="subtitle">系统运行状态和数据概览</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">💬</div>
        <div class="stat-info">
          <div class="stat-value">{{ activeSessions }}</div>
          <div class="stat-label">活跃会话数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalMessages }}</div>
          <div class="stat-label">消息总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ successRate }}%</div>
          <div class="stat-label">成功率</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">⏱️</div>
        <div class="stat-info">
          <div class="stat-value">{{ avgResponseTime }}ms</div>
          <div class="stat-label">平均响应时间</div>
        </div>
      </div>
    </div>

    <div class="content-placeholder">
      <div class="placeholder-icon">📊</div>
      <h2>功能开发中...</h2>
      <p>仪表盘功能正在紧张开发中，敬请期待！</p>
      <p class="coming-soon">预计功能：会话统计、流量分析、响应时长图表等</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMonitoringStats, getActiveSessionCount } from '@/api'

const activeSessions = ref(0)
const totalMessages = ref(0)
const successRate = ref(100)
const avgResponseTime = ref(0)

async function fetchStats() {
  try {
    const [statsData, sessionData] = await Promise.all([
      getMonitoringStats(),
      getActiveSessionCount()
    ])

    activeSessions.value = sessionData.activeSessions || 0

    // 解析监控数据
    try {
      const stats = JSON.parse(statsData)
      totalMessages.value = stats.totalMessages || 0
      successRate.value = stats.successRate || 100
      avgResponseTime.value = stats.avgResponseTime || 0
    } catch (e) {
      console.warn('Failed to parse stats:', e)
    }
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard-view {
  padding: 30px;
}

.page-header {
  margin-bottom: 30px;
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

.content-placeholder {
  background: white;
  padding: 60px 20px;
  border-radius: 15px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.content-placeholder h2 {
  color: #333;
  margin-bottom: 10px;
}

.content-placeholder p {
  color: #666;
}

.coming-soon {
  margin-top: 20px;
  font-size: 14px;
  color: #999;
}
</style>
