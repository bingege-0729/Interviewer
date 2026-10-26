import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { AiChatApiService } from '@/services'
import type { DisplayMessage, ChatStreamParams, ChatSession, ContentType } from '@/types'

export const useChatStore = defineStore('chat', () => {
  // 响应式状态
  const messageList = ref<DisplayMessage[]>([])
  const isLoading = ref(false)
  const isSending = ref(false)
  const isLoadingHistory = ref(false)
  const errorMessage = ref('')
  const currentSessionId = ref<number | null>(null)
  const abortController = ref<AbortController | null>(null)

  // 会话列表状态
  const sessionList = ref<ChatSession[]>([])
  const isLoadingSessions = ref(false)

  // 计算属性
  const hasMessages = computed(() => messageList.value.length > 0)
  const isStreaming = computed(() => messageList.value.some(m => m.isStreaming))

  /**
   * 发送消息并获取流式响应
   */
  const sendMessage = async (content: string) => {
    if (!content.trim() || isSending.value) return

    errorMessage.value = ''
    isSending.value = true

    // 添加用户消息
    const userMessage: DisplayMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: content.trim(),
      contentType: 'TEXT',
      timestamp: new Date().toISOString(),
    }
    messageList.value.push(userMessage)

    // 添加AI占位消息（流式填充）
    const assistantMessage: DisplayMessage = {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '',
      contentType: 'MARKDOWN',
      timestamp: new Date().toISOString(),
      isStreaming: true,
    }
    messageList.value.push(assistantMessage)

    // 如果没有sessionId，先创建一个
    if (!currentSessionId.value) {
      try {
        const createResp = await AiChatApiService.createSession()
        if (createResp.code === 0 && createResp.data) {
          currentSessionId.value = createResp.data
        }
      } catch (e) {
        console.error('创建会话失败:', e)
      }
    }

    const params: ChatStreamParams = {
      message: content.trim(),
      ...(currentSessionId.value ? { sessionId: currentSessionId.value } : {}),
    }

    try {
      abortController.value = AiChatApiService.streamChat(
        params,
        (chunk: string) => {
          // 流式更新AI消息内容
          const lastMessage = messageList.value[messageList.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.content += chunk
          }
        },
        () => {
          // 流式完成
          const lastMessage = messageList.value[messageList.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.isStreaming = false
          }
          isSending.value = false
          abortController.value = null
          // 刷新会话列表
          loadSessionList()
        },
        (error: string) => {
          // 错误处理
          const lastMessage = messageList.value[messageList.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            if (!lastMessage.content) {
              lastMessage.content = '抱歉，发生了错误，请稍后重试。'
            }
            lastMessage.isStreaming = false
          }
          errorMessage.value = error
          isSending.value = false
          abortController.value = null
        }
      )
    } catch (error) {
      console.error('发送消息失败:', error)
      isSending.value = false
      errorMessage.value = '发送失败，请稍后重试'
    }
  }

  /**
   * 加载对话历史
   */
  const loadHistory = async (sessionId: number) => {
    try {
      isLoadingHistory.value = true
      errorMessage.value = ''
      currentSessionId.value = sessionId

      const response = await AiChatApiService.getHistory(sessionId)
      if (response.code === 0 && response.data) {
        messageList.value = response.data
          .filter(record => record.role === 'user' || record.role === 'assistant')
          .map(record => ({
            id: `history-${record.id}`,
            role: record.role as 'user' | 'assistant',
            content: record.content,
            contentType: (record.contentType || (record.role === 'assistant' ? 'MARKDOWN' : 'TEXT')) as ContentType,
            timestamp: record.gmtCreate,
            isStreaming: false,
          }))
      }
    } catch (error) {
      console.error('加载对话历史失败:', error)
      errorMessage.value = '加载历史记录失败'
    } finally {
      isLoadingHistory.value = false
    }
  }

  /**
   * 加载会话列表
   */
  const loadSessionList = async () => {
    try {
      isLoadingSessions.value = true
      const response = await AiChatApiService.getSessionList()
      if (response.code === 0 && response.data) {
        sessionList.value = response.data
      }
    } catch (error) {
      console.error('加载会话列表失败:', error)
    } finally {
      isLoadingSessions.value = false
    }
  }

  /**
   * 停止当前流式响应
   */
  const stopStreaming = () => {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
    }
    const lastMessage = messageList.value[messageList.value.length - 1]
    if (lastMessage && lastMessage.isStreaming) {
      lastMessage.isStreaming = false
    }
    isSending.value = false
  }

  /**
   * 清空当前对话
   */
  const clearMessages = () => {
    stopStreaming()
    messageList.value = []
    errorMessage.value = ''
    currentSessionId.value = null
  }

  /**
   * 开始新对话（调用后端创建会话）
   */
  const startNewSession = async () => {
    clearMessages()
    try {
      const response = await AiChatApiService.createSession()
      if (response.code === 0 && response.data) {
        currentSessionId.value = response.data
      }
    } catch (error) {
      console.error('创建会话失败:', error)
    }
  }

  return {
    // 状态
    messageList,
    isLoading,
    isSending,
    isLoadingHistory,
    errorMessage,
    currentSessionId,
    sessionList,
    isLoadingSessions,
    // 计算属性
    hasMessages,
    isStreaming,
    // 方法
    sendMessage,
    loadHistory,
    loadSessionList,
    stopStreaming,
    clearMessages,
    startNewSession,
  }
})
