<template>
  <div class="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-primary/5 to-light">
    <div class="w-full max-w-md">
      <div class="bg-white rounded-2xl shadow-xl p-8">
        <!-- Header -->
        <div class="text-center mb-8">
          <div class="w-12 h-12 bg-primary rounded-xl flex items-center justify-center mx-auto mb-4">
            <span class="text-white font-bold text-lg">AI</span>
          </div>
          <h1 class="text-2xl font-bold text-dark">登录 DCloud AI面试</h1>
          <p class="text-sm text-secondary mt-2">手机号验证码快速登录</p>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-dark mb-1.5">手机号</label>
            <input
              v-model="phone"
              type="tel"
              placeholder="请输入手机号"
              maxlength="11"
              class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-custom"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-dark mb-1.5">验证码</label>
            <div class="flex gap-3">
              <input
                v-model="code"
                type="text"
                placeholder="请输入验证码"
                maxlength="6"
                class="flex-1 px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-custom"
              />
              <button
                type="button"
                @click="handleSendCode"
                :disabled="isCounting"
                class="px-4 py-2.5 text-sm font-medium text-primary border border-primary rounded-lg hover:bg-primary/5 transition-custom disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
              >
                {{ isCounting ? `${countdown}s` : '获取验证码' }}
              </button>
            </div>
          </div>

          <button
            type="submit"
            :disabled="isLoading"
            class="w-full btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
        </form>

        <!-- Back -->
        <div class="mt-6 text-center">
          <router-link to="/" class="text-sm text-secondary hover:text-primary transition-custom">
            返回首页
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const phone = ref('')
const code = ref('')
const isLoading = ref(false)
const isCounting = ref(false)
const countdown = ref(60)

const handleSendCode = () => {
  if (!phone.value || phone.value.length !== 11) return
  isCounting.value = true
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      isCounting.value = false
      countdown.value = 60
    }
  }, 1000)
}

const handleLogin = async () => {
  if (!phone.value || !code.value) return
  try {
    isLoading.value = true
    await userStore.login({ identifier: phone.value, checkCode: code.value, type: 'LOGIN' })
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch {
    // 错误已在拦截器中处理
  } finally {
    isLoading.value = false
  }
}
</script>
