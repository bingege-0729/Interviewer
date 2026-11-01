<template>
  <div class="min-h-screen bg-gray-50 pb-12">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="flex items-center justify-center py-32">
      <div class="text-center">
        <div class="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-secondary mt-4 text-sm">加载面试报告...</p>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="flex items-center justify-center py-32">
      <div class="text-center">
        <svg class="w-16 h-16 text-gray-200 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/></svg>
        <p class="text-secondary mt-4">{{ loadError }}</p>
        <button @click="fetchReportData" class="mt-4 px-5 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary-dark transition-colors">重新加载</button>
      </div>
    </div>

    <!-- 报告内容 -->
    <template v-else-if="interview">
      <!-- 顶部概览 -->
      <div class="bg-white border-b border-gray-100">
        <div class="max-w-5xl mx-auto px-6 py-8">
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-3 mb-2">
                <span class="px-3 py-1 text-xs font-bold text-white bg-green-500 rounded-full">已完成</span>
                <span class="text-xs text-secondary">{{ formatDate(interview.gmtCreate) }}</span>
              </div>
              <h1 class="text-xl font-bold text-dark mb-1 truncate">{{ interview.title || '面试报告' }}</h1>
              <p class="text-sm text-secondary">{{ interview.description }}</p>
            </div>
            <div class="flex-shrink-0 text-center">
              <div class="w-20 h-20 rounded-full border-4 flex items-center justify-center" :class="overallScoreClass">
                <span class="text-2xl font-bold">{{ interview.overallScore || 0 }}</span>
              </div>
              <p class="text-xs text-secondary mt-1.5">综合评分</p>
            </div>
          </div>

          <!-- 维度评分 -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
            <div class="bg-blue-50 rounded-xl p-4 text-center">
              <p class="text-xs text-blue-600 font-medium mb-1">技术能力</p>
              <p class="text-2xl font-bold text-blue-700">{{ interview.technicalSkills || 0 }}</p>
              <div class="w-full bg-blue-100 rounded-full h-1.5 mt-2">
                <div class="bg-blue-500 h-1.5 rounded-full" :style="{ width: getScorePercent(interview.technicalSkills) + '%' }"></div>
              </div>
            </div>
            <div class="bg-purple-50 rounded-xl p-4 text-center">
              <p class="text-xs text-purple-600 font-medium mb-1">逻辑思维</p>
              <p class="text-2xl font-bold text-purple-700">{{ interview.logicalThinking || 0 }}</p>
              <div class="w-full bg-purple-100 rounded-full h-1.5 mt-2">
                <div class="bg-purple-500 h-1.5 rounded-full" :style="{ width: getScorePercent(interview.logicalThinking) + '%' }"></div>
              </div>
            </div>
            <div class="bg-orange-50 rounded-xl p-4 text-center">
              <p class="text-xs text-orange-600 font-medium mb-1">沟通表达</p>
              <p class="text-2xl font-bold text-orange-700">{{ interview.communication || 0 }}</p>
              <div class="w-full bg-orange-100 rounded-full h-1.5 mt-2">
                <div class="bg-orange-500 h-1.5 rounded-full" :style="{ width: getScorePercent(interview.communication) + '%' }"></div>
              </div>
            </div>
            <div class="bg-green-50 rounded-xl p-4 text-center">
              <p class="text-xs text-green-600 font-medium mb-1">问题解决</p>
              <p class="text-2xl font-bold text-green-700">{{ interview.problemSolving || 0 }}</p>
              <div class="w-full bg-green-100 rounded-full h-1.5 mt-2">
                <div class="bg-green-500 h-1.5 rounded-full" :style="{ width: getScorePercent(interview.problemSolving) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 报告主体 -->
      <div class="max-w-5xl mx-auto px-6 mt-8 space-y-6">
        <!-- 综合评价 -->
        <section v-if="interview.summary" class="bg-white rounded-2xl shadow-sm p-6">
          <h2 class="flex items-center gap-2 text-base font-bold text-dark mb-4">
            <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            综合评价
          </h2>
          <div class="prose prose-sm max-w-none" v-html="renderMarkdown(interview.summary)"></div>
        </section>

        <!-- 突出优势 -->
        <section v-if="interview.strength" class="bg-white rounded-2xl shadow-sm p-6">
          <h2 class="flex items-center gap-2 text-base font-bold text-green-700 mb-4">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            突出优势
          </h2>
          <div class="prose prose-sm max-w-none" v-html="renderMarkdown(interview.strength)"></div>
        </section>

        <!-- 改进建议 -->
        <section v-if="interview.improvement" class="bg-white rounded-2xl shadow-sm p-6">
          <h2 class="flex items-center gap-2 text-base font-bold text-orange-600 mb-4">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/></svg>
            需要改进
          </h2>
          <div class="prose prose-sm max-w-none" v-html="renderMarkdown(interview.improvement)"></div>
        </section>

        <!-- 发展建议 -->
        <section v-if="interview.suggestion" class="bg-white rounded-2xl shadow-sm p-6">
          <h2 class="flex items-center gap-2 text-base font-bold text-blue-600 mb-4">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/></svg>
            发展建议
          </h2>
          <div class="prose prose-sm max-w-none" v-html="renderMarkdown(interview.suggestion)"></div>
        </section>

        <!-- 各轮次详情 -->
        <section v-if="rounds.length > 0" class="bg-white rounded-2xl shadow-sm p-6">
          <h2 class="flex items-center gap-2 text-base font-bold text-dark mb-6">
            <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
            各轮次详情
          </h2>

          <div class="space-y-6">
            <div v-for="(round, roundIdx) in rounds" :key="round.id" class="border border-gray-100 rounded-xl overflow-hidden">
              <!-- 轮次头部 -->
              <button class="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 transition-colors text-left" @click="toggleRound(round.id)">
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold bg-primary text-white">{{ roundIdx + 1 }}</div>
                  <div>
                    <p class="text-sm font-semibold text-dark">{{ round.roundType }}</p>
                    <p class="text-xs text-secondary">{{ round.totalQuestion }} 题 · 得分 {{ round.overallScore || '-' }}</p>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <span class="text-xs px-2 py-0.5 rounded-full" :class="round.status === 'COMPLETED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'">
                    {{ round.status === 'COMPLETED' ? '已完成' : '进行中' }}
                  </span>
                  <svg class="w-4 h-4 text-gray-400 transition-transform" :class="{ 'rotate-180': expandedRoundId === round.id }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                </div>
              </button>

              <!-- 轮次展开内容 -->
              <div v-if="expandedRoundId === round.id" class="p-4 space-y-4">
                <!-- 轮次评价 -->
                <div v-if="round.summary" class="bg-gray-50 rounded-lg p-4">
                  <p class="text-xs font-medium text-secondary mb-2">本轮评价</p>
                  <div class="prose prose-sm max-w-none" v-html="renderMarkdown(round.summary)"></div>
                </div>

                <!-- 题目列表 -->
                <div class="space-y-3">
                  <div v-for="(question, qIdx) in round.questions" :key="question.id" class="border border-gray-100 rounded-lg p-4">
                    <div class="flex items-center justify-between mb-2">
                      <div class="flex items-center gap-2">
                        <span class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold" :class="question.userAnswer ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-600'">{{ qIdx + 1 }}</span>
                        <span class="text-xs px-2 py-0.5 bg-primary/10 text-primary rounded-full">{{ question.category || round.roundType }}</span>
                      </div>
                      <span class="text-sm font-semibold" :class="getScoreColor(question.userScore, question.maxScore)">
                        {{ question.userScore != null ? `${question.userScore}/${question.maxScore}` : `未评分/${question.maxScore}` }}
                      </span>
                    </div>
                    <!-- 题目内容 -->
                    <div class="prose prose-sm max-w-none mb-3" v-html="renderMarkdown(question.content)"></div>
                    <!-- 回答 -->
                    <div v-if="question.userAnswer" class="bg-green-50 rounded-lg p-3 mb-2">
                      <p class="text-xs font-medium text-green-700 mb-1">我的回答</p>
                      <p class="text-xs text-dark leading-relaxed whitespace-pre-wrap">{{ question.userAnswer }}</p>
                    </div>
                    <div v-else class="bg-red-50 rounded-lg p-3 mb-2">
                      <p class="text-xs font-medium text-red-600">未作答</p>
                    </div>
                    <!-- AI评价 -->
                    <div v-if="question.evaluation" class="bg-blue-50 rounded-lg p-3">
                      <p class="text-xs font-medium text-blue-700 mb-2">AI评价</p>
                      <div class="prose prose-xs max-w-none" v-html="renderMarkdown(question.evaluation)"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 返回按钮 -->
        <div class="text-center pt-4">
          <button @click="router.push('/user')" class="px-6 py-2.5 bg-primary text-white text-sm font-medium rounded-lg hover:bg-primary-dark transition-colors">
            返回个人中心
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { InterviewApiService } from '@/services/interview'
import type { InterviewInfo, InterviewRoundDetail } from '@/types'

