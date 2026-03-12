<template>
  <div class="settings-view">
    <div class="page-header">
      <h1>⚙️ 系统设置</h1>
      <p class="subtitle">配置系统参数</p>
    </div>

    <div class="settings-section">
      <h2>API配置</h2>
      <div class="setting-item">
        <label>API地址</label>
        <input
          type="text"
          v-model="apiBaseUrl"
          placeholder="http://localhost:8080"
          class="setting-input"
        />
      </div>
    </div>

    <div class="settings-section">
      <h2>聊天设置</h2>
      <div class="setting-item">
        <label>默认用户ID</label>
        <select v-model="defaultUserId" class="setting-select">
          <option value="user001">user001</option>
          <option value="user002">user002</option>
          <option value="user003">user003</option>
          <option value="webuser001">webuser001</option>
        </select>
      </div>
      <div class="setting-item">
        <label>默认打字速度 (ms)</label>
        <div class="setting-range">
          <input
            type="range"
            v-model="defaultStreamInterval"
            min="0"
            max="100"
          />
          <span>{{ defaultStreamInterval }}ms</span>
        </div>
      </div>
    </div>

    <div class="settings-section">
      <h2>关于</h2>
      <div class="about-info">
        <p><strong>智能客服系统</strong></p>
        <p>版本: 1.0.0</p>
        <p>基于 AgentScope Java 框架</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const apiBaseUrl = ref(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
const defaultUserId = ref('user001')
const defaultStreamInterval = ref(30)
</script>

<style scoped>
.settings-view {
  padding: 30px;
  max-width: 800px;
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

.settings-section {
  background: white;
  padding: 25px;
  border-radius: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.settings-section h2 {
  font-size: 18px;
  color: #333;
  margin: 0 0 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e1e8ed;
}

.setting-item {
  margin-bottom: 20px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-item label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.setting-input {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.setting-input:focus {
  border-color: #667eea;
}

.setting-select {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
}

.setting-select:focus {
  border-color: #667eea;
}

.setting-range {
  display: flex;
  align-items: center;
  gap: 15px;
}

.setting-range input[type="range"] {
  flex: 1;
  accent-color: #667eea;
}

.setting-range span {
  min-width: 50px;
  color: #666;
}

.about-info {
  color: #666;
  line-height: 1.8;
}

.about-info p {
  margin: 5px 0;
}
</style>
