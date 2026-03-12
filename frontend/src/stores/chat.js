import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // 消息列表
  const messages = ref([])
  // 当前用户ID
  const currentUserId = ref('user001')
  // 流式输出间隔
  const streamInterval = ref(30)
  // 是否正在流式输出
  const isStreaming = ref(false)
  // 活跃会话数
  const activeSessions = ref(0)

  /**
   * 添加用户消息
   */
  function addUserMessage(content) {
    messages.value.push({
      id: Date.now(),
      role: 'user',
      content,
      timestamp: new Date().toISOString()
    })
  }

  /**
   * 添加助手消息
   */
  function addAssistantMessage(content, isStreaming = false) {
    const existingMessage = messages.value.find(
      msg => msg.role === 'assistant' && msg.isStreaming
    )

    if (existingMessage) {
      // 更新现有消息
      existingMessage.content = content
      if (!isStreaming) {
        existingMessage.isStreaming = false
        existingMessage.timestamp = new Date().toISOString()
      }
    } else {
      // 新建消息
      messages.value.push({
        id: Date.now(),
        role: 'assistant',
        content,
        isStreaming,
        timestamp: new Date().toISOString()
      })
    }
  }

  /**
   * 标记流式输出开始
   */
  function startStreaming() {
    isStreaming.value = true
  }

  /**
   * 标记流式输出结束
   */
  function endStreaming() {
    isStreaming.value = false
  }

  /**
   * 清除所有消息
   */
  function clearMessages() {
    messages.value = []
  }

  /**
   * 设置当前用户ID
   */
  function setUserId(userId) {
    currentUserId.value = userId
  }

  /**
   * 设置流式输出间隔
   */
  function setStreamInterval(interval) {
    streamInterval.value = interval
  }

  /**
   * 更新活跃会话数
   */
  function setActiveSessions(count) {
    activeSessions.value = count
  }

  return {
    messages,
    currentUserId,
    streamInterval,
    isStreaming,
    activeSessions,
    addUserMessage,
    addAssistantMessage,
    startStreaming,
    endStreaming,
    clearMessages,
    setUserId,
    setStreamInterval,
    setActiveSessions
  }
})
