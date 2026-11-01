import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { AccountApiService } from '@/services'
import type { AccountInfo, AccountBenefit, LoginParams } from '@/types'

export const useUserStore = defineStore('user', () => {
  // 响应式状态
  const userInfo = ref<AccountInfo | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))
  const benefits = ref<AccountBenefit[]>([])
  const isLoading = ref(false)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const headImg = computed(() => userInfo.value?.headImg || '')

  /**
   * 用户登录
   */
  const login = async (params: LoginParams) => {
    try {
      isLoading.value = true
      const response = await AccountApiService.login(params)
      if (response.code === 0 && response.data.success) {
        token.value = response.data.token
        userInfo.value = response.data.accountDTO
        localStorage.setItem('token', response.data.token)
        localStorage.setItem('userInfo', JSON.stringify(response.data.accountDTO))
      }
      return response
    } catch (error) {
      console.error('登录失败:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 退出登录
   */
  const logout = async () => {
    try {
      await AccountApiService.logout()
    } catch (error) {
      console.error('退出登录请求失败:', error)
    } finally {
      token.value = null
      userInfo.value = null
      benefits.value = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  /**
   * 获取用户详情
   */
  const fetchUserInfo = async () => {
    try {
      isLoading.value = true
      const response = await AccountApiService.getDetail()
      if (response.code === 0) {
        userInfo.value = response.data
        localStorage.setItem('userInfo', JSON.stringify(response.data))
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 获取用户权益
   */
  const fetchBenefits = async () => {
    try {
      const response = await AccountApiService.getBenefits()
      if (response.code === 0) {
        benefits.value = response.data
      }
    } catch (error) {
      console.error('获取用户权益失败:', error)
      throw error
    }
  }

  /**
   * 从本地存储恢复用户状态
   */
  const restoreUser = () => {
    const savedToken = localStorage.getItem('token')
    const savedUserInfo = localStorage.getItem('userInfo')
    if (savedToken) {
      token.value = savedToken
    }
    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo)
      } catch {
        localStorage.removeItem('userInfo')
      }
    }
  }

  return {
    // 状态
    userInfo,
    token,
    benefits,
    isLoading,
    // 计算属性
    isLoggedIn,
    username,
    headImg,
    // 方法
    login,
    logout,
    fetchUserInfo,
    fetchBenefits,
    restoreUser,
  }
})
