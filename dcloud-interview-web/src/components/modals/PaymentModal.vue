<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="show"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        @click.self="handleClose"
      >
        <Transition name="scale">
          <div
            v-if="show"
            class="relative bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 p-8"
          >
            <!-- 关闭按钮 -->
            <button
              class="absolute top-4 right-4 text-secondary hover:text-dark transition-colors"
              @click="handleClose"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            <!-- 步骤1：选择支付方式 -->
            <div v-if="step === 'select'">
              <div class="mb-6">
                <h2 class="text-xl font-bold text-dark">选择支付方式</h2>
                <p class="text-secondary text-sm mt-1">请选择您偏好的支付方式</p>
              </div>

              <!-- 分隔线 -->
              <div class="border-t border-gray-100 mb-5"></div>

              <!-- 订单信息 -->
              <div class="space-y-3 mb-5">
                <div class="flex justify-between items-center">
                  <span class="text-sm text-secondary">{{ orderTypeLabel }}</span>
                  <span class="text-sm font-medium text-dark">{{ productName }}</span>
                </div>
                <div class="flex justify-between items-center">
                  <span class="text-sm text-secondary">价格</span>
                  <span class="text-sm text-dark">¥{{ totalAmount.toFixed(2) }}</span>
                </div>
              </div>

              <!-- 分隔线 -->
              <div class="border-t border-gray-100 mb-5"></div>

              <!-- 实付款 -->
              <div class="flex justify-between items-center mb-6">
                <span class="text-base font-bold text-dark">实付款</span>
                <span class="text-xl font-bold text-primary">¥{{ totalAmount.toFixed(2) }}</span>
              </div>

              <!-- 支付方式选项 -->
              <div class="space-y-3">
                <!-- 微信支付 -->
                <button
                  class="w-full flex items-center justify-between px-4 py-3.5 border border-gray-200 rounded-xl hover:border-primary/40 hover:bg-primary/5 transition-all"
                  :disabled="isCreatingOrder"
                  @click="handleSelectPayType('WECHAT_PAY')"
                >
                  <div class="flex items-center gap-3">
                    <svg class="w-7 h-7" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="12" fill="#07C160"/>
                      <path d="M16.2 10.5c-.2 0-.4 0-.6.1.1-.4.1-.8.1-1.2 0-2.5-2.4-4.4-5.4-4.4C7.3 5 5 7 5 9.4c0 1.4.8 2.6 2 3.5l-.5 1.5 1.7-1c.5.2 1 .3 1.6.3h.3c-.1-.3-.1-.6-.1-.9 0-2.1 2-3.8 4.5-3.8.6 0 1.2.1 1.7.5zm-5.9-1.7c.4 0 .7.3.7.7s-.3.7-.7.7-.7-.3-.7-.7.3-.7.7-.7zm-3.3 0c.4 0 .7.3.7.7s-.3.7-.7.7-.7-.3-.7-.7.3-.7.7-.7z" fill="white"/>
                      <path d="M19 12.8c0-1.9-1.9-3.4-4.2-3.4-2.3 0-4.2 1.5-4.2 3.4s1.9 3.4 4.2 3.4c.5 0 .9-.1 1.3-.2l1.3.7-.4-1.2c1.2-.7 2-1.6 2-2.7zm-5.6-.5c-.3 0-.5-.2-.5-.5s.2-.5.5-.5.5.2.5.5-.2.5-.5.5zm2.8 0c-.3 0-.5-.2-.5-.5s.2-.5.5-.5.5.2.5.5-.2.5-.5.5z" fill="white"/>
                    </svg>
                    <span class="text-sm font-medium text-dark">微信支付</span>
                  </div>
                  <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>

                <!-- 支付宝（暂不可用） -->
                <div class="w-full flex items-center justify-between px-4 py-3.5 border border-gray-100 rounded-xl bg-gray-50 opacity-60 cursor-not-allowed">
                  <div class="flex items-center gap-3">
                    <svg class="w-7 h-7" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="12" fill="#1677FF"/>
                      <path d="M17.5 14.2c-.8-.3-1.7-.7-2.5-1.1.4-.7.7-1.5.9-2.3h-2.6v-1h3.2v-.6h-3.2V7.5h-1.5v1.7H8.5v.6h3.3v1H9v.6h4.7c-.2.6-.5 1.1-.8 1.6-1.2-.5-2.5-.8-3.6-.8-1.7 0-2.8.8-2.8 2 0 1.3 1.1 2.1 2.9 2.1 1.5 0 2.8-.6 3.8-1.6.9.5 1.9 1 2.9 1.3l1.4-1.8zM9.3 15.7c-1 0-1.5-.4-1.5-.9 0-.6.6-1 1.6-1 .8 0 1.7.2 2.5.6-.8.8-1.7 1.3-2.6 1.3z" fill="white"/>
                    </svg>
                    <span class="text-sm font-medium text-gray-400">支付宝支付</span>
                  </div>
                  <span class="text-xs text-gray-400">暂不可用</span>
                </div>
              </div>
            </div>

            <!-- 步骤2：扫码支付 -->
            <div v-else-if="step === 'qrcode'">
              <div class="mb-6">
                <h2 class="text-xl font-bold text-dark">微信扫码支付</h2>
                <p class="text-secondary text-sm mt-1">请使用微信扫一扫完成支付</p>
              </div>

              <div class="flex flex-col items-center py-4">
                <!-- 二维码加载中 -->
                <div v-if="isGeneratingQr" class="flex flex-col items-center justify-center h-56">
                  <svg class="animate-spin w-8 h-8 text-primary" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  <p class="text-secondary text-sm mt-3">正在生成支付二维码...</p>
                </div>

                <!-- 二维码显示 -->
                <div v-else-if="qrCodeDataUrl" class="flex flex-col items-center">
                  <div class="w-52 h-52 border border-gray-100 rounded-lg overflow-hidden p-2">
                    <img :src="qrCodeDataUrl" alt="微信支付二维码" class="w-full h-full object-contain" />
                  </div>

                  <!-- 支付金额 -->
                  <div class="mt-4 text-center">
                    <p class="text-2xl font-bold text-primary">¥{{ totalAmount.toFixed(2) }}</p>
                    <p class="text-secondary text-xs mt-1">支付完成后将自动跳转</p>
                  </div>

                  <!-- 轮询状态指示 -->
                  <div class="mt-4 flex items-center gap-2 text-secondary text-xs">
                    <svg class="animate-spin w-3.5 h-3.5" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    <span>等待扫码支付中...</span>
                  </div>
                </div>

                <!-- 生成失败 -->
                <div v-else class="flex flex-col items-center justify-center h-56">
                  <p class="text-secondary text-sm mb-3">二维码生成失败</p>
                  <button
                    class="px-4 py-1.5 bg-primary text-white text-sm rounded-lg hover:bg-primary-dark transition-colors"
                    @click="handleRetryPayment"
                  >
                    重新生成
                  </button>
                </div>
              </div>
            </div>

            <!-- 步骤3：支付成功 -->
            <div v-else-if="step === 'success'">
              <div class="flex flex-col items-center py-10">
                <svg class="w-16 h-16 text-green-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <h3 class="text-lg font-bold text-dark mb-2">支付成功</h3>
                <p class="text-secondary text-sm">您的订单已支付完成</p>
                <button
                  class="mt-6 px-6 py-2.5 bg-primary text-white rounded-lg font-medium hover:bg-primary-dark transition-colors"
                  @click="handlePaySuccess"
                >
                  完成
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import QRCode from 'qrcode'
import { OrderApiService } from '@/services'
import { PayType } from '@/types'
import type { CreatePackageOrderParams, CreateBenefitOrderParams } from '@/types'

