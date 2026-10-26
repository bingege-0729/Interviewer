<template>
  <div class="pt-20 pb-12 px-4 sm:px-6 lg:px-8 min-h-screen">
    <div class="max-w-4xl mx-auto">
      <!-- 页面标题 -->
      <div class="text-center mb-10">
        <h1 class="text-3xl font-bold text-dark mb-3">上传简历，获取定制面试题</h1>
        <p class="text-secondary">上传您的简历，系统将自动分析并生成个性化面试题</p>
      </div>

      <!-- 上传卡片 -->
      <div class="bg-[#FFFDF5] rounded-2xl border border-gray-100 shadow-sm p-8 mb-8">
        <h3 class="text-base font-medium text-dark mb-4">上传您的简历 (PDF格式)</h3>

        <!-- 拖拽上传区域 -->
        <div
          class="relative border-2 border-dashed rounded-xl p-10 text-center transition-colors"
          :class="isDragOver ? 'border-primary bg-primary/5' : 'border-gray-300'"
          @dragover.prevent="handleDragOver"
          @dragleave.prevent="handleDragLeave"
          @drop.prevent="handleDrop"
        >
          <!-- 上传中 -->
          <div v-if="isUploading" class="flex flex-col items-center">
            <svg class="animate-spin w-10 h-10 text-primary mb-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            <p class="text-dark font-medium">正在上传并解析简历...</p>
            <p class="text-secondary text-sm mt-1">请耐心等待，这可能需要几秒钟</p>
          </div>

          <!-- 上传成功 -->
          <div v-else-if="uploadedResume" class="flex flex-col items-center">
            <svg class="w-12 h-12 text-green-500 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p class="text-dark font-medium mb-1">简历上传成功</p>
            <p class="text-secondary text-sm">{{ uploadedResume.filename }}</p>
            <button
              class="mt-4 px-4 py-2 text-sm text-primary border border-primary rounded-lg hover:bg-primary/5 transition-colors"
              @click="handleReUpload"
            >
              重新上传
            </button>
          </div>

          <!-- 默认上传区域 -->
          <div v-else class="flex flex-col items-center">
            <p class="text-dark mb-4">拖放PDF文件到此处，或点击选择文件</p>
            <button
              class="px-6 py-2.5 bg-primary text-white font-medium rounded-lg hover:bg-primary-dark transition-colors"
              @click="handleSelectFile"
            >
              选择文件
            </button>
            <p class="text-secondary text-sm mt-4">支持的格式：PDF（最大5MB）</p>
          </div>

          <!-- 隐藏文件输入框 -->
          <input
            ref="fileInputRef"
            type="file"
            accept=".pdf"
            class="hidden"
            @change="handleFileChange"
          />
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMessage" class="mt-4 flex items-center gap-2 text-red-500 text-sm">
          <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>
      </div>

      <!-- 提交按钮区域 -->
      <div class="text-center">
        <button
          class="px-8 py-3 rounded-lg font-medium text-base transition-colors inline-flex items-center gap-2"
          :class="uploadedResume ? 'bg-primary text-white hover:bg-primary-dark' : 'bg-gray-200 text-gray-500 cursor-not-allowed'"
          :disabled="!uploadedResume"
          @click="handleGoInterview"
        >
          {{ uploadedResume ? '开始面试' : '请先上传简历' }}
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
        </button>
        <p class="text-secondary text-sm mt-3 flex items-center justify-center gap-1.5">
          <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
          </svg>
          请先上传简历文件，系统将自动分析并生成个性化面试题
        </p>
      </div>

      <!-- 已上传简历列表 -->
      <div v-if="resumeList.length > 0" class="mt-12">
        <h2 class="text-lg font-bold text-dark mb-4">我的简历</h2>
        <div class="space-y-3">
          <div
            v-for="resume in resumeList"
            :key="resume.id"
            class="flex items-center justify-between p-4 bg-white border border-gray-100 rounded-xl hover:shadow-sm transition-shadow"
          >
            <div class="flex items-center gap-3">
              <!-- PDF图标 -->
              <div class="w-10 h-10 bg-red-50 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd" />
                </svg>
              </div>
              <div>
                <p class="text-sm font-medium text-dark">{{ resume.filename || '未命名简历' }}</p>
                <p class="text-xs text-secondary">{{ formatDate(resume.gmtCreate) }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button
                class="px-3 py-1.5 text-xs text-primary border border-primary/30 rounded-lg hover:bg-primary/5 transition-colors"
                @click="handleAnalyse(resume.id)"
              >
                分析
              </button>
              <button
                class="px-3 py-1.5 text-xs text-red-500 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
                @click="handleDelete(resume.id)"
              >
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ResumeApiService } from '@/services'
import type { ResumeInfo } from '@/types'

