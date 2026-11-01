<template>
  <div class="pt-16 min-h-screen bg-light flex">
    <!-- 左侧会话历史栏 -->
    <aside
      class="w-72 bg-white border-r border-gray-100 flex flex-col h-[calc(100vh-4rem)] sticky top-16 transition-all duration-300"
      :class="{ '-ml-72 sm:ml-0': !showSidebar }"
    >
      <!-- 侧边栏头部 -->
      <div class="px-4 py-4 border-b border-gray-100">
        <button
          @click="handleNewChat"
          :disabled="isCreatingSession"
          class="w-full flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium text-white bg-primary rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-60"
        >
          <div v-if="isCreatingSession" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
          {{ isCreatingSession ? '创建中...' : '新建对话' }}
        </button>
      </div>

      <!-- 会话列表 -->
      <div class="flex-1 overflow-y-auto">
        <!-- 加载中 -->
        <div v-if="chatStore.isLoadingSessions" class="flex items-center justify-center py-8">
          <div class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="chatStore.sessionList.length === 0" class="px-4 py-8 text-center">
          <div class="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mx-auto mb-3">
            <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
          </div>
          <p class="text-sm text-secondary">暂无对话记录</p>
          <p class="text-xs text-gray-400 mt-1">点击上方按钮开始新对话</p>
        </div>

        <!-- 会话列表项 -->
        <div v-else class="py-2">
          <button
            v-for="session in chatStore.sessionList"
            :key="session.sessionId"
            @click="handleSelectSession(session.sessionId)"
            class="w-full px-4 py-3 text-left hover:bg-gray-50 transition-colors group relative"
            :class="{ 'bg-primary/5 border-r-2 border-primary': chatStore.currentSessionId === session.sessionId }"
          >
            <div class="flex items-start gap-2">
              <svg class="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0 group-hover:text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-dark truncate">{{ session.title }}</p>
                <p class="text-xs text-secondary mt-0.5 truncate">{{ session.lastMessage }}</p>
                <p class="text-xs text-gray-400 mt-1">{{ formatTime(session.lastActiveTime) }}</p>
              </div>
            </div>
          </button>
        </div>
      </div>

      <!-- 侧边栏底部 -->
      <div class="px-4 py-3 border-t border-gray-100">
        <router-link to="/user" class="flex items-center gap-2 text-sm text-secondary hover:text-primary transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
          返回个人中心
        </router-link>
      </div>
    </aside>

    <!-- 移动端侧边栏遮罩 -->
    <div
      v-if="showSidebar"
      class="fixed inset-0 bg-black/30 z-10 sm:hidden"
      @click="showSidebar = false"
    ></div>

    <!-- 右侧对话主区域 -->
    <main class="flex-1 flex flex-col h-[calc(100vh-4rem)]">
      <!-- 顶部标题栏 -->
      <div class="bg-white border-b border-gray-100 px-4 sm:px-6 py-3 flex-shrink-0">
        <div class="max-w-4xl mx-auto flex items-center justify-between">
          <div class="flex items-center gap-3">
            <!-- 移动端菜单按钮 -->
            <button
              @click="showSidebar = !showSidebar"
              class="sm:hidden w-8 h-8 flex items-center justify-center rounded-lg hover:bg-gray-100 transition-colors text-secondary"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/></svg>
            </button>
            <div class="w-9 h-9 bg-gradient-to-br from-primary to-primary-dark rounded-xl flex items-center justify-center">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
            </div>
            <div>
              <h1 class="text-base font-bold text-dark">AI对话助手</h1>
              <p class="text-xs text-secondary">智能问答 · 简历分析 · 面试创建 · 权益查询</p>
            </div>
          </div>
          <button
            @click="handleNewChat"
            class="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-primary border border-primary/30 rounded-lg hover:bg-primary/5 transition-colors"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
            <span class="hidden sm:inline">新对话</span>
          </button>
        </div>
      </div>

      <!-- 消息区域 -->
      <div ref="messageContainerRef" class="flex-1 overflow-y-auto px-4 sm:px-6 py-6">
        <div class="max-w-4xl mx-auto space-y-6">

          <!-- 加载历史中 -->
          <div v-if="chatStore.isLoadingHistory" class="flex items-center justify-center py-20">
            <div class="text-center">
              <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
              <p class="text-secondary mt-3 text-sm">加载对话记录...</p>
            </div>
          </div>

          <!-- 空状态引导 -->
          <div v-else-if="!chatStore.hasMessages" class="flex flex-col items-center justify-center py-16">
            <div class="w-20 h-20 bg-primary/10 rounded-2xl flex items-center justify-center mb-6">
              <svg class="w-10 h-10 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
            </div>
            <h2 class="text-lg font-bold text-dark mb-2">你好，我是AI对话助手</h2>
            <p class="text-sm text-secondary text-center max-w-md mb-8">我可以帮你分析简历、创建AI面试、查询权益信息，或者直接和我聊天。</p>

            <!-- 快捷操作卡片 -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 w-full max-w-lg">
              <button
                v-for="quick in quickActions"
                :key="quick.text"
                class="flex items-start gap-3 p-4 bg-white rounded-xl border border-gray-100 hover:border-primary/30 hover:shadow-sm transition-all text-left"
                @click="handleQuickAction(quick.text)"
              >
                <span class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" :class="quick.bgClass">
                  <span v-html="quick.icon"></span>
                </span>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-dark">{{ quick.title }}</p>
                  <p class="text-xs text-secondary mt-0.5">{{ quick.desc }}</p>
                </div>
              </button>
            </div>
          </div>

          <!-- 消息列表 -->
          <template v-else>
            <div
              v-for="message in chatStore.messageList"
              :key="message.id"
              class="flex gap-3"
              :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
            >
              <!-- AI头像 -->
              <div v-if="message.role === 'assistant'" class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-primary-dark flex items-center justify-center flex-shrink-0 mt-1">
                <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
              </div>

              <!-- 消息气泡 -->
              <div
                class="max-w-[80%] sm:max-w-[70%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
                :class="message.role === 'user'
                  ? 'bg-primary text-white rounded-br-md'
                  : 'bg-white border border-gray-100 text-dark rounded-bl-md shadow-sm'"
              >
                <!-- 用户消息：纯文本 -->
                <div v-if="message.role === 'user'" class="whitespace-pre-wrap break-words">{{ message.content }}</div>
                <!-- AI消息：根据内容类型渲染 -->
                <div
                  v-else
                  class="prose prose-sm max-w-none prose-p:my-1 prose-headings:my-2 prose-ul:my-1 prose-ol:my-1 prose-li:my-0.5 prose-pre:my-2 prose-code:text-primary prose-code:bg-primary/5 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:before:content-none prose-code:after:content-none"
                  v-html="renderMessageContent(message.content, message.contentType)"
                ></div>
                <!-- 流式加载指示器 -->
                <div v-if="message.isStreaming" class="flex items-center gap-1 mt-2">
                  <span class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce" style="animation-delay: 0ms"></span>
                  <span class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce" style="animation-delay: 150ms"></span>
                  <span class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce" style="animation-delay: 300ms"></span>
                </div>
              </div>

              <!-- 用户头像 -->
              <div v-if="message.role === 'user'" class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 mt-1">
                <span class="text-primary text-xs font-bold">{{ avatarText }}</span>
              </div>
            </div>
          </template>

        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="chatStore.errorMessage" class="px-4 sm:px-6 flex-shrink-0">
        <div class="max-w-4xl mx-auto">
          <div class="flex items-center gap-2 px-4 py-2 bg-red-50 border border-red-100 rounded-lg mb-3">
            <svg class="w-4 h-4 text-red-500 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/></svg>
            <span class="text-xs text-red-600">{{ chatStore.errorMessage }}</span>
            <button @click="chatStore.errorMessage = ''" class="ml-auto text-red-400 hover:text-red-600">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="bg-white border-t border-gray-100 px-4 sm:px-6 py-4 flex-shrink-0">
        <div class="max-w-4xl mx-auto">
          <div class="flex items-end gap-3">
            <div class="flex-1 relative">
              <textarea
                ref="inputRef"
                v-model="inputContent"
                :disabled="chatStore.isSending"
                :placeholder="chatStore.isSending ? 'AI正在回复中...' : '输入你的问题，按 Enter 发送'"
                class="w-full px-4 py-3 pr-12 border border-gray-200 rounded-xl text-sm resize-none focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-colors disabled:bg-gray-50 disabled:text-secondary"
                rows="1"
                @keydown.enter.exact.prevent="handleSend"
                @input="handleTextareaResize"
              ></textarea>
            </div>
            <!-- 发送/停止按钮 -->
            <button
              v-if="chatStore.isSending"
              @click="chatStore.stopStreaming()"
              class="w-10 h-10 flex items-center justify-center bg-red-500 text-white rounded-xl hover:bg-red-600 transition-colors flex-shrink-0"
              title="停止生成"
            >
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><rect x="5" y="5" width="10" height="10" rx="1"/></svg>
            </button>
            <button
              v-else
              @click="handleSend"
              :disabled="!inputContent.trim()"
              class="w-10 h-10 flex items-center justify-center bg-primary text-white rounded-xl hover:bg-primary-dark transition-colors disabled:opacity-40 disabled:cursor-not-allowed flex-shrink-0"
              title="发送消息"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19V5m0 0l-7 7m7-7l7 7"/></svg>
            </button>
          </div>
          <p class="text-xs text-secondary mt-2 text-center">AI对话助手支持简历分析、面试创建、权益查询等功能</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore, useChatStore } from '@/stores'