interface Props {
  show?: boolean
  /** 订单类型：package=套餐, benefit=权益 */
  orderType?: 'package' | 'benefit'
  /** 商品名称 */
  productName?: string
  /** 套餐ID（套餐订单时必填） */
  packageId?: number
  /** 权益ID（权益订单时必填） */
  benefitId?: number
  /** 购买次数（权益订单时必填） */
  purchaseCount?: number
  /** 总金额 */
  totalAmount?: number
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
  orderType: 'package',
  productName: '',
  packageId: 0,
  benefitId: 0,
  purchaseCount: 1,
  totalAmount: 0,
})

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success'): void
}>()

// 步骤状态
const step = ref<'select' | 'qrcode' | 'success'>('select')
const isCreatingOrder = ref(false)
const isGeneratingQr = ref(false)
const qrCodeDataUrl = ref('')
const outTradeNo = ref('')
let pollTimer: ReturnType<typeof setInterval> | null = null

// 计算属性
const orderTypeLabel = props.orderType === 'package' ? '套餐' : '权益'

// 监听弹窗显示状态
watch(() => props.show, (val) => {
  if (val) {
    step.value = 'select'
    qrCodeDataUrl.value = ''
    outTradeNo.value = ''
  } else {
    stopPolling()
  }
})

/** 选择支付方式 - 微信支付 */
const handleSelectPayType = async (_payType: string) => {
  if (isCreatingOrder.value) return
  try {
    isCreatingOrder.value = true
    step.value = 'qrcode'
    isGeneratingQr.value = true

    let response
    if (props.orderType === 'package') {
      const params: CreatePackageOrderParams = {
        packageId: props.packageId,
        payType: PayType.WECHAT_PAY,
      }
      response = await OrderApiService.createPackageOrder(params)
    } else {
      const params: CreateBenefitOrderParams = {
        benefitId: props.benefitId,
        purchaseCount: props.purchaseCount,
        payType: PayType.WECHAT_PAY,
      }
      response = await OrderApiService.createBenefitOrder(params)
    }

    if (response.code === 0 && response.data) {
      outTradeNo.value = response.data.outTradeNo
      // 将 codeUrl 转换为二维码图像
      const dataUrl = await QRCode.toDataURL(response.data.codeUrl, {
        width: 256,
        margin: 2,
        color: { dark: '#000000', light: '#ffffff' },
      })
      qrCodeDataUrl.value = dataUrl
      // 开始轮询支付状态
      startPolling()
    } else {
      qrCodeDataUrl.value = ''
    }
  } catch (error) {
    console.error('创建订单失败:', error)
    qrCodeDataUrl.value = ''
  } finally {
    isCreatingOrder.value = false
    isGeneratingQr.value = false
  }
}

/** 开始轮询支付状态 */
const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!outTradeNo.value) return
    try {
      const response = await OrderApiService.queryPayStatus(outTradeNo.value)
      if (response.code === 0 && response.data) {
        if (response.data.orderState === 'PAY') {
          stopPolling()
          step.value = 'success'
        }
      }
    } catch (error) {
      console.error('查询支付状态失败:', error)
    }
  }, 3000)
}

/** 停止轮询 */
const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 重试支付 */
const handleRetryPayment = () => {
  step.value = 'select'
}

/** 支付成功确认 */
const handlePaySuccess = () => {
  emit('success')
  handleClose()
}

/** 关闭弹窗 */
const handleClose = () => {
  stopPolling()
  emit('update:show', false)
  // 重置状态
  step.value = 'select'
  qrCodeDataUrl.value = ''
  outTradeNo.value = ''
}

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.scale-enter-active,
.scale-leave-active {
  transition: all 0.2s ease;
}
.scale-enter-from,
.scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
