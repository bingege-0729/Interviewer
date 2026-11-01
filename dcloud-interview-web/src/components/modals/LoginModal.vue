<template>
  <!-- 遮罩层 -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="show"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        @click.self="handleClose"
      >
        <!-- 弹窗主体 -->
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

            <!-- 标题 -->
            <div class="text-center mb-6">
              <h2 class="text-2xl font-bold text-dark">手机短信登录</h2>
              <p class="text-secondary text-sm mt-2">未注册的手机号将自动创建账号</p>
            </div>

            <!-- Tab 切换 -->
            <div class="flex border border-gray-200 rounded-lg mb-6 overflow-hidden">
              <button
                class="flex-1 py-2.5 text-sm font-medium transition-colors"
                :class="activeTab === 'wechat' ? 'bg-white text-primary border-b-2 border-primary' : 'bg-gray-50 text-secondary'"
                @click="activeTab = 'wechat'"
              >
                微信登录
              </button>
              <button
                class="flex-1 py-2.5 text-sm font-medium transition-colors"
                :class="activeTab === 'sms' ? 'bg-white text-primary border-b-2 border-primary' : 'bg-gray-50 text-secondary'"
                @click="activeTab = 'sms'"
              >
                手机短信
              </button>
            </div>

            <!-- 手机短信登录表单 -->
            <div v-show="activeTab === 'sms'">
              <!-- 手机号 -->
              <div class="mb-4">
                <label class="block text-sm font-medium text-dark mb-1.5">手机号</label>
                <div class="relative">
                  <input
                    v-model="phone"
                    type="tel"
                    maxlength="11"
                    placeholder="请输入手机号"
                    class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    :class="{ 'border-red-400': phoneError }"
                  />
                  <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                  </svg>
                </div>
                <p v-if="phoneError" class="text-red-500 text-xs mt-1">{{ phoneError }}</p>
              </div>

              <!-- 图形验证码 -->
              <div class="mb-4">
                <label class="block text-sm font-medium text-dark mb-1.5">图形验证码（发送短信前需要验证）</label>
                <div class="flex gap-3">
                  <input
                    v-model="captcha"
                    type="text"
                    maxlength="6"
                    placeholder="请输入验证码"
                    class="flex-1 px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                  />
                  <button
                    class="flex-shrink-0 px-4 py-2.5 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
                    @click="handleRefreshCaptcha"
                  >
                    <img
                      v-if="captchaUrl"
                      :src="captchaUrl"
                      alt="验证码"
                      class="h-8 w-auto"
                    />
                    <span v-else class="text-sm text-secondary">获取验证码</span>
                  </button>
                </div>
              </div>

              <!-- 短信验证码 -->
              <div class="mb-4">
                <label class="block text-sm font-medium text-dark mb-1.5">短信验证码</label>
                <div class="flex gap-3">
                  <input
                    v-model="smsCode"
                    type="text"
                    maxlength="6"
                    placeholder="请输入短信验证码"
                    class="flex-1 px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                  />
                  <button
                    class="flex-shrink-0 px-4 py-2.5 border border-gray-200 rounded-lg text-sm font-medium transition-colors"
                    :class="isCounting ? 'text-gray-400 cursor-not-allowed' : 'text-dark hover:bg-gray-50'"
                    :disabled="isCounting"
                    @click="handleSendCode"
                  >
                    {{ isCounting ? `${countdown}s` : '发送验证码' }}
                  </button>
                </div>
              </div>

              <!-- 协议 -->
              <div class="flex items-start gap-2 mb-6">
                <input
                  v-model="isAgreed"
                  type="checkbox"
                  class="mt-0.5 w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary"
                />
                <span class="text-xs text-secondary">
                  注册登录即表示同意
                  <a href="#" class="text-primary hover:underline">用户协议</a>
                  和
                  <a href="#" class="text-primary hover:underline">隐私政策</a>
                </span>
              </div>

              <!-- 登录按钮 -->
              <button
                class="w-full py-3 rounded-lg font-semibold text-white transition-colors"
                :class="isFormValid ? 'bg-primary hover:bg-primary-dark' : 'bg-gray-300 cursor-not-allowed'"
                :disabled="!isFormValid || isSubmitting"
                @click="handleLogin"
              >
                {{ isSubmitting ? '登录中...' : '登录' }}
              </button>
            </div>

            <!-- 微信扫码登录 -->
            <div v-show="activeTab === 'wechat'" class="flex flex-col items-center py-6">
              <!-- 加载状态 -->
              <div v-if="isQrcodeLoading" class="flex flex-col items-center justify-center h-56">
                <svg class="animate-spin w-8 h-8 text-primary" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                <p class="text-secondary text-sm mt-3">正在加载二维码...</p>
              </div>

              <!-- 二维码显示 -->
              <div v-else-if="wechatQrcodeUrl" class="flex flex-col items-center">
                <div class="relative w-52 h-52 border border-gray-100 rounded-lg overflow-hidden">
                  <img :src="wechatQrcodeUrl" alt="微信登录二维码" class="w-full h-full object-contain" />
                  <!-- 过期遮罩 -->
                  <div
                    v-if="wechatStatus === 'EXPIRED'"
                    class="absolute inset-0 bg-white/90 flex flex-col items-center justify-center"
                  >
                    <p class="text-secondary text-sm mb-2">二维码已过期</p>
                    <button
                      class="px-4 py-1.5 bg-primary text-white text-sm rounded-lg hover:bg-primary-dark transition-colors"
                      @click="handleRefreshQrcode"
                    >
                      点击刷新
                    </button>
                  </div>
                </div>

                <!-- 状态提示 -->
                <div class="mt-4 text-center">
                  <p v-if="wechatStatus === 'WAITING'" class="text-secondary text-sm">
                    请使用微信扫一扫登录
                  </p>
                  <p v-else-if="wechatStatus === 'SCANNED'" class="text-primary text-sm font-medium">
                    扫码成功，请在微信中确认登录
                  </p>
                  <p v-else-if="wechatStatus === 'CONFIRMED'" class="text-green-500 text-sm font-medium">
                    登录成功，正在跳转...
                  </p>
                </div>
              </div>

              <!-- 加载失败 -->
              <div v-else class="flex flex-col items-center justify-center h-56">
                <p class="text-secondary text-sm mb-3">二维码加载失败</p>
                <button
                  class="px-4 py-1.5 bg-primary text-white text-sm rounded-lg hover:bg-primary-dark transition-colors"
                  @click="handleRefreshQrcode"
                >
                  重新加载
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
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { AccountApiService } from '@/services'
import { useUserStore } from '@/stores'
import type { WechatScanStatus } from '@/types'