/** 渲染 Markdown 为 HTML */
const renderMarkdown = (text: string | null): string => {
  if (!text) return ''
  return marked.parse(text, { async: false }) as string
}

const route = useRoute()
const router = useRouter()

const isLoading = ref(true)
const loadError = ref('')
const interview = ref<InterviewInfo | null>(null)
const rounds = ref<InterviewRoundDetail[]>([])
const expandedRoundId = ref<number | null>(null)

const interviewId = computed(() => Number(route.params.id))

/** 综合评分样式 */
const overallScoreClass = computed(() => {
  const score = interview.value?.overallScore || 0
  if (score >= 8) return 'border-green-400 text-green-600'
  if (score >= 5) return 'border-blue-400 text-blue-600'
  if (score >= 3) return 'border-orange-400 text-orange-600'
  return 'border-red-400 text-red-600'
})

/** 维度评分百分比（满分10分） */
const getScorePercent = (score: number | null) => {
  if (!score) return 0
  return Math.min(Math.round((score / 10) * 100), 100)
}

/** 题目得分颜色 */
const getScoreColor = (userScore: number | null, maxScore: number) => {
  if (userScore == null) return 'text-gray-400'
  const ratio = userScore / maxScore
  if (ratio >= 0.8) return 'text-green-600'
  if (ratio >= 0.5) return 'text-blue-600'
  if (ratio >= 0.3) return 'text-orange-600'
  return 'text-red-600'
}

