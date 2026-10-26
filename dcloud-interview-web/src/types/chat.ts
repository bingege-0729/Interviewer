/**
 * 对话消息角色
 */
export type ChatRole = 'user' | 'assistant' | 'system'

/**
 * 消息内容类型
 */
export type ContentType = 'TEXT' | 'MARKDOWN' | 'IMAGE'

/**
 * 对话消息
 */
export interface ChatMessage {
  id: number
  accountId: number
  sessionId: number
  role: ChatRole | null
  content: string
  contentType: ContentType | null
  sequence: number | null
  gmtCreate: string
}

/**
 * 发送对话请求参数
 */
export interface ChatStreamParams {
  message: string
  sessionId?: number
}

/**
 * SSE流式消息事件
 */
export interface ChatStreamEvent {
  data: string
  event?: string
  id?: string
}

/**
 * 前端展示用消息结构
 */
export interface DisplayMessage {
  id: string
  role: ChatRole
  content: string
  contentType: ContentType
  timestamp: string
  isStreaming?: boolean
}

/**
 * 对话会话摘要（用于侧边栏历史列表）
 */
export interface ChatSession {
  sessionId: number
  title: string
  lastMessage: string
  lastActiveTime: string
  messageCount: number
}
