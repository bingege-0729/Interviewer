<template>
  <div class="pt-20 pb-12 px-4 sm:px-6 lg:px-8 min-h-screen">
    <div class="max-w-7xl mx-auto">
      <div class="text-center mb-12">
        <h1 class="text-3xl font-bold text-dark mb-4">选择适合你的套餐</h1>
        <p class="text-secondary">灵活的定价方案，满足不同求职阶段的需求</p>
      </div>

      <!-- 套餐卡片 -->
      <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <p class="col-span-full text-center text-secondary py-12">加载套餐信息中...</p>
      </div>

      <div v-else-if="packageList.length" class="grid grid-cols-1 md:grid-cols-3 gap-8 mb-16">
        <div
          v-for="pkg in packageList"
          :key="pkg.id"
          class="relative rounded-2xl border p-6 flex flex-col transition-all duration-300 hover:shadow-lg"
          :class="pkg.isRecommended ? 'border-primary shadow-md scale-105' : 'border-gray-200'"
        >
          <!-- 推荐标签 -->
          <div
            v-if="pkg.isRecommended"
            class="absolute -top-3 left-1/2 -translate-x-1/2 bg-primary text-white text-xs font-semibold px-3 py-1 rounded-full"
          >
            推荐
          </div>

          <!-- 套餐名称 -->
          <h3 class="text-xl font-bold text-dark mb-2">{{ pkg.name }}</h3>
          <p class="text-secondary text-sm mb-4">{{ pkg.description }}</p>

          <!-- 价格 -->
          <div class="mb-6">
            <span class="text-3xl font-bold text-primary">¥{{ pkg.price }}</span>
            <span class="text-secondary text-sm ml-1">/{{ pkg.validDays }}天</span>
          </div>

          <!-- 包含权益 -->
          <ul class="space-y-2 mb-6 flex-1">
            <li
              v-for="benefit in pkg.benefits"
              :key="benefit.id"
              class="flex items-center text-sm text-dark"
            >
              <svg class="w-4 h-4 text-primary mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              <span>{{ benefit.name }}</span>
            </li>
          </ul>

          <!-- 购买按钮 -->
          <button
            class="w-full py-2.5 rounded-lg font-semibold transition-colors"
            :class="pkg.isRecommended ? 'bg-primary text-white hover:bg-primary-dark' : 'bg-gray-100 text-dark hover:bg-gray-200'"
            @click="handleBuyPackage(pkg)"
          >
            立即购买
          </button>
        </div>
      </div>

      <div v-else class="text-center py-12">
        <p class="text-secondary">暂无可用套餐</p>
      </div>

      <!-- 支付弹窗 -->
      <PaymentModal
        v-model:show="showPaymentModal"
        :order-type="paymentOrderType"
        :product-name="paymentProductName"
        :package-id="paymentPackageId"
        :benefit-id="paymentBenefitId"
        :purchase-count="paymentPurchaseCount"
        :total-amount="paymentTotalAmount"
        @success="handlePaymentSuccess"
      />

      <!-- 单项权益 -->
      <div v-if="benefitList.length" class="mt-8">
        <h2 class="text-2xl font-bold text-dark text-center mb-8">单项权益</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div
            v-for="benefit in benefitList"
            :key="benefit.id"
            class="rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow flex flex-col"
          >
            <div class="flex items-center justify-between mb-3">
              <h4 class="font-semibold text-dark">{{ benefit.name }}</h4>
              <span v-if="benefit.isHot" class="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded-full">热门</span>
            </div>
            <p class="text-secondary text-sm mb-3 flex-1">{{ benefit.description }}</p>
            <div class="flex items-center justify-between mb-4">
              <span class="text-lg font-bold text-primary">¥{{ benefit.unitPrice }}</span>
              <span class="text-xs text-secondary">有效期 {{ benefit.validDays }} 天</span>
            </div>

            <!-- 购买次数选择 -->
            <div class="flex items-center justify-between mb-3">
              <span class="text-sm text-secondary">购买次数</span>
              <div class="flex items-center space-x-2">
                <button
                  class="w-7 h-7 rounded border border-gray-300 flex items-center justify-center text-dark hover:bg-gray-100 transition-colors"
                  @click="decrementCount(benefit.id)"
                >
                  -
                </button>
                <span class="w-8 text-center text-sm font-semibold text-dark">{{ getBenefitCount(benefit.id) }}</span>
                <button
                  class="w-7 h-7 rounded border border-gray-300 flex items-center justify-center text-dark hover:bg-gray-100 transition-colors"
                  @click="incrementCount(benefit.id)"
                >
                  +
                </button>
              </div>
            </div>

            <!-- 购买按钮 -->
            <button
              class="w-full py-2 rounded-lg font-semibold bg-primary text-white hover:bg-primary-dark transition-colors text-sm"
              @click="handleBuyBenefit(benefit)"
            >
              立即购买 ¥{{ (benefit.unitPrice * getBenefitCount(benefit.id)).toFixed(2) }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ProductApiService } from '@/services'
import { PaymentModal } from '@/components/modals'
import type { PackageInfo, BenefitInfo } from '@/types'

const isLoading = ref(true)
const packageList = ref<PackageInfo[]>([])
const benefitList = ref<BenefitInfo[]>([])

// 权益购买次数管理
const benefitCounts = reactive<Record<number, number>>({})

const getBenefitCount = (benefitId: number): number => {
  return benefitCounts[benefitId] ?? 1
}

const incrementCount = (benefitId: number) => {
  benefitCounts[benefitId] = getBenefitCount(benefitId) + 1
}

const decrementCount = (benefitId: number) => {
  const current = getBenefitCount(benefitId)
  if (current > 1) {
    benefitCounts[benefitId] = current - 1
  }
}

// 支付弹窗状态
const showPaymentModal = ref(false)
const paymentOrderType = ref<'package' | 'benefit'>('package')
const paymentProductName = ref('')
const paymentPackageId = ref(0)
const paymentBenefitId = ref(0)
const paymentPurchaseCount = ref(1)
const paymentTotalAmount = ref(0)

/** 购买套餐 */
const handleBuyPackage = (pkg: PackageInfo) => {
  paymentOrderType.value = 'package'
  paymentProductName.value = pkg.name
  paymentPackageId.value = pkg.id
  paymentTotalAmount.value = pkg.price
  showPaymentModal.value = true
}

/** 购买权益 */
const handleBuyBenefit = (benefit: BenefitInfo) => {
  const count = getBenefitCount(benefit.id)
  paymentOrderType.value = 'benefit'
  paymentProductName.value = benefit.name
  paymentBenefitId.value = benefit.id
  paymentPurchaseCount.value = count
  paymentTotalAmount.value = benefit.unitPrice * count
  showPaymentModal.value = true
}

/** 支付成功回调 */
const handlePaymentSuccess = () => {
  showPaymentModal.value = false
}

const fetchPackages = async () => {
  try {
    const response = await ProductApiService.getPackages()
    if (response.code === 0) {
      packageList.value = response.data
    }
  } catch (error) {
    console.error('获取套餐列表失败:', error)
  }
}

const fetchBenefits = async () => {
  try {
    const response = await ProductApiService.getBenefits()
    if (response.code === 0) {
      benefitList.value = response.data
    }
  } catch (error) {
    console.error('获取权益列表失败:', error)
  }
}

onMounted(async () => {
  try {
    await Promise.all([fetchPackages(), fetchBenefits()])
  } finally {
    isLoading.value = false
  }
})
</script>
