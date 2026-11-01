import { http } from '@/utils/http'
import { endpoints } from '@/config/api'
import type { ApiResponse, ChatMessage, ChatStreamParams, ChatSession } from '@/types'

/**
 * SSE流式消息数据结构
 */
interface StreamChunk {
  content: string
  contentType: string
}

/**
 * AI对话服务
 * 提供SSE流式对话和历史记录查询
 */
export class AiChatApiService {
  /**
   * SSE流式对话
   * 使用fetch API发送POST请求，返回ReadableStream处理SSE事件
   */
  static streamChat(
    params: ChatStreamParams,
    onMessage: (content: string) => void,
    onComplete: () => void,
    onError: (error: string) => void
  ): AbortController {
    const controller = new AbortController()
    const baseURL = import.meta.env.VITE_API_BASE_URL
    const url = `${baseURL}${endpoints.chat.stream}`
    const token = localStorage.getItem('token')

    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { token } : {}),
      },
      body: JSON.stringify(params),
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            window.location.href = '/login'
            return
          }
          throw new Error(`请求失败: ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
          throw new Error('无法读取响应流')
        }

        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            onComplete()
            break
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.slice(5).trim()
              if (data === '[DONE]') {
                onComplete()
                return
              }
              if (data) {
                // 解析JSON格式的SSE数据
                try {
                  const chunk: StreamChunk = JSON.parse(data)
                  if (chunk.content) {
                    onMessage(chunk.content)
                  }
                } catch {
                  // 兼容旧格式：纯文本
                  onMessage(data)
                }
              }
            }
          }
        }
      })
      .catch((error: Error) => {
        if (error.name === 'AbortError') {
          return
        }
        onError(error.message || '网络连接异常')
      })

    return controller
  }

  /**
   * 获取对话历史记录
   */
  static async getHistory(sessionId: number): Promise<ApiResponse<ChatMessage[]>> {
    const response = await http.getAxiosInstance().get(endpoints.chat.history, {
      params: { sessionId },
    })
    return response.data
  }

  /**
   * 获取用户的对话会话列表
   */
  static async getSessionList(): Promise<ApiResponse<ChatSession[]>> {
    const response = await http.getAxiosInstance().get(endpoints.chat.sessions)
    return response.data
  }

  /**
   * 创建新会话，获取sessionId
   */
  static async createSession(): Promise<ApiResponse<number>> {
    const response = await http.getAxiosInstance().post(endpoints.chat.createSession)
    return response.data
  }
}

export default AiChatApiService
