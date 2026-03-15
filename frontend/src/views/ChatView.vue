<template>
  <div class="chat-view">
    <!-- 聊天头部 -->
    <header class="chat-header">
      <h1>💬 在线客服</h1>
      <div class="header-controls">
        <div class="connection-status" :class="connectionStatus">
          <span class="status-dot"></span>
          <span>{{ statusText }}</span>
        </div>
      </div>
    </header>

    <!-- 消息列表 -->
    <div class="messages-container" ref="messagesContainer">
      <!-- 欢迎消息 -->
      <div v-if="messages.length === 0" class="welcome-message">
        <div class="welcome-icon">🤖</div>
        <h2>你好！我是智能客服助手</h2>
        <p>请问有什么可以帮助您的？</p>
        <div class="example-queries">
          <h3>您可以尝试以下问题：</h3>
          <div
            v-for="(example, index) in exampleQueries"
            :key="index"
            class="example-item"
            @click="sendExample(example)"
          >
            {{ example }}
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-else class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message"
          :class="msg.role"
        >
          <div class="message-avatar">
            {{ msg.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-content">
            <div
              class="message-text"
              :class="{ streaming: msg.isStreaming }"
              v-html="formatMessage(msg.content)"
            ></div>
            <div v-if="msg.isStreaming" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
            <div class="message-time">
              {{ formatTime(msg.timestamp) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-container">
      <div class="input-controls">
        <div class="control-group">
          <label>用户ID:</label>
          <select v-model="currentUserId" class="user-select">
            <option value="user001">user001</option>
            <option value="user002">user002</option>
            <option value="user003">user003</option>
            <option value="webuser001">webuser001</option>
          </select>
        </div>
        <div class="control-group">
          <label>打字速度:</label>
          <input
            type="range"
            v-model="streamInterval"
            min="0"
            max="100"
            class="speed-slider"
          />
          <span class="speed-value">{{ streamInterval }}ms</span>
        </div>
        <button class="clear-btn" @click="clearChat">清空对话</button>
      </div>

      <div class="input-wrapper">
        <textarea
          v-model="inputMessage"
          @keydown.enter.exact.prevent="sendMessage"
          placeholder="输入您的问题..."
          class="message-input"
          :disabled="isStreaming"
        ></textarea>
        <button
          class="send-btn"
          @click="sendMessage"
          :disabled="!inputMessage.trim() || isStreaming"
        >
          {{ isStreaming ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useSettingsStore } from '@/stores/settings'
import { createStreamChat, getActiveSessionCount } from '@/api'

const chatStore = useChatStore()
const settingsStore = useSettingsStore()

// 响应式状态
const inputMessage = ref('')
const messagesContainer = ref(null)
const eventSource = ref(null)

// 从store获取状态
const messages = computed(() => chatStore.messages)
const currentUserId = computed({
  get: () => chatStore.currentUserId,
  set: (val) => chatStore.setUserId(val)
})
const streamInterval = computed({
  get: () => chatStore.streamInterval,
  set: (val) => chatStore.setStreamInterval(val)
})
const isStreaming = computed(() => chatStore.isStreaming)

// 示例问题
const exampleQueries = [
  '我想查询订单ORD001的状态',
  '我要为订单ORD002办理退款',
  '订单ORD003的物流状态如何？',
  '如何联系人工客服？',
  'iPhone 15 Pro有什么特性？'
]

// 连接状态
const connectionStatus = computed(() => {
  if (isStreaming.value) return 'streaming'
  return 'connected'
})

const statusText = computed(() => {
  if (isStreaming.value) return '正在接收响应...'
  return '已就绪'
})

// 发送示例问题
function sendExample(query) {
  inputMessage.value = query
  sendMessage()
}

// 发送消息
async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || isStreaming.value) return

  // 添加用户消息
  chatStore.addUserMessage(message)
  inputMessage.value = ''

  // 开始流式输出
  chatStore.startStreaming()
  chatStore.addAssistantMessage('', true)

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 创建SSE连接
  eventSource.value = createStreamChat(
    currentUserId.value,
    message,
    // onMessage
    (data) => {
      if (data.done) {
        chatStore.endStreaming()

        // 找到正在流式输出的消息，标记结束
        const currentContent = messages.value.find(m => m.role === 'assistant' && m.isStreaming)
        if (currentContent) {
          const index = messages.value.indexOf(currentContent)
          if (index !== -1) {
            messages.value[index] = {
              ...currentContent,
              isStreaming: false
            }
          }
        }

        // 刷新活跃会话数
        fetchActiveSessions()
      } else {
        const currentContent = messages.value.find(m => m.role === 'assistant' && m.isStreaming)
        if (currentContent) {
          chatStore.addAssistantMessage(currentContent.content + data.content, true)
        }
        nextTick(() => scrollToBottom())
      }
    },
    // onError
    (errorMsg) => {
      console.error('Stream error:', errorMsg)
      chatStore.endStreaming()

      // 找到正在流式输出的消息，标记结束并添加错误信息
      const currentContent = messages.value.find(m => m.role === 'assistant' && m.isStreaming)
      if (currentContent) {
        // 移除正在流式输出的标记
        const index = messages.value.indexOf(currentContent)
        if (index !== -1) {
          messages.value[index] = {
            ...currentContent,
            content: currentContent.content + `\n\n[错误: ${errorMsg}]`,
            isStreaming: false
          }
        }
      }
    },
    streamInterval.value
  )
}

// 清空对话
function clearChat() {
  chatStore.clearMessages()
}

// 滚动到底部
function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 格式化消息（换行符，并过滤think/思考过程）
function formatMessage(content) {
  if (!content) return ''

  let filtered = content
    .replace(/<(think|thought|reasoning)>[\s\S]*?<\/\1>/gi, '')
    .replace(/&lt;(think|thought|reasoning)&gt;[\s\S]*?&lt;\/\1&gt;/gi, '')

  const lines = filtered.split('\n')
  const cleanLines = lines.filter(line => {
    const trimmed = line.trim().toLowerCase()
    return !/^(think|thought|reasoning|思考|思路)\s*[:：]/i.test(trimmed)
  })
  filtered = cleanLines.join('\n')

  // 处理换行 - 先将转义的\n转换回换行符，再替换\n为<br>
  filtered = filtered
    .trim()
    .replace(/\\n/g, '\n')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')

  return filtered
}


// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 获取活跃会话数
async function fetchActiveSessions() {
  try {
    const data = await getActiveSessionCount()
    chatStore.setActiveSessions(data.activeSessions)
  } catch (error) {
    console.error('Failed to fetch active sessions:', error)
  }
}

onMounted(() => {
  fetchActiveSessions()
  nextTick(() => scrollToBottom())
})
</script>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f5f7fa;
}

.chat-header {
  background: white;
  padding: 15px 25px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e1e8ed;
}

.chat-header h1 {
  font-size: 22px;
  color: #333;
  margin: 0;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
}

.connection-status.connected {
  background: rgba(76, 175, 80, 0.1);
  color: #4caf50;
}

.connection-status.streaming {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.streaming .status-dot {
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.messages-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px clamp(20px, 2.5vw, 36px);
}

.welcome-message {
  width: 100%;
  margin: 0 auto;
  text-align: center;
  padding: 60px 20px;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.welcome-message h2 {
  color: #333;
  margin-bottom: 10px;
}

.welcome-message p {
  color: #666;
  margin-bottom: 30px;
}

.example-queries {
  max-width: 500px;
  margin: 0 auto;
}

.example-queries h3 {
  font-size: 14px;
  color: #999;
  margin-bottom: 15px;
}

.example-item {
  background: white;
  padding: 12px 20px;
  margin: 8px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  text-align: left;
  color: #333;
  border: 1px solid #e1e8ed;
}

.example-item:hover {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.message-list {
  width: 100%;
  margin: 0 auto;
}

.message {
  display: flex;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.user {
  justify-content: flex-end;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  margin: 0 10px;
}

.message.user .message-avatar {
  background: #667eea;
  order: 2;
}

.message.assistant .message-avatar {
  background: #764ba2;
}

.message-content {
  max-width: 88%;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  background: white;
  padding: 15px 20px;
  border-radius: 15px;
  line-height: 1.6;
  color: #333;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

.message.user .message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 15px 15px 0 15px;
}

.message.assistant .message-text {
  border-radius: 15px 15px 15px 0;
}

.message-text.streaming {
  min-height: 24px;
}

/* 消息内容中的表格和预格式化文本 */
.message-text p {
  margin: 0 0 8px 0;
}

.message-text p:last-child {
  margin-bottom: 0;
}

.message-text table {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
  font-size: 13px;
}

.message-text table th,
.message-text table td {
  border: 1px solid #e1e8ed;
  padding: 8px 12px;
  text-align: left;
}

.message-text table th {
  background: #f5f7fa;
  font-weight: 600;
}

.message-text pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

.message-text code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-10px); }
}

.input-container {
  flex-shrink: 0;
  background: white;
  padding: 15px 25px;
  border-top: 1px solid #e1e8ed;
}

.input-controls {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 15px;
  flex-wrap: wrap;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-group label {
  font-size: 14px;
  color: #666;
}

.user-select {
  padding: 6px 12px;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
}

.user-select:focus {
  border-color: #667eea;
}

.speed-slider {
  width: 100px;
  accent-color: #667eea;
}

.speed-value {
  font-size: 13px;
  color: #666;
  min-width: 40px;
}

.clear-btn {
  padding: 6px 15px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-btn:hover {
  background: #e53935;
}

.input-wrapper {
  display: flex;
  gap: 10px;
  width: 100%;
  margin: 0 auto;
}

.message-input {
  flex: 1;
  padding: 15px 20px;
  border: 2px solid #e1e8ed;
  border-radius: 25px;
  font-size: 15px;
  resize: none;
  outline: none;
  font-family: inherit;
}

.message-input:focus {
  border-color: #667eea;
}

.message-input:disabled {
  background: #f5f5f5;
}

.send-btn {
  padding: 15px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 自定义滚动条 */
.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: #f5f7fa;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #667eea;
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #764ba2;
}
</style>