import { renderMessageContent } from '@/utils/markdown'
import { OrderApiService } from '@/services'
import QRCode from 'qrcode'

// Store
const userStore = useUserStore()
const chatStore = useChatStore()

// DOM引用
const messageContainerRef = ref<HTMLDivElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)

// 响应式状态
const inputContent = ref('')
const showSidebar = ref(false)
const isCreatingSession = ref(false)

// 计算属性
const avatarText = computed(() => {
  const name = userStore.username
  return name ? name.charAt(0).toUpperCase() : '?'
})

// 快捷操作配置
const quickActions = [
  {
    title: '分析我的简历',
    desc: '获取简历优化建议',
    text: '帮我分析一下简历',
    icon: '<svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>',
    bgClass: 'bg-blue-50',
  },
  {
    title: '创建AI面试',
    desc: '模拟真实面试场景',
    text: '我想创建一场面试',
    icon: '<svg class="w-4 h-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>',
    bgClass: 'bg-green-50',
  },
  {
    title: '查看我的权益',
    desc: '了解套餐与余额',
    text: '查询一下我的权益信息',
    icon: '<svg class="w-4 h-4 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7"/></svg>',
    bgClass: 'bg-amber-50',
  },
  {
    title: '随便聊聊',
    desc: '问我任何问题',
    text: '你好，你能帮我做什么？',
    icon: '<svg class="w-4 h-4 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z"/></svg>',
    bgClass: 'bg-purple-50',
  },
]

