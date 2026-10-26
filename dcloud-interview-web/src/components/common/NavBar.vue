<template>
  <nav class="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <router-link to="/" class="flex items-center gap-2">
          <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
            <span class="text-white font-bold text-sm">AI</span>
          </div>
          <span class="text-lg font-bold text-dark">DCloud面试</span>
        </router-link>

        <!-- Desktop Navigation -->
        <div class="hidden md:flex items-center gap-1">
          <router-link
            to="/"
            class="px-4 py-2 text-sm font-medium text-secondary hover:text-primary rounded-lg transition-custom"
            active-class="text-primary bg-primary/5"
          >
            首页
          </router-link>
          <router-link
            to="/resume"
            class="px-4 py-2 text-sm font-medium text-secondary hover:text-primary rounded-lg transition-custom"
            active-class="text-primary bg-primary/5"
          >
            简历管理
          </router-link>
          <router-link
            to="/pricing"
            class="px-4 py-2 text-sm font-medium text-secondary hover:text-primary rounded-lg transition-custom"
            active-class="text-primary bg-primary/5"
          >
            套餐价格
          </router-link>
        </div>

        <!-- Right Section -->
        <div class="flex items-center gap-3">
          <template v-if="isLoggedIn">
            <router-link to="/user" class="flex items-center gap-2 hover:opacity-80 transition-custom">
              <div class="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center overflow-hidden">
                <img v-if="headImg" :src="headImg" alt="avatar" class="w-full h-full object-cover" />
                <span v-else class="text-primary text-xs font-medium">{{ username.charAt(0).toUpperCase() }}</span>
              </div>
              <span class="hidden sm:inline text-sm font-medium text-dark max-w-[100px] truncate">{{ username }}</span>
            </router-link>
            <button
              @click="handleLogout"
              class="text-sm text-secondary hover:text-red-500 transition-custom"
            >
              退出
            </button>
          </template>
          <template v-else>
            <button @click="showLoginModal = true" class="btn-primary text-sm">
              一键登录
            </button>
          </template>

          <!-- Mobile Menu Button -->
          <button
            @click="showMobileMenu = !showMobileMenu"
            class="md:hidden p-2 text-secondary hover:text-dark"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                v-if="!showMobileMenu"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 6h16M4 12h16M4 18h16"
              />
              <path
                v-else
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Mobile Menu -->
    <div v-if="showMobileMenu" class="md:hidden border-t border-gray-100 bg-white">
      <div class="px-4 py-3 space-y-1">
        <router-link
          to="/"
          class="block px-3 py-2 text-sm font-medium text-secondary hover:text-primary rounded-lg"
          @click="showMobileMenu = false"
        >
          首页
        </router-link>
        <router-link
          to="/resume"
          class="block px-3 py-2 text-sm font-medium text-secondary hover:text-primary rounded-lg"
          @click="showMobileMenu = false"
        >
          简历管理
        </router-link>
        <router-link
          to="/pricing"
          class="block px-3 py-2 text-sm font-medium text-secondary hover:text-primary rounded-lg"
          @click="showMobileMenu = false"
        >
          套餐价格
        </router-link>
      </div>
    </div>
  </nav>

  <!-- 登录弹窗 -->
  <LoginModal v-model:show="showLoginModal" @success="handleLoginSuccess" />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { LoginModal } from '@/components/modals'

const router = useRouter()
const userStore = useUserStore()

const showMobileMenu = ref(false)
const showLoginModal = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)
const username = computed(() => userStore.username)
const headImg = computed(() => userStore.headImg)

const handleLogout = async () => {
  await userStore.logout()
  router.push('/')
}

const handleLoginSuccess = () => {
  showLoginModal.value = false
}
</script>