interface Props {
  show?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
})

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success'): void
}>()

const userStore = useUserStore()

// 表单状态
const activeTab = ref<'sms' | 'wechat'>('sms')
const phone = ref('')
const captcha = ref('')
const smsCode = ref('')
const isAgreed = ref(false)
const isSubmitting = ref(false)
const phoneError = ref('')

// 微信扫码登录状态
const isQrcodeLoading = ref(false)
const wechatQrcodeUrl = ref('')
const wechatSceneId = ref('')
const wechatStatus = ref<WechatScanStatus>('WAITING')
let pollTimer: ReturnType<typeof setInterval> | null = null

// 图形验证码
const captchaUrl = ref('')

// 倒计时
const countdown = ref(0)
const isCounting = computed(() => countdown.value > 0)

// 表单校验
const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(phone.value))
const isFormValid = computed(() => isPhoneValid.value && smsCode.value.length >= 4 && isAgreed.value)

// 手机号输入校验
watch(phone, (val) => {
  if (!val) {
    phoneError.value = ''
  } else if (val.length === 11 && !isPhoneValid.value) {
    phoneError.value = '请输入正确的手机号'
  } else {
    phoneError.value = ''
  }
})

// 弹窗打开时加载验证码
watch(() => props.show, (val) => {
  if (val) {
    activeTab.value = 'sms'
  } else {
    stopPolling()
  }
})

