<template>
  <div class="pt-20 pb-12 px-4 sm:px-6 lg:px-8 min-h-screen">
    <div class="max-w-3xl mx-auto">
      <h1 class="text-2xl font-bold text-dark mb-8">创建面试</h1>

      <form @submit.prevent="handleSubmit" class="space-y-6">
        <!-- 选择简历 -->
        <div>
          <label class="block text-sm font-medium text-dark mb-1.5">选择简历</label>
          <div v-if="isLoadingResumes" class="flex items-center gap-2 px-4 py-2.5 border border-gray-200 rounded-lg">
            <div class="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            <span class="text-sm text-secondary">加载简历列表中...</span>
          </div>
          <select
            v-else
            v-model="form.resumeId"
            class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 appearance-none bg-white"
          >
            <option value="">请选择简历</option>
            <option v-for="resume in resumeList" :key="resume.id" :value="resume.id">
              {{ resume.filename || '未命名简历' }}
            </option>
          </select>
          <p v-if="resumeList.length === 0 && !isLoadingResumes" class="text-xs text-secondary mt-1.5">
            暂无简历，请先到
            <router-link to="/user" class="text-primary hover:underline">个人中心</router-link>
            上传简历
          </p>
        </div>

        <!-- 面试类型 -->
        <div>
          <label class="block text-sm font-medium text-dark mb-1.5">面试类型</label>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <label
              class="flex items-center p-4 border rounded-lg cursor-pointer transition-custom"
              :class="form.interviewType === 'COMMON_INTERVIEW' ? 'border-primary bg-primary/5' : 'border-gray-200 hover:border-primary'"
            >
              <input type="radio" v-model="form.interviewType" value="COMMON_INTERVIEW" class="mr-3" />
              <span class="text-sm">简历综合面</span>
            </label>
            <label
              class="flex items-center p-4 border rounded-lg cursor-pointer transition-custom"
              :class="form.interviewType === 'SPECIAL_INTERVIEW' ? 'border-primary bg-primary/5' : 'border-gray-200 hover:border-primary'"
            >
              <input type="radio" v-model="form.interviewType" value="SPECIAL_INTERVIEW" class="mr-3" />
              <span class="text-sm">专项面试</span>
            </label>
          </div>
        </div>

        <!-- 目标岗位 -->
        <div>
          <label class="block text-sm font-medium text-dark mb-1.5">目标岗位</label>
          <input
            v-model="form.targetPosition"
            type="text"
            placeholder="例如：高级前端工程师"
            class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
          />
        </div>

        <!-- 工作年限 + 期望薪资 -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-dark mb-1.5">工作年限</label>
            <input
              v-model="form.workYears"
              type="text"
              placeholder="例如：5年"
              class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-dark mb-1.5">期望薪资</label>
            <input
              v-model="form.expectedSalary"
              type="text"
              placeholder="例如：20k"
              class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            />
          </div>
        </div>

        <!-- 目标城市 -->
        <div>
          <label class="block text-sm font-medium text-dark mb-1.5">目标城市</label>
          <input
            v-model="form.targetCity"
            type="text"
            placeholder="例如：广州"
            class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
          />
        </div>

        <!-- 专项内容（仅专项面试显示） -->
        <div v-if="form.interviewType === 'SPECIAL_INTERVIEW'">
          <label class="block text-sm font-medium text-dark mb-1.5">专项内容</label>
          <textarea
            v-model="form.specialContent"
            rows="3"
            placeholder="请输入专项面试的具体内容或方向"
            class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary/50"
          ></textarea>
        </div>

        <button
          type="submit"
          :disabled="isSubmitting || !isFormValid"
          class="w-full btn-primary flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <div v-if="isSubmitting" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
          {{ isSubmitting ? '生成中...' : '开始生成面试' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ResumeApiService, InterviewApiService } from '@/services/interview'
import type { ResumeInfo, CreateInterviewParams } from '@/types'
import { InterviewType } from '@/types'

const router = useRouter()

const isLoadingResumes = ref(false)
const isSubmitting = ref(false)
const resumeList = ref<ResumeInfo[]>([])

const form = reactive({
  resumeId: '' as number | '',
  interviewType: 'COMMON_INTERVIEW' as string,
  targetPosition: '',
  workYears: '',
  expectedSalary: '',
  targetCity: '',
  specialContent: '',
})

/** 表单是否有效 */
const isFormValid = computed(() => {
  return (
    form.resumeId !== '' &&
    form.targetPosition.trim() !== '' &&
    form.workYears.trim() !== '' &&
    form.expectedSalary.trim() !== '' &&
    form.targetCity.trim() !== ''
  )
})

/** 加载简历列表 */
const fetchResumeList = async () => {
  try {
    isLoadingResumes.value = true
    const response = await ResumeApiService.getList()
    if (response.code === 0 && response.data) {
      resumeList.value = response.data
    }
  } catch (error) {
    console.error('加载简历列表失败:', error)
  } finally {
    isLoadingResumes.value = false
  }
}

/** 提交创建面试 */
const handleSubmit = async () => {
  if (!isFormValid.value || isSubmitting.value) return

  try {
    isSubmitting.value = true
    const params: CreateInterviewParams = {
      resumeId: form.resumeId as number,
      interviewType: form.interviewType as InterviewType,
      targetPosition: form.targetPosition.trim(),
      workYears: form.workYears.trim(),
      expectedSalary: form.expectedSalary.trim(),
      targetCity: form.targetCity.trim(),
      specialContent: form.specialContent.trim() || undefined,
    }
    const response = await InterviewApiService.create(params)
    if (response.code === 0) {
      alert('面试创建成功，面试题目生成中，请稍后到个人中心查看')
      router.push('/user')
    } else {
      alert(response.message || '创建失败，请稍后重试')
    }
  } catch (error) {
    console.error('创建面试失败:', error)
    alert('创建面试失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  fetchResumeList()
})
</script>
