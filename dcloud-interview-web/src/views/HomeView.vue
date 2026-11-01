<template>
  <div class="min-h-screen">
    <!-- Hero + Banner Section -->
    <section class="pt-24 pb-16 px-4 sm:px-6 lg:px-8">
      <div class="max-w-7xl mx-auto flex flex-col lg:flex-row items-center gap-10 lg:gap-16">
        <!-- 左侧文案 -->
        <div class="flex-shrink-0 lg:w-[42%] text-center lg:text-left">
          <div class="brand-pill mb-6 inline-block">AI 驱动面试</div>
          <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-dark leading-tight mb-6">
            智能面试，精准提升<br />
            <span class="text-primary">轻松拿下心仪 Offer</span>
          </h1>
          <p class="text-base text-secondary mb-8">
            基于AI大模型，根据你的简历自动生成定制化面试题目，模拟真实面试场景，实时评分反馈，助你高效备战。
          </p>
          <div class="flex items-center justify-center lg:justify-start gap-4">
            <router-link to="/interview/create" class="btn-primary">
              开始面试
            </router-link>
            <router-link to="/pricing" class="btn-secondary">
              查看套餐
            </router-link>
          </div>
        </div>

        <!-- 右侧轮播图 -->
        <div class="w-full lg:w-[58%]">
          <div v-if="isBannerLoading" class="w-full aspect-[16/9] bg-gray-100 rounded-xl animate-pulse" />
          <div v-else-if="bannerList.length > 0" class="relative rounded-xl overflow-hidden shadow-lg">
            <Swiper
              :modules="swiperModules"
              :autoplay="{ delay: 4000, disableOnInteraction: false }"
              :pagination="{ clickable: true }"
              :navigation="true"
              :loop="true"
              class="banner-swiper"
            >
              <SwiperSlide v-for="banner in bannerList" :key="banner.id">
                <a
                  :href="banner.pcLink ? (banner.pcLink.startsWith('http') ? banner.pcLink : `https://${banner.pcLink}`) : undefined"
                  :target="banner.pcLink ? '_blank' : undefined"
                  class="block relative w-full aspect-[16/9]"
                >
                  <img
                    :src="banner.imgUrl || ''"
                    :alt="banner.name || '轮播图'"
                    class="w-full h-full object-cover"
                    @error="handleImgError"
                  />
                  <!-- 文字覆盖层 -->
                  <div v-if="banner.text" class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent px-6 py-4">
                    <p class="text-white text-sm sm:text-base font-medium">{{ banner.text }}</p>
                  </div>
                </a>
              </SwiperSlide>
            </Swiper>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="py-16 bg-white px-4 sm:px-6 lg:px-8">
      <div class="max-w-7xl mx-auto">
        <h2 class="text-3xl font-bold text-dark text-center mb-12">核心功能</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div class="p-6 rounded-xl border border-gray-100 card-hover">
            <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
              <span class="text-2xl">📄</span>
            </div>
            <h3 class="text-lg font-semibold text-dark mb-2">简历智能分析</h3>
            <p class="text-sm text-secondary">AI深度解析简历，识别优势与不足，提供针对性优化建议。</p>
          </div>
          <div class="p-6 rounded-xl border border-gray-100 card-hover">
            <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
              <span class="text-2xl">🎯</span>
            </div>
            <h3 class="text-lg font-semibold text-dark mb-2">定制化面试题</h3>
            <p class="text-sm text-secondary">根据简历和目标岗位，AI自动生成多轮高质量面试题目。</p>
          </div>
          <div class="p-6 rounded-xl border border-gray-100 card-hover">
            <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
              <span class="text-2xl">📊</span>
            </div>
            <h3 class="text-lg font-semibold text-dark mb-2">全维度评估</h3>
            <p class="text-sm text-secondary">技术能力、逻辑思维、表达能力、问题解决力四维评分。</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Autoplay, Pagination, Navigation } from 'swiper/modules'
import { BannerApiService } from '@/services'
import type { BannerInfo } from '@/types'
import 'swiper/css'
import 'swiper/css/pagination'
import 'swiper/css/navigation'

const swiperModules = [Autoplay, Pagination, Navigation]

const bannerList = ref<BannerInfo[]>([])
const isBannerLoading = ref(false)

/** 图片加载失败占位 */
const handleImgError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjNmNGY2Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMjAiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjOWNhM2FmIj7lm77niYfliqDovb3lpLHotKU8L3RleHQ+PC9zdmc+'
}

/** 获取轮播图列表 */
const fetchBannerList = async () => {
  try {
    isBannerLoading.value = true
    const response = await BannerApiService.getList()
    if (response.code === 0) {
      bannerList.value = response.data
    }
  } catch (error) {
    console.error('获取轮播图失败:', error)
  } finally {
    isBannerLoading.value = false
  }
}

onMounted(() => {
  fetchBannerList()
})
</script>

<style scoped>
.banner-swiper {
  border-radius: 0.75rem;
}

.banner-swiper :deep(.swiper-pagination-bullet) {
  background: white;
  opacity: 0.6;
}

.banner-swiper :deep(.swiper-pagination-bullet-active) {
  opacity: 1;
}

.banner-swiper :deep(.swiper-button-prev),
.banner-swiper :deep(.swiper-button-next) {
  color: white;
  background: rgba(0, 0, 0, 0.3);
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.banner-swiper :deep(.swiper-button-prev::after),
.banner-swiper :deep(.swiper-button-next::after) {
  font-size: 14px;
  font-weight: bold;
}
</style>