// Tab切换时处理微信二维码加载/轮询
watch(activeTab, (tab) => {
  if (tab === 'wechat') {
    handleRefreshQrcode()
  } else {
    stopPolling()
  }
})

/** 获取微信登录二维码 */
const handleRefreshQrcode = async () => {
  try {
    stopPolling()
    isQrcodeLoading.value = true
    wechatStatus.value = 'WAITING'
    const response = await AccountApiService.getWechatQrcode()
    if (response.code === 0 && response.data) {
      wechatQrcodeUrl.value = response.data.qrCodeUrl
      wechatSceneId.value = response.data.sceneId
      startPolling()
    } else {
      wechatQrcodeUrl.value = ''
    }
  } catch (error) {
    console.error('获取微信二维码失败:', error)
    wechatQrcodeUrl.value = ''
  } finally {
    isQrcodeLoading.value = false
  }
}

/** 开始轮询扫码状态 */
const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!wechatSceneId.value) return
    try {
      const response = await AccountApiService.getWechatLoginResult(wechatSceneId.value)
      if (response.code === 0 && response.data) {
        // 登录成功：success=true 且有 token
        if (response.data.success && response.data.token) {
          stopPolling()
          wechatStatus.value = 'CONFIRMED'
          // 存储token和用户信息
          localStorage.setItem('token', response.data.token)
          if (response.data.accountDTO) {
            localStorage.setItem('userInfo', JSON.stringify(response.data.accountDTO))
          }
          // 恢复用户状态
          userStore.restoreUser()
          emit('success')
          handleClose()
        } else if (response.data.qrCodeStatus === 'EXPIRED') {
          stopPolling()
          wechatStatus.value = 'EXPIRED'
        } else if (response.data.qrCodeStatus === 'SCANNED') {
          wechatStatus.value = 'SCANNED'
        }
      }
    } catch (error) {
      console.error('查询扫码状态失败:', error)
    }
  }, 2500)
}

/** 停止轮询 */
const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 刷新图形验证码 */
const handleRefreshCaptcha = () => {
  if (!phone.value) {
    phoneError.value = '请先输入手机号'
    return
  }
  if (!isPhoneValid.value) {
    phoneError.value = '请输入正确的手机号'
    return
  }
  captchaUrl.value = AccountApiService.getCaptchaUrl(phone.value, 'LOGIN')
}

/** 发送短信验证码 */
const handleSendCode = async () => {
  if (isCounting.value) return
  if (!isPhoneValid.value) {
    phoneError.value = '请输入正确的手机号'
    return
  }
  if (!captcha.value) {
    return
  }

  try {
    const response = await AccountApiService.sendCode({
      identifier: phone.value,
      captcha: captcha.value,
      type: 'LOGIN',
    })
    if (response.code === 0) {
      startCountdown()
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    handleRefreshCaptcha()
  }
}

/** 开始倒计时 */
const startCountdown = () => {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

/** 登录 */
const handleLogin = async () => {
  if (!isFormValid.value || isSubmitting.value) return

  try {
    isSubmitting.value = true
    const response = await userStore.login({
      identifier: phone.value,
      checkCode: smsCode.value,
      type: 'LOGIN',
    })
    if (response.code === 0 && response.data.success) {
      emit('success')
      handleClose()
    }
  } catch (error) {
    console.error('登录失败:', error)
    handleRefreshCaptcha()
  } finally {
    isSubmitting.value = false
  }
}

/** 关闭弹窗 */
const handleClose = () => {
  emit('update:show', false)
  stopPolling()
  // 重置表单
  phone.value = ''
  captcha.value = ''
  smsCode.value = ''
  phoneError.value = ''
  captchaUrl.value = ''
  // 重置微信状态
  wechatQrcodeUrl.value = ''
  wechatSceneId.value = ''
  wechatStatus.value = 'WAITING'
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
