<template>
  <div class="settings-view">
    <div class="page-header">
      <h1>⚙️ 系统设置</h1>
      <p class="subtitle">保存前端操作偏好，并应用到聊天页默认行为</p>
    </div>

    <div class="settings-section">
      <h2>API配置</h2>
      <div class="setting-item">
        <label>API地址</label>
        <input
          type="text"
          v-model="form.apiBaseUrl"
          placeholder="http://localhost:8080 或留空走同源代理"
          class="setting-input"
        />
        <p class="setting-help">支持填写服务根地址；若已包含 <code>/api</code>，系统会自动兼容。</p>
      </div>
    </div>

    <div class="settings-section">
      <h2>聊天设置</h2>
      <div class="setting-item">
        <label>默认用户ID</label>
        <select v-model="form.defaultUserId" class="setting-select">
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
            v-model="form.defaultStreamInterval"
            min="0"
            max="100"
          />
          <span>{{ form.defaultStreamInterval }}ms</span>
        </div>
      </div>
    </div>

    <div class="settings-actions">
      <button class="secondary-btn" @click="resetToDefaults">恢复默认值</button>
      <button class="primary-btn" :disabled="!isDirty" @click="saveAllSettings">保存设置</button>
    </div>

    <p v-if="feedbackMessage" class="feedback-message">{{ feedbackMessage }}</p>

    <div class="settings-section">
      <h2>关于</h2>
      <div class="about-info">
        <p><strong>智能客服系统</strong></p>
        <p>版本: 1.0.0</p>
        <p>基于 AgentScope Java 框架</p>
        <p>当前 API 模式: {{ settingsStore.hasCustomApiBaseUrl ? '自定义地址' : '环境默认地址' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useSettingsStore } from '@/stores/settings'
import { normalizeSettings } from '@/config/runtimeSettings'

const settingsStore = useSettingsStore()
const chatStore = useChatStore()

const form = reactive({ ...settingsStore.settings })
const feedbackMessage = ref('')

const isDirty = computed(() => {
  const current = normalizeSettings(form)
  return JSON.stringify(current) !== JSON.stringify(settingsStore.settings)
})

function saveAllSettings() {
  const saved = settingsStore.saveSettings(form)
  Object.assign(form, saved)
  chatStore.applySettings(saved)
  feedbackMessage.value = '设置已保存，聊天页默认值已更新。'
}

function resetToDefaults() {
  const defaults = settingsStore.resetSettings()
  Object.assign(form, defaults)
  chatStore.applySettings(defaults)
  feedbackMessage.value = '已恢复默认设置。'
}
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

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
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

.setting-help {
  margin-top: 8px;
  font-size: 12px;
  color: #7c8796;
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

.primary-btn,
.secondary-btn {
  border: none;
  border-radius: 10px;
  padding: 12px 18px;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.primary-btn {
  background: #667eea;
  color: white;
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.18);
}

.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.secondary-btn {
  background: #eef2ff;
  color: #4052b5;
}

.primary-btn:not(:disabled):hover,
.secondary-btn:hover {
  transform: translateY(-1px);
}

.feedback-message {
  margin-bottom: 20px;
  color: #2f855a;
  font-size: 14px;
}

.about-info {
  color: #666;
  line-height: 1.8;
}

.about-info p {
  margin: 5px 0;
}
</style>