const router = useRouter()

// 响应式状态
const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragOver = ref(false)
const isUploading = ref(false)
const uploadedResume = ref<ResumeInfo | null>(null)
const errorMessage = ref('')
const resumeList = ref<ResumeInfo[]>([])

// 文件大小限制 5MB
const MAX_FILE_SIZE = 5 * 1024 * 1024

/** 选择文件 */
const handleSelectFile = () => {
  fileInputRef.value?.click()
}

/** 文件输入变化 */
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    validateAndUpload(file)
  }
  // 重置 input 值以支持重复选择同一文件
  target.value = ''
}

/** 拖拽进入 */
const handleDragOver = () => {
  isDragOver.value = true
}

/** 拖拽离开 */
const handleDragLeave = () => {
  isDragOver.value = false
}

/** 拖拽放置 */
const handleDrop = (event: DragEvent) => {
  isDragOver.value = false
  const file = event.dataTransfer?.files[0]
  if (file) {
    validateAndUpload(file)
  }
}

/** 校验并上传文件 */
const validateAndUpload = async (file: File) => {
  errorMessage.value = ''

  // 校验文件类型
  if (file.type !== 'application/pdf') {
    errorMessage.value = '仅支持 PDF 格式文件，请重新选择'
    return
  }

  // 校验文件大小
  if (file.size > MAX_FILE_SIZE) {
    errorMessage.value = '文件大小不能超过 5MB，请压缩后重试'
    return
  }

  // 上传文件
  try {
    isUploading.value = true
    const response = await ResumeApiService.upload(file)
    if (response.code === 0 && response.data) {
      uploadedResume.value = response.data
      // 刷新简历列表
      await fetchResumeList()
    } else {
      errorMessage.value = '上传失败，请稍后重试'
    }
  } catch (error) {
    console.error('简历上传失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '上传失败，请稍后重试'
  } finally {
    isUploading.value = false
  }
}

/** 重新上传 */
const handleReUpload = () => {
  uploadedResume.value = null
  errorMessage.value = ''
}

/** 跳转面试创建页 */
const handleGoInterview = () => {
  if (uploadedResume.value) {
    router.push({
      path: '/interview/create',
      query: { resumeId: uploadedResume.value.id },
    })
  }
}

/** 分析简历 */
const handleAnalyse = async (id: number) => {
  try {
    const response = await ResumeApiService.analyse(id)
    if (response.code === 0) {
      // 刷新列表获取最新状态
      await fetchResumeList()
    }
  } catch (error) {
    console.error('简历分析失败:', error)
  }
}

/** 删除简历 */
const handleDelete = async (id: number) => {
  try {
    const response = await ResumeApiService.delete(id)
    if (response.code === 0) {
      // 如果删除的是当前上传的简历，清除状态
      if (uploadedResume.value?.id === id) {
        uploadedResume.value = null
      }
      await fetchResumeList()
    }
  } catch (error) {
    console.error('删除简历失败:', error)
  }
}

/** 获取简历列表 */
const fetchResumeList = async () => {
  try {
    const response = await ResumeApiService.getList()
    if (response.code === 0) {
      resumeList.value = response.data || []
    }
  } catch (error) {
    console.error('获取简历列表失败:', error)
  }
}

/** 格式化日期 */
const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  fetchResumeList()
})
</script>