/**
 * 格式化时间显示
 */
const formatTime = (timeStr: string): string => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  if (diffHour < 24) return `${diffHour}小时前`
  if (diffDay < 7) return `${diffDay}天前`
  return `${date.getMonth() + 1}/${date.getDate()}`
}

/**
 * 发送消息
 */
const handleSend = () => {
  if (!inputContent.value.trim() || chatStore.isSending) return
  chatStore.sendMessage(inputContent.value)
  inputContent.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
}

/**
 * 快捷操作
 */
const handleQuickAction = (text: string) => {
  inputContent.value = text
  handleSend()
}

/**
 * 新对话
 */
const handleNewChat = async () => {
  isCreatingSession.value = true
  await chatStore.startNewSession()
  inputContent.value = ''
  showSidebar.value = false
  isCreatingSession.value = false
  inputRef.value?.focus()
}

/**
 * 选择历史会话
 */
const handleSelectSession = (sessionId: number) => {
  if (chatStore.currentSessionId === sessionId) return
  chatStore.loadHistory(sessionId)
  showSidebar.value = false
}

/**
 * textarea自适应高度
 */
const handleTextareaResize = () => {
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
    const maxHeight = 120
    inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, maxHeight) + 'px'
  }
}

/**
 * 滚动到底部
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
    }
  })
}

// 监听消息列表变化 → 自动滚动到底部
watch(
  () => chatStore.messageList.length,
  () => scrollToBottom()
)

// 监听最后一条消息内容变化（流式更新时滚动）
watch(
  () => {
    const lastMsg = chatStore.messageList[chatStore.messageList.length - 1]
    return lastMsg?.content?.length || 0
  },
  () => scrollToBottom()
)

// 组件挂载时加载会话列表
onMounted(() => {
  inputRef.value?.focus()
  chatStore.loadSessionList()
})

// 支付卡片QR码生成和轮询管理
const paymentPollingTimers = ref<Map<string, ReturnType<typeof setInterval>>>(new Map())

/**
 * 处理支付卡片：生成QR码并开始轮询支付状态
 * 在消息渲染完成后调用，扫描DOM中的payment-card元素
 */
const processPaymentCards = async () => {
  await nextTick()
  const container = messageContainerRef.value
  if (!container) return

  const paymentCards = container.querySelectorAll('.payment-card[data-code-url]')
  paymentCards.forEach(async (card) => {
    const cardEl = card as HTMLElement
    const codeUrl = cardEl.dataset.codeUrl || ''
    const outTradeNo = cardEl.dataset.outTradeNo || ''
    const canvas = cardEl.querySelector('.payment-qr-canvas') as HTMLCanvasElement

    if (!codeUrl || !canvas || canvas.dataset.rendered === 'true') return

    // 生成QR码到canvas
    try {
      await QRCode.toCanvas(canvas, codeUrl, {
        width: 200,
        margin: 2,
        color: { dark: '#000000', light: '#ffffff' },
      })
      canvas.dataset.rendered = 'true'
    } catch (err) {
      console.error('QR码生成失败:', err)
    }

    // 开始轮询支付状态
    if (outTradeNo && !paymentPollingTimers.value.has(outTradeNo)) {
      startPaymentPolling(outTradeNo)
    }
  })
}

