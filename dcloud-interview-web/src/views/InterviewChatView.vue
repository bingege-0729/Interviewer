<template>
  <div class="h-screen flex flex-col bg-gray-50">
    <!-- 顶部导航栏 -->
    <header class="flex items-center justify-between px-6 py-3 bg-white border-b border-gray-100 flex-shrink-0">
      <button @click="router.back()" class="flex items-center gap-2 text-secondary hover:text-dark transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        <span class="text-sm font-medium">返回</span>
      </button>
      <div class="flex-1 text-center px-4">
        <h1 class="text-sm font-bold text-dark truncate">{{ interviewTitle }}</h1>
        <p class="text-xs text-secondary mt-0.5">{{ interviewStatusText }}</p>
      </div>
      <div class="text-right flex-shrink-0 flex items-center gap-3">
        <button
          @click="handleFinishInterview"
          :disabled="isFinishing"
          class="px-4 py-2 text-sm font-medium border border-red-200 text-red-600 rounded-lg hover:bg-red-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1.5"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 10a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z"/></svg>
          {{ isFinishing ? '结束中...' : '结束面试' }}
        </button>
        <div class="text-right">
          <p class="text-xs text-secondary">总体进度</p>
          <div class="flex items-center gap-2 mt-0.5">
            <span class="text-lg font-bold text-primary">{{ totalAnswered }}</span>
            <span class="text-xs text-secondary">/{{ totalQuestions }}</span>
            <div class="w-20 h-2 bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full bg-primary rounded-full transition-all" :style="{ width: overallProgress + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="flex-1 flex items-center justify-center">
      <div class="text-center">
        <div class="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-secondary mt-4 text-sm">加载面试数据...</p>
      </div>
    </div>

    <!-- 主内容区 -->
    <div v-else class="flex-1 flex overflow-hidden">
      <!-- 左侧轮次面板 -->
      <aside class="w-[320px] flex-shrink-0 bg-white border-r border-gray-100 flex flex-col overflow-hidden">
        <!-- 轮次头部 -->
        <div class="px-5 py-4 border-b border-gray-100 flex-shrink-0">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
            <h2 class="text-base font-bold text-dark">面试轮次</h2>
          </div>
          <p class="text-xs text-secondary mt-1">共 {{ rounds.length }} 轮</p>
        </div>
        <!-- 轮次列表 -->
        <div class="flex-1 overflow-y-auto px-4 py-3 space-y-3">
          <div
            v-for="(round, roundIdx) in rounds"
            :key="round.id"
            class="border rounded-xl transition-all"
            :class="expandedRoundId === round.id ? 'border-primary/30 shadow-sm' : 'border-gray-100'"
          >
            <!-- 轮次卡片头部 -->
            <button
              class="w-full flex items-center gap-3 p-3 text-left"
              @click="toggleRound(round.id)"
            >
              <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 text-sm font-bold" :class="roundNumberClass(round)">
                {{ roundIdx + 1 }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-semibold text-dark truncate">{{ round.roundType }}</p>
                <p class="text-xs text-secondary">{{ round.answeredQuestions }}/{{ round.totalQuestion }} 题</p>
              </div>
              <div class="flex items-center gap-2 flex-shrink-0">
                <span class="text-xs px-2 py-0.5 rounded-full" :class="roundStatusClass(round.status)">{{ roundStatusText(round.status) }}</span>
                <svg class="w-4 h-4 text-gray-400 transition-transform" :class="{ 'rotate-180': expandedRoundId === round.id }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
              </div>
            </button>
            <!-- 轮次进度条 -->
            <div v-if="expandedRoundId === round.id" class="px-3 pb-2">
              <div class="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                <div class="h-full bg-primary rounded-full transition-all" :style="{ width: getRoundProgress(round) + '%' }"></div>
              </div>
            </div>
            <!-- 题目列表（展开时） -->
            <div v-if="expandedRoundId === round.id" class="px-3 pb-3 space-y-1.5">
              <button
                v-for="(question, qIdx) in round.questions"
                :key="question.id"
                class="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-left transition-colors"
                :class="selectedQuestionId === question.id ? 'bg-primary/10 border border-primary/30' : 'hover:bg-gray-50 border border-transparent'"
                @click="selectQuestion(round, question)"
              >
                <div class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0" :class="questionNumberClass(question)">
                  {{ qIdx + 1 }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-xs font-medium text-dark truncate">{{ question.category || round.roundType }}</p>
                  <p class="text-[10px] text-secondary">{{ question.maxScore }}分 · {{ question.timeLimit || 10 }}分钟</p>
                </div>
                <div class="flex-shrink-0">
                  <svg v-if="question.userAnswer" class="w-5 h-5 text-green-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
                  <div v-else class="w-5 h-5 rounded-full border-2 border-gray-300"></div>
                </div>
                <span v-if="question.userScore != null" class="text-xs font-semibold text-primary">{{ question.userScore }}/{{ question.maxScore }}</span>
              </button>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧题目内容区 -->
      <main class="flex-1 flex flex-col overflow-hidden">
        <!-- 未选择题目时 -->
        <div v-if="!selectedQuestion" class="flex-1 flex items-center justify-center">
          <div class="text-center">
            <svg class="w-16 h-16 text-gray-200 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            <p class="text-secondary mt-4 text-sm">请从左侧选择一道题目开始作答</p>
          </div>
        </div>

        <!-- 题目详情 -->
        <template v-else>
          <!-- 轮次标题栏 -->
          <div class="flex items-center justify-between px-6 py-3 bg-white border-b border-gray-100 flex-shrink-0">
            <div>
              <h2 class="text-base font-bold text-dark">第{{ selectedRound?.roundNumber }}轮：{{ selectedRound?.roundType }}</h2>
              <div v-if="selectedRound?.description" class="flex items-center gap-1.5 mt-1">
                <span class="px-2 py-0.5 text-[10px] font-medium text-primary bg-primary/10 rounded-full">考查点</span>
                <span class="text-xs text-secondary">{{ selectedRound.description }}</span>
              </div>
            </div>
            <div class="text-right">
              <p class="text-xs text-secondary">本轮进度</p>
              <p class="text-lg font-bold" :class="selectedRound && selectedRound.answeredQuestions >= selectedRound.totalQuestion ? 'text-green-600' : 'text-primary'">
                {{ selectedRound?.answeredQuestions || 0 }}/{{ selectedRound?.totalQuestion || 0 }}
              </p>
            </div>
          </div>

          <!-- 题目内容滚动区 -->
          <div class="flex-1 overflow-y-auto px-6 py-5">
            <!-- 题目头部信息 -->
            <div class="flex items-center gap-3 flex-wrap mb-4">
              <span class="px-3 py-1 text-xs font-bold text-white bg-primary rounded-full">第{{ selectedQuestionIndex + 1 }}题</span>
              <span class="px-2.5 py-1 text-xs font-medium text-primary bg-primary/10 rounded-full">{{ selectedQuestion.category || selectedRound?.roundType }}</span>
              <span class="px-2.5 py-1 text-xs font-medium text-orange-600 bg-orange-50 rounded-full">{{ selectedQuestion.maxScore }}分</span>
              <span class="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-secondary bg-gray-100 rounded-full">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                {{ formatTimeLimit(selectedQuestion.timeLimit) }}
              </span>
              <span class="ml-auto px-2.5 py-1 text-xs font-medium rounded-full" :class="questionAnswerStatusClass(selectedQuestion)">
                {{ questionAnswerStatusText(selectedQuestion) }}
              </span>
            </div>

            <!-- 题目内容 -->
            <div class="bg-white rounded-xl border border-gray-100 p-6 mb-6">
              <h3 class="flex items-center gap-2 text-sm font-bold text-dark mb-4">
                <svg class="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                题目内容
              </h3>
              <div class="text-sm text-dark leading-relaxed whitespace-pre-wrap">{{ selectedQuestion.content }}</div>
            </div>

            <!-- 关键要点 -->
            <div v-if="selectedQuestion.keyPoints" class="bg-gray-50 rounded-xl border border-gray-100 p-4 mb-6">
              <button @click="showKeyPoints = !showKeyPoints" class="flex items-center gap-2 text-sm font-medium text-secondary hover:text-dark transition-colors w-full text-left">
                <svg class="w-4 h-4 transition-transform" :class="{ 'rotate-90': showKeyPoints }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                查看本轮考查点
              </button>
              <div v-if="showKeyPoints" class="mt-3 text-xs text-secondary leading-relaxed">{{ selectedQuestion.keyPoints }}</div>
            </div>

            <!-- 已回答展示 -->
            <div v-if="selectedQuestion.userAnswer" class="bg-green-50 rounded-xl border border-green-100 p-5 mb-6">
              <div class="flex items-center justify-between mb-3">
                <h4 class="text-sm font-bold text-green-700 flex items-center gap-2">
                  <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
                  我的回答
                </h4>
                <span v-if="selectedQuestion.userScore != null" class="text-sm font-bold text-primary">得分：{{ selectedQuestion.userScore }}/{{ selectedQuestion.maxScore }}</span>
              </div>
              <p class="text-sm text-dark leading-relaxed whitespace-pre-wrap">{{ selectedQuestion.userAnswer }}</p>
            </div>

            <!-- AI评价 -->
            <div v-if="selectedQuestion.evaluation" class="bg-blue-50 rounded-xl border border-blue-100 p-5 mb-6">
              <h4 class="text-sm font-bold text-blue-700 flex items-center gap-2 mb-3">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
                AI评价
              </h4>
              <p class="text-sm text-dark leading-relaxed whitespace-pre-wrap">{{ selectedQuestion.evaluation }}</p>
            </div>
          </div>

          <!-- 答题输入区 -->
          <div v-if="!selectedQuestion.userAnswer" class="px-6 py-4 bg-white border-t border-gray-100 flex-shrink-0">
            <div class="mb-3">
              <label class="text-sm font-medium text-dark">请写下您的答案</label>
            </div>
            <textarea
              v-model="answerContent"
              rows="4"
              placeholder="请在此处输入您的答案..."
              class="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm resize-none focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-colors"
            ></textarea>
            <div class="flex items-center justify-between mt-3">
              <p class="text-xs text-secondary">{{ answerContent.length > 0 ? `已输入 ${answerContent.length} 字` : '' }}</p>
              <button
                @click="handleSubmitAnswer"
                :disabled="isSubmitting || !answerContent.trim()"
                class="px-6 py-2.5 bg-primary text-white text-sm font-medium rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <div v-if="isSubmitting" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                {{ isSubmitting ? '提交中...' : '提交答案' }}
              </button>
            </div>
          </div>
        </template>
      </main>
    </div>
  </div>

  <!-- 面试报告生成中弹窗 -->
  <Teleport to="body">
    <div v-if="showFinishToast" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>
      <div class="relative bg-white rounded-2xl shadow-2xl p-8 max-w-sm w-full text-center">
        <div class="w-14 h-14 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-7 h-7 text-primary animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
        </div>
        <h3 class="text-lg font-bold text-dark mb-2">面试已结束</h3>
        <p class="text-sm text-secondary leading-relaxed">面试报告正在生成中，请稍等片刻...</p>
        <div class="mt-4 w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
          <div class="h-full bg-primary rounded-full animate-pulse" style="width: 60%"></div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { InterviewApiService, QuestionApiService } from '@/services/interview'
import type { InterviewInfo, InterviewRoundDetail, QuestionInfo } from '@/types'

// Router
const route = useRoute()
const router = useRouter()

// 响应式状态
const isLoading = ref(true)
const interview = ref<InterviewInfo | null>(null)
const rounds = ref<InterviewRoundDetail[]>([])
const expandedRoundId = ref<number | null>(null)
const selectedRound = ref<InterviewRoundDetail | null>(null)
const selectedQuestion = ref<QuestionInfo | null>(null)
const selectedQuestionId = ref<number | null>(null)
const selectedQuestionIndex = ref(0)
const showKeyPoints = ref(false)
const answerContent = ref('')
const isSubmitting = ref(false)
const isFinishing = ref(false)
const showFinishToast = ref(false)

// 计算属性
const interviewId = computed(() => Number(route.params.id))

const interviewTitle = computed(() => {
  if (!interview.value) return '面试答题'
  return interview.value.title || '面试答题'
})

const interviewStatusText = computed(() => {
  if (!interview.value) return ''
  switch (interview.value.status) {
    case 'IN_PROGRESS': return '面试生成完成，可以开始答题'
    case 'EVALUATING': return '面试结束，评估中...'
    case 'COMPLETED': return '面试已完成'
    default: return ''
  }
})

const totalQuestions = computed(() => {
  return rounds.value.reduce((sum, r) => sum + r.totalQuestion, 0)
})

const totalAnswered = computed(() => {
  return rounds.value.reduce((sum, r) => sum + r.answeredQuestions, 0)
})

const overallProgress = computed(() => {
  if (totalQuestions.value === 0) return 0
  return Math.round((totalAnswered.value / totalQuestions.value) * 100)
})

// 方法
const getRoundProgress = (round: InterviewRoundDetail) => {
  if (round.totalQuestion === 0) return 0
  return Math.round((round.answeredQuestions / round.totalQuestion) * 100)
}

const toggleRound = (roundId: number) => {
  expandedRoundId.value = expandedRoundId.value === roundId ? null : roundId
}

const selectQuestion = (round: InterviewRoundDetail, question: QuestionInfo) => {
  selectedRound.value = round
  selectedQuestion.value = question
  selectedQuestionId.value = question.id
  selectedQuestionIndex.value = round.questions.findIndex(q => q.id === question.id)
  showKeyPoints.value = false
  answerContent.value = ''
}

const roundNumberClass = (round: InterviewRoundDetail) => {
  if (round.answeredQuestions >= round.totalQuestion && round.totalQuestion > 0) {
    return 'bg-green-500 text-white'
  }
  if (round.answeredQuestions > 0) {
    return 'bg-primary text-white'
  }
  return 'bg-orange-100 text-orange-600'
}

const roundStatusClass = (status: string | null) => {
  switch (status) {
    case 'COMPLETED': return 'bg-green-100 text-green-700'
    case 'IN_PROGRESS': return 'bg-primary/10 text-primary'
    case 'EVALUATING': return 'bg-yellow-100 text-yellow-700'
    default: return 'bg-gray-100 text-gray-600'
  }
}

const roundStatusText = (status: string | null) => {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'IN_PROGRESS': return '进行中'
    case 'EVALUATING': return '评估中'
    default: return '未开始'
  }
}

const questionNumberClass = (question: QuestionInfo) => {
  if (question.userAnswer) {
    return 'bg-green-500 text-white'
  }
  return 'bg-gray-200 text-gray-600'
}

const questionAnswerStatusClass = (question: QuestionInfo) => {
  if (question.userAnswer) {
    return 'bg-green-100 text-green-700'
  }
  return 'bg-red-50 text-red-600'
}

const questionAnswerStatusText = (question: QuestionInfo) => {
  if (question.userAnswer) {
    return question.userScore != null ? `已评分 ${question.userScore}分` : '已作答'
  }
  return '未作答'
}

const formatTimeLimit = (timeLimit: number | null) => {
  if (!timeLimit) return '10:00'
  const min = String(timeLimit).padStart(2, '0')
  return `${min}:00`
}

/** 加载面试详情数据 */
const fetchInterviewData = async () => {
  try {
    isLoading.value = true
    const response = await InterviewApiService.getDetail(interviewId.value)
    if (response.code === 0 && response.data) {
      interview.value = response.data.interview
      rounds.value = response.data.rounds || []
      // 自动展开第一个轮次
      if (rounds.value.length > 0) {
        expandedRoundId.value = rounds.value[0].id
        // 自动选择第一个未答题目
        const firstUnanswered = findFirstUnansweredQuestion()
        if (firstUnanswered) {
          selectQuestion(firstUnanswered.round, firstUnanswered.question)
        } else if (rounds.value[0].questions.length > 0) {
          selectQuestion(rounds.value[0], rounds.value[0].questions[0])
        }
      }
    }
  } catch (error) {
    console.error('加载面试详情失败:', error)
  } finally {
    isLoading.value = false
  }
}

/** 查找第一个未回答的题目 */
const findFirstUnansweredQuestion = () => {
  for (const round of rounds.value) {
    for (const question of round.questions) {
      if (!question.userAnswer) {
        return { round, question }
      }
    }
  }
  return null
}

/** 提交答案 */
const handleSubmitAnswer = async () => {
  if (!selectedQuestion.value || !answerContent.value.trim()) return

  try {
    isSubmitting.value = true
    const response = await QuestionApiService.submit({
      questionId: selectedQuestion.value.id,
      interviewId: interviewId.value,
      answer: answerContent.value.trim(),
    })
    if (response.code === 0) {
      // 更新本地数据
      if (response.data) {
        selectedQuestion.value.userAnswer = answerContent.value.trim()
        selectedQuestion.value.userScore = response.data.userScore
        selectedQuestion.value.evaluation = response.data.evaluation
        selectedQuestion.value.status = response.data.status
      } else {
        selectedQuestion.value.userAnswer = answerContent.value.trim()
      }
      // 更新轮次已答题数
      if (selectedRound.value) {
        selectedRound.value.answeredQuestions = selectedRound.value.questions.filter(q => q.userAnswer).length
      }
      answerContent.value = ''
      // 自动跳转到下一题
      autoSelectNextQuestion()
    } else {
      alert(response.message || '提交失败，请稍后重试')
    }
  } catch (error) {
    console.error('提交答案失败:', error)
    alert('提交答案失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

/** 自动选择下一题 */
const autoSelectNextQuestion = () => {
  const next = findFirstUnansweredQuestion()
  if (next) {
    expandedRoundId.value = next.round.id
    selectQuestion(next.round, next.question)
  } else {
    // 所有题目已答完，自动触发结束面试
    handleFinishInterview()
  }
}

/** 结束面试 */
const handleFinishInterview = async () => {
  if (isFinishing.value) return

  try {
    isFinishing.value = true
    const response = await InterviewApiService.finish(interviewId.value)
    if (response.code === 0) {
      // 显示报告生成中弹窗
      showFinishToast.value = true
      setTimeout(() => {
        showFinishToast.value = false
        router.push('/user')
      }, 3000)
    } else {
      alert(response.message || '结束面试失败，请稍后重试')
    }
  } catch (error) {
    console.error('结束面试失败:', error)
    alert('结束面试失败，请稍后重试')
  } finally {
    isFinishing.value = false
  }
}

// 生命周期
onMounted(() => {
  fetchInterviewData()
})
</script>