/** 轮次展开/折叠 */
const toggleRound = (roundId: number) => {
  expandedRoundId.value = expandedRoundId.value === roundId ? null : roundId
}

/** 格式化日期 */
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

/** 加载报告数据 */
const fetchReportData = async () => {
  try {
    isLoading.value = true
    loadError.value = ''
    const response = await InterviewApiService.getDetail(interviewId.value)
    if (response.code === 0 && response.data) {
      interview.value = response.data.interview
      rounds.value = response.data.rounds || []
      // 默认展开第一轮
      if (rounds.value.length > 0) {
        expandedRoundId.value = rounds.value[0].id
      }
    } else {
      loadError.value = response.message || '加载失败'
    }
  } catch (error) {
    console.error('加载面试报告失败:', error)
    loadError.value = '网络错误，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchReportData()
})
</script>

<style scoped>
/* Markdown 渲染样式 */
:deep(.prose) {
  color: #1f2937;
  line-height: 1.75;
}
:deep(.prose h1) {
  font-size: 1.25rem;
  font-weight: 700;
  margin-top: 1.25rem;
  margin-bottom: 0.75rem;
}
:deep(.prose h2) {
  font-size: 1.125rem;
  font-weight: 700;
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}
:deep(.prose h3) {
  font-size: 1rem;
  font-weight: 600;
  margin-top: 0.75rem;
  margin-bottom: 0.5rem;
}
:deep(.prose p) {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}
:deep(.prose ul) {
  list-style-type: disc;
  padding-left: 1.5rem;
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}
:deep(.prose ol) {
  list-style-type: decimal;
  padding-left: 1.5rem;
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}
:deep(.prose li) {
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
}
:deep(.prose strong) {
  font-weight: 700;
}
:deep(.prose code) {
  background-color: #f3f4f6;
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-size: 0.85em;
  color: #dc2626;
}
:deep(.prose pre) {
  background-color: #1f2937;
  color: #e5e7eb;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
}
:deep(.prose pre code) {
  background: none;
  color: inherit;
  padding: 0;
  border-radius: 0;
  font-size: 0.8125rem;
}
:deep(.prose blockquote) {
  border-left: 4px solid #6366f1;
  padding-left: 1rem;
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
  color: #4b5563;
  font-style: italic;
}
:deep(.prose hr) {
  border-color: #e5e7eb;
  margin-top: 1rem;
  margin-bottom: 1rem;
}
:deep(.prose-xs) {
  font-size: 0.75rem;
}
:deep(.prose-sm) {
  font-size: 0.875rem;
}
</style>