/**
 * 更新支付卡片为成功状态
 * 通过outTradeNo重新查询DOM（避免v-html重渲染导致旧引用失效）
 */
const markPaymentSuccess = (outTradeNo: string) => {
  const container = messageContainerRef.value
  if (!container) return
  const cardEl = container.querySelector(`.payment-card[data-out-trade-no="${outTradeNo}"]`)
  if (!cardEl) return
  const statusEl = cardEl.querySelector('.payment-status')
  if (statusEl) {
    statusEl.setAttribute('data-status', 'success')
    const iconEl = statusEl.querySelector('.payment-status-icon')
    const textEl = statusEl.querySelector('.payment-status-text')
    if (iconEl) iconEl.textContent = '\u2705'
    if (textEl) textEl.textContent = '支付成功！权益已自动发放'
  }
}

/**
 * 开始轮询指定订单的支付状态
 * 先查询一次，已支付则直接展示成功不再轮询
 */
const startPaymentPolling = async (outTradeNo: string) => {
  // 先查询一次当前状态，已支付直接展示成功
  try {
    const initResp = await OrderApiService.queryPayStatus(outTradeNo)
    if (initResp.code === 0 && initResp.data && initResp.data.orderState === 'PAY') {
      markPaymentSuccess(outTradeNo)
      return
    }
  } catch (err) {
    console.error('初始查询支付状态失败:', err)
  }

  // 未支付，开始定时轮询
  const timer = setInterval(async () => {
    try {
      const response = await OrderApiService.queryPayStatus(outTradeNo)
      if (response.code === 0 && response.data && response.data.orderState === 'PAY') {
        markPaymentSuccess(outTradeNo)
        stopPaymentPolling(outTradeNo)
      }
    } catch (err) {
      console.error('查询支付状态失败:', err)
    }
  }, 3000)

  paymentPollingTimers.value.set(outTradeNo, timer)

  // 5分钟后自动停止轮询（订单超时）
  setTimeout(() => {
    if (paymentPollingTimers.value.has(outTradeNo)) {
      markPaymentExpired(outTradeNo)
      stopPaymentPolling(outTradeNo)
    }
  }, 5 * 60 * 1000)
}

/**
 * 标记支付卡片为过期状态
 */
const markPaymentExpired = (outTradeNo: string) => {
  const container = messageContainerRef.value
  if (!container) return
  const cardEl = container.querySelector(`.payment-card[data-out-trade-no="${outTradeNo}"]`)
  if (!cardEl) return
  const statusEl = cardEl.querySelector('.payment-status')
  if (statusEl && statusEl.getAttribute('data-status') !== 'success') {
    statusEl.setAttribute('data-status', 'expired')
    const iconEl = statusEl.querySelector('.payment-status-icon')
    const textEl = statusEl.querySelector('.payment-status-text')
    if (iconEl) iconEl.textContent = '\u23f0'
    if (textEl) textEl.textContent = '二维码已过期，请重新下单'
  }
}

/**
 * 停止指定订单的支付轮询
 */
const stopPaymentPolling = (outTradeNo: string) => {
  const timer = paymentPollingTimers.value.get(outTradeNo)
  if (timer) {
    clearInterval(timer)
    paymentPollingTimers.value.delete(outTradeNo)
  }
}

/**
 * 停止所有支付轮询
 */
const stopAllPaymentPolling = () => {
  paymentPollingTimers.value.forEach((timer) => clearInterval(timer))
  paymentPollingTimers.value.clear()
}

// 监听消息内容变化，处理新出现的支付卡片
watch(
  () => {
    const lastMsg = chatStore.messageList[chatStore.messageList.length - 1]
    return lastMsg?.content || ''
  },
  (newContent) => {
    if (newContent && newContent.includes('[PAY_QRCODE:')) {
      processPaymentCards()
    }
  }
)

// 流式完成时也检查
watch(
  () => chatStore.messageList.filter(m => m.isStreaming === false).length,
  () => {
    processPaymentCards()
  }
)

// 组件卸载时停止流式响应和支付轮询
onUnmounted(() => {
  chatStore.stopStreaming()
  stopAllPaymentPolling()
})
</script>
