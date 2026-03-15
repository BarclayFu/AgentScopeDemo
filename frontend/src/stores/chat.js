import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loadStoredSettings } from '@/config/runtimeSettings'

const CHAT_STORAGE_KEY = 'agentscope.chat.state'

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function cloneMessages(messages = []) {
  return messages.map(message => ({
    ...message,
    isStreaming: false
  }))
}

function loadStoredChatState(defaultSettings) {
  if (!canUseStorage()) {
    return {
      currentUserId: defaultSettings.defaultUserId,
      streamInterval: defaultSettings.defaultStreamInterval,
      conversations: {}
    }
  }

  const raw = window.localStorage.getItem(CHAT_STORAGE_KEY)
  if (!raw) {
    return {
      currentUserId: defaultSettings.defaultUserId,
      streamInterval: defaultSettings.defaultStreamInterval,
      conversations: {}
    }
  }

  try {
    const parsed = JSON.parse(raw)
    const conversations = Object.fromEntries(
      Object.entries(parsed.conversations || {}).map(([userId, messages]) => [
        userId,
        cloneMessages(Array.isArray(messages) ? messages : [])
      ])
    )

    return {
      currentUserId: parsed.currentUserId || defaultSettings.defaultUserId,
      streamInterval: Number.parseInt(parsed.streamInterval, 10) || defaultSettings.defaultStreamInterval,
      conversations
    }
  } catch (error) {
    console.warn('Failed to parse stored chat state:', error)
    return {
      currentUserId: defaultSettings.defaultUserId,
      streamInterval: defaultSettings.defaultStreamInterval,
      conversations: {}
    }
  }
}

export const useChatStore = defineStore('chat', () => {
  const initialSettings = loadStoredSettings()
  const persistedState = loadStoredChatState(initialSettings)
  const conversations = ref(persistedState.conversations)
  // 消息列表
  const messages = ref(
    cloneMessages(persistedState.conversations[persistedState.currentUserId] || [])
  )
  // 当前用户ID
  const currentUserId = ref(persistedState.currentUserId)
  // 流式输出间隔
  const streamInterval = ref(persistedState.streamInterval)
  // 是否正在流式输出
  const isStreaming = ref(false)
  // 活跃会话数
  const activeSessions = ref(0)

  function persistChatState() {
    conversations.value = {
      ...conversations.value,
      [currentUserId.value]: cloneMessages(messages.value)
    }

    if (!canUseStorage()) {
      return
    }

    window.localStorage.setItem(
      CHAT_STORAGE_KEY,
      JSON.stringify({
        currentUserId: currentUserId.value,
        streamInterval: streamInterval.value,
        conversations: conversations.value
      })
    )
  }

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
    persistChatState()
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
      existingMessage.isStreaming = isStreaming
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

    persistChatState()
  }

  /**
   * 标记流式输出开始
   */
  function startStreaming() {
    isStreaming.value = true
    persistChatState()
  }

  /**
   * 标记流式输出结束
   */
  function endStreaming() {
    isStreaming.value = false
    persistChatState()
  }

  /**
   * 清除所有消息
   */
  function clearMessages() {
    messages.value = []
    persistChatState()
  }

  /**
   * 设置当前用户ID
   */
  function setUserId(userId) {
    persistChatState()
    currentUserId.value = userId
    messages.value = cloneMessages(conversations.value[userId] || [])
    isStreaming.value = false
    persistChatState()
  }

  /**
   * 设置流式输出间隔
   */
  function setStreamInterval(interval) {
    streamInterval.value = Number.parseInt(interval, 10)
    persistChatState()
  }

  /**
   * 更新活跃会话数
   */
  function setActiveSessions(count) {
    activeSessions.value = count
  }

  /**
   * 根据设置同步聊天默认值
   */
  function applySettings(settings) {
    if (settings.defaultUserId && messages.value.length === 0 && !conversations.value[currentUserId.value]?.length) {
      currentUserId.value = settings.defaultUserId
    }
    if (typeof settings.defaultStreamInterval !== 'undefined') {
      streamInterval.value = Number.parseInt(settings.defaultStreamInterval, 10)
    }
    persistChatState()
  }

  return {
    messages,
    conversations,
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
    setActiveSessions,
    applySettings
  }
})
