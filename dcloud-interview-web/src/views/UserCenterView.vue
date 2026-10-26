<template>
  <div class="pt-20 pb-12 px-4 sm:px-6 lg:px-8 min-h-screen">
    <div class="max-w-7xl mx-auto">

      <!-- 加载状态 -->
      <div v-if="isPageLoading" class="flex items-center justify-center py-20">
        <div class="text-center">
          <div class="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
          <p class="text-secondary mt-4 text-sm">加载中...</p>
        </div>
      </div>

      <template v-else>
        <div class="flex flex-col lg:flex-row gap-6">

          <!-- 左侧边栏 -->
          <aside class="w-full lg:w-[280px] flex-shrink-0">
            <div class="bg-white rounded-2xl shadow-sm p-6 sticky top-24">
              <!-- 用户信息 -->
              <div class="flex items-start gap-4 mb-6">
                <div class="relative">
                  <div class="w-16 h-16 rounded-full overflow-hidden flex-shrink-0">
                    <img v-if="userStore.headImg" :src="userStore.headImg" alt="头像" class="w-full h-full object-cover" />
                    <div v-else class="w-full h-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-white text-xl font-bold">
                      {{ avatarText }}
                    </div>
                  </div>
                  <div class="absolute bottom-0 left-0 w-3.5 h-3.5 bg-green-500 rounded-full border-2 border-white"></div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2">
                    <h3 class="font-semibold text-dark truncate">{{ userStore.username || '未设置昵称' }}</h3>
                    <button class="text-gray-400 hover:text-primary transition-colors flex-shrink-0">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
                    </button>
                  </div>
                  <p class="text-xs text-secondary mt-0.5">ID: {{ userStore.userInfo?.id || '--' }}</p>
                  <span class="inline-flex items-center mt-2 px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-secondary">
                    <svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/></svg>
                    {{ hasValidBenefit ? 'VIP会员' : '普通用户' }}
                  </span>
                </div>
              </div>

              <!-- 导航菜单 -->
              <nav class="space-y-1">
                <button
                  v-for="item in menuItems"
                  :key="item.key"
                  class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors"
                  :class="activeMenu === item.key ? 'bg-primary text-white shadow-sm' : 'text-dark hover:bg-gray-50'"
                  @click="handleMenuClick(item.key)"
                >
                  <span v-html="item.icon"></span>
                  <span class="flex-1 text-left">{{ item.label }}</span>
                  <span
                    v-if="item.key === 'interview' && totalRecord > 0"
                    class="px-2 py-0.5 text-xs rounded-full"
                    :class="activeMenu === 'interview' ? 'bg-white/20 text-white' : 'bg-primary/10 text-primary'"
                  >{{ totalRecord }}</span>
                </button>
              </nav>

              <!-- 创建新面试按钮 -->
              <router-link
                to="/interview/create"
                class="mt-6 w-full flex items-center justify-center gap-2 py-3 bg-primary text-white rounded-xl font-medium hover:bg-primary-dark transition-colors shadow-sm"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
                创建新面试
              </router-link>
            </div>
          </aside>

          <!-- 右侧内容区 -->
          <main class="flex-1 min-w-0">

            <!-- ========== AI面试面板 ========== -->
            <div v-if="activeMenu === 'interview'" class="bg-white rounded-2xl shadow-sm p-6 lg:p-8">
              <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center">
                    <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
                  </div>
                  <div>
                    <h1 class="text-xl font-bold text-dark">AI面试中心</h1>
                    <p class="text-sm text-secondary">智能面试，精准评估</p>
                  </div>
                </div>
                <router-link to="/interview/create" class="hidden sm:flex items-center gap-2 px-5 py-2.5 bg-primary text-white rounded-lg font-medium hover:bg-primary-dark transition-colors text-sm">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
                  创建新面试
                </router-link>
              </div>

              <!-- 面试列表加载中 -->
              <div v-if="isInterviewLoading" class="flex items-center justify-center py-16">
                <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
              </div>

              <!-- 空状态 -->
              <div v-else-if="interviewList.length === 0" class="text-center py-16">
                <svg class="w-16 h-16 text-gray-200 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
                <p class="text-secondary mt-4">暂无面试记录</p>
                <router-link to="/interview/create" class="inline-block mt-4 px-5 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary-dark transition-colors">
                  创建第一次面试
                </router-link>
              </div>

              <!-- 面试记录列表 -->
              <div v-else class="space-y-4">
                <div v-for="item in interviewList" :key="item.id" class="border border-gray-100 rounded-xl p-5 hover:border-primary/30 hover:shadow-sm transition-all">
                  <div class="flex items-start justify-between gap-3">
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 flex-wrap">
                        <h3 class="font-semibold text-dark truncate">{{ item.title || '未命名面试' }}</h3>
                        <span class="px-2 py-0.5 text-xs rounded-full flex-shrink-0" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
                      </div>
                      <p class="text-sm text-secondary mt-1">{{ statusDescription(item) }}</p>
                    </div>
                    <router-link v-if="item.status === InterviewStatus.IN_PROGRESS" :to="`/interview/${item.id}/chat`" class="flex-shrink-0 flex items-center gap-1.5 px-4 py-2 border border-primary text-primary rounded-lg text-sm font-medium hover:bg-primary hover:text-white transition-colors">
                      <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd"/></svg>
                      继续面试
                    </router-link>
                    <router-link v-else-if="item.status === InterviewStatus.COMPLETED" :to="`/interview/${item.id}/report`" class="flex-shrink-0 flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-primary border border-primary/30 rounded-lg hover:bg-primary hover:text-white transition-colors">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                      查看详情
                    </router-link>
                  </div>
                  <div class="flex items-center gap-6 mt-3 text-sm">
                    <div class="flex items-center gap-1.5 text-secondary">
                      <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                      <span>创建时间</span>
                      <span class="font-medium text-dark">{{ formatDate(item.gmtCreate) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div v-if="totalPage > 1" class="flex items-center justify-center gap-2 mt-8">
                <button class="flex items-center gap-1 px-4 py-2 text-sm rounded-lg border transition-colors" :class="currentPage === 1 ? 'border-gray-200 text-gray-300 cursor-not-allowed' : 'border-gray-200 text-secondary hover:bg-gray-50'" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                  上一页
                </button>
                <button v-for="page in displayPages" :key="page" class="w-9 h-9 flex items-center justify-center text-sm rounded-lg transition-colors" :class="page === currentPage ? 'bg-primary text-white font-medium' : 'text-secondary hover:bg-gray-50'" @click="goToPage(page)">
                  {{ page }}
                </button>
                <button class="flex items-center gap-1 px-4 py-2 text-sm rounded-lg border transition-colors" :class="currentPage === totalPage ? 'border-gray-200 text-gray-300 cursor-not-allowed' : 'border-gray-200 text-secondary hover:bg-gray-50'" :disabled="currentPage === totalPage" @click="goToPage(currentPage + 1)">
                  下一页
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                </button>
              </div>
              <p v-if="totalPage > 0" class="text-center text-xs text-secondary mt-2">第 {{ currentPage }} / {{ totalPage }} 页</p>
            </div>

            <!-- ========== 简历管理面板 ========== -->
            <div v-else-if="activeMenu === 'resume'" class="bg-white rounded-2xl shadow-sm p-6 lg:p-8">
              <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center">
                    <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                  </div>
                  <div>
                    <h1 class="text-xl font-bold text-dark">简历管理</h1>
                    <p class="text-sm text-secondary">管理和上传您的简历</p>
                  </div>
                </div>
                <router-link to="/resume" class="hidden sm:flex items-center gap-2 px-5 py-2.5 bg-primary text-white rounded-lg font-medium hover:bg-primary-dark transition-colors text-sm">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/></svg>
                  上传简历
                </router-link>
              </div>

              <div v-if="isResumeLoading" class="flex items-center justify-center py-16">
                <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
              </div>
              <div v-else-if="resumeList.length === 0" class="text-center py-16">
                <svg class="w-16 h-16 text-gray-200 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                <p class="text-secondary mt-4">暂无简历</p>
                <router-link to="/resume" class="inline-block mt-4 px-5 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary-dark transition-colors">
                  去上传简历
                </router-link>
              </div>
              <div v-else class="space-y-3">
                <div v-for="resume in resumeList" :key="resume.id" class="border border-gray-100 rounded-xl p-4 hover:border-primary/30 transition-colors">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3 flex-1 min-w-0">
                      <div class="w-10 h-10 bg-red-50 rounded-lg flex items-center justify-center flex-shrink-0">
                        <svg class="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/></svg>
                      </div>
                      <div class="flex-1 min-w-0">
                        <p class="font-medium text-dark truncate">{{ resume.filename || '未命名简历' }}</p>
                        <p class="text-xs text-secondary mt-0.5">{{ formatDate(resume.gmtCreate) }}</p>
                      </div>
                    </div>
                    <div class="flex items-center gap-2 flex-shrink-0">
                      <span class="px-2 py-0.5 text-xs rounded-full" :class="resumeStatusClass(resume.status)">
                        {{ resumeStatusText(resume.status) }}
                      </span>
                    </div>
                  </div>
                  <!-- 操作按钮 -->
                  <div class="flex items-center gap-2 mt-3 pt-3 border-t border-gray-50">
                    <button
                      class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
                      :disabled="resumeActionLoading[resume.id] === 'preview'"
                      @click="handleResumePreview(resume)"
                    >
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
                      <span v-if="resumeActionLoading[resume.id] === 'preview'">预览中...</span>
                      <span v-else>预览</span>
                    </button>
                    <button
                      class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"
                      :class="(resume.status === 'ANALYZED' || resume.status === 'IN_PROCESS') ? 'text-gray-400 bg-gray-100 cursor-not-allowed' : 'text-primary bg-primary/10 hover:bg-primary/20'"
                      :disabled="resumeActionLoading[resume.id] === 'analyse' || resume.status === 'ANALYZED' || resume.status === 'IN_PROCESS'"
                      @click="handleResumeAnalyse(resume)"
                    >
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
                      <span v-if="resumeActionLoading[resume.id] === 'analyse'">分析中...</span>
                      <span v-else-if="resume.status === 'IN_PROCESS'">分析中</span>
                      <span v-else-if="resume.status === 'ANALYZED'">已分析</span>
                      <span v-else>AI分析</span>
                    </button>
                    <button
                      v-if="resume.status === 'ANALYZED' && resume.evaluation"
                      class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-green-600 bg-green-50 rounded-lg hover:bg-green-100 transition-colors"
                      @click="handleViewReport(resume)"
                    >
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                      <span>查看报告</span>
                    </button>
                    <button
                      class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-white bg-primary rounded-lg hover:bg-primary-dark transition-colors"
                      @click="openCreateInterviewModal(resume)"
                    >
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                      <span>开始面试</span>
                    </button>
                    <button
                      class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-red-600 bg-red-50 rounded-lg hover:bg-red-100 transition-colors ml-auto"
                      :disabled="resumeActionLoading[resume.id] === 'delete'"
                      @click="handleResumeDelete(resume)"
                    >
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                      <span v-if="resumeActionLoading[resume.id] === 'delete'">删除中...</span>
                      <span v-else>删除</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- ========== 我的订单面板 ========== -->
            <div v-else-if="activeMenu === 'order'" class="bg-white rounded-2xl shadow-sm p-6 lg:p-8">
              <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-green-50 rounded-xl flex items-center justify-center">
                    <svg class="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
                  </div>
                  <div>
                    <h1 class="text-xl font-bold text-dark">我的订单</h1>
                    <p class="text-sm text-secondary">查看购买记录</p>
                  </div>
                </div>
              </div>

              <div v-if="isOrderLoading" class="flex items-center justify-center py-16">
                <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
              </div>
              <div v-else-if="orderList.length === 0" class="text-center py-16">
                <svg class="w-16 h-16 text-gray-200 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
                <p class="text-secondary mt-4">暂无订单记录</p>
                <router-link to="/pricing" class="inline-block mt-4 px-5 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary-dark transition-colors">
                  去购买套餐
                </router-link>
              </div>
              <div v-else class="space-y-3">
                <div v-for="order in orderList" :key="order.id" class="flex items-center justify-between p-4 border border-gray-100 rounded-xl hover:border-primary/30 transition-colors">
                  <div class="flex-1 min-w-0">
                    <p class="font-medium text-dark truncate">{{ order.title || order.outTradeNo }}</p>
                    <p class="text-xs text-secondary mt-0.5">{{ formatDate(order.gmtCreate) }}</p>
                  </div>
                  <div class="flex items-center gap-3 flex-shrink-0">
                    <span class="font-semibold text-dark">¥{{ (order.payAmount / 100).toFixed(2) }}</span>
                    <span class="px-2 py-0.5 text-xs rounded-full" :class="orderStateClass(order.orderState)">
                      {{ orderStateText(order.orderState) }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- 订单分页 -->
              <div v-if="orderTotalPage > 1" class="flex items-center justify-center gap-2 mt-8">
                <button class="flex items-center gap-1 px-4 py-2 text-sm rounded-lg border transition-colors" :class="orderCurrentPage === 1 ? 'border-gray-200 text-gray-300 cursor-not-allowed' : 'border-gray-200 text-secondary hover:bg-gray-50'" :disabled="orderCurrentPage === 1" @click="goToOrderPage(orderCurrentPage - 1)">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                  上一页
                </button>
                <button v-for="page in orderDisplayPages" :key="page" class="w-9 h-9 flex items-center justify-center text-sm rounded-lg transition-colors" :class="page === orderCurrentPage ? 'bg-primary text-white font-medium' : 'text-secondary hover:bg-gray-50'" @click="goToOrderPage(page)">
                  {{ page }}
                </button>
                <button class="flex items-center gap-1 px-4 py-2 text-sm rounded-lg border transition-colors" :class="orderCurrentPage === orderTotalPage ? 'border-gray-200 text-gray-300 cursor-not-allowed' : 'border-gray-200 text-secondary hover:bg-gray-50'" :disabled="orderCurrentPage === orderTotalPage" @click="goToOrderPage(orderCurrentPage + 1)">
                  下一页
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                </button>
              </div>
              <p v-if="orderTotalPage > 0" class="text-center text-xs text-secondary mt-2">第 {{ orderCurrentPage }} / {{ orderTotalPage }} 页，共 {{ orderTotalRecord }} 条记录</p>
            </div>

            <!-- ========== 权益套餐面板 ========== -->
            <div v-else-if="activeMenu === 'pricing'" class="bg-white rounded-2xl shadow-sm p-6 lg:p-8">
              <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center">
                    <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7"/></svg>
                  </div>
                  <div>
                    <h1 class="text-xl font-bold text-dark">权益套餐</h1>
                    <p class="text-sm text-secondary">我的会员权益</p>
                  </div>
                </div>
                <router-link to="/pricing" class="hidden sm:flex items-center gap-2 px-5 py-2.5 bg-primary text-white rounded-lg font-medium hover:bg-primary-dark transition-colors text-sm">
                  升级套餐
                </router-link>
              </div>

              <!-- 权益卡片 -->
              <div v-if="isBenefitLoading" class="flex items-center justify-center py-16">
                <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
              </div>
              <div v-else-if="benefitList.length === 0" class="text-center py-16">
                <svg class="w-16 h-16 text-gray-200 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7"/></svg>
                <p class="text-secondary mt-4">暂无权益</p>
                <router-link to="/pricing" class="inline-block mt-4 px-5 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary-dark transition-colors">
                  购买套餐
                </router-link>
              </div>
              <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div v-for="benefit in benefitList" :key="benefit.id" class="border border-gray-100 rounded-xl p-5 hover:border-primary/30 transition-colors">
                  <div class="flex items-center justify-between mb-2">
                    <h3 class="font-semibold text-dark">{{ benefit.benefitName || benefitName(benefit.benefitCode) }}</h3>
                    <span class="text-xs text-secondary">{{ formatDate(benefit.endTime).split(' ')[0] }} 到期</span>
                  </div>
                  <p v-if="benefit.benefitDescription" class="text-xs text-secondary mb-3">{{ benefit.benefitDescription }}</p>
                  <div class="flex items-end gap-1 mb-2">
                    <span class="text-2xl font-bold text-primary">{{ benefit.remainingCount }}</span>
                    <span class="text-sm text-secondary mb-0.5">/ {{ benefit.totalCount }} 次</span>
                  </div>
                  <div class="w-full bg-gray-100 rounded-full h-2">
                    <div class="bg-primary h-2 rounded-full transition-all" :style="{ width: Math.round(((benefit.totalCount - benefit.remainingCount) / benefit.totalCount) * 100) + '%' }"></div>
                  </div>
                  <p class="text-xs text-secondary mt-2">已使用 {{ benefit.totalCount - benefit.remainingCount }} 次</p>
                </div>
              </div>

              <!-- 权益分页 -->
              <div v-if="benefitTotalPage > 1" class="flex items-center justify-center gap-2 mt-8">
                <button class="flex items-center gap-1 px-4 py-2 text-sm rounded-lg border transition-colors" :class="benefitCurrentPage === 1 ? 'border-gray-200 text-gray-300 cursor-not-allowed' : 'border-gray-200 text-secondary hover:bg-gray-50'" :disabled="benefitCurrentPage === 1" @click="goToBenefitPage(benefitCurrentPage - 1)">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
                  上一页
                </button>
                <button v-for="page in benefitDisplayPages" :key="page" class="w-9 h-9 flex items-center justify-center text-sm rounded-lg transition-colors" :class="page === benefitCurrentPage ? 'bg-primary text-white font-medium' : 'text-secondary hover:bg-gray-50'" @click="goToBenefitPage(page)">
                  {{ page }}
                </button>
                <button class="flex items-center gap-1 px-4 py-2 text-sm rounded-lg border transition-colors" :class="benefitCurrentPage === benefitTotalPage ? 'border-gray-200 text-gray-300 cursor-not-allowed' : 'border-gray-200 text-secondary hover:bg-gray-50'" :disabled="benefitCurrentPage === benefitTotalPage" @click="goToBenefitPage(benefitCurrentPage + 1)">
                  下一页
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                </button>
              </div>
              <p v-if="benefitTotalPage > 0" class="text-center text-xs text-secondary mt-2">第 {{ benefitCurrentPage }} / {{ benefitTotalPage }} 页，共 {{ benefitTotalRecord }} 条记录</p>
            </div>

            <!-- ========== 账号设置面板 ========== -->
            <div v-else-if="activeMenu === 'settings'" class="bg-white rounded-2xl shadow-sm p-6 lg:p-8">
              <div class="flex items-center gap-3 mb-6">
                <div class="w-10 h-10 bg-gray-100 rounded-xl flex items-center justify-center">
                  <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                </div>
                <div>
                  <h1 class="text-xl font-bold text-dark">账号设置</h1>
                  <p class="text-sm text-secondary">管理您的账号信息</p>
                </div>
              </div>

              <div class="space-y-4">
                <div class="flex items-center justify-between p-4 border border-gray-100 rounded-xl">
                  <div>
                    <p class="font-medium text-dark">手机号</p>
                    <p class="text-sm text-secondary mt-0.5">{{ maskedPhone }}</p>
                  </div>
                </div>
                <div class="flex items-center justify-between p-4 border border-gray-100 rounded-xl">
                  <div>
                    <p class="font-medium text-dark">昵称</p>
                    <p class="text-sm text-secondary mt-0.5">{{ userStore.username || '未设置' }}</p>
                  </div>
                </div>
                <div class="pt-4">
                  <button @click="handleLogout" class="w-full sm:w-auto px-6 py-2.5 border border-red-200 text-red-600 rounded-lg text-sm font-medium hover:bg-red-50 transition-colors">
                    退出登录
                  </button>
                </div>
              </div>
            </div>

          </main>
        </div>
      </template>

    </div>
  </div>

  <!-- ========== 创建面试弹窗 ========== -->
  <Teleport to="body">
    <div v-if="showCreateInterviewModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showCreateInterviewModal = false">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg flex flex-col">
        <!-- 弹窗头部 -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 flex-shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 bg-primary/10 rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            </div>
            <div>
              <h2 class="text-lg font-bold text-dark">创建面试</h2>
              <p class="text-xs text-secondary">简历：{{ interviewFormResumeName }}</p>
            </div>
          </div>
          <button @click="showCreateInterviewModal = false" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-gray-100 transition-colors text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <!-- 表单内容 -->
        <div class="px-6 py-5 space-y-4 overflow-y-auto max-h-[60vh]">
          <div>
            <label class="block text-sm font-medium text-dark mb-1.5">目标岗位 <span class="text-red-500">*</span></label>
            <input v-model="interviewForm.targetPosition" type="text" placeholder="如：Java后端开发工程师" class="w-full px-3 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-colors" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-dark mb-1.5">期望薪资 <span class="text-red-500">*</span></label>
              <input v-model="interviewForm.expectedSalary" type="text" placeholder="如：20-30K" class="w-full px-3 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-colors" />
            </div>
            <div>
              <label class="block text-sm font-medium text-dark mb-1.5">工作年限 <span class="text-red-500">*</span></label>
              <input v-model="interviewForm.workYears" type="text" placeholder="如：3年" class="w-full px-3 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-colors" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-dark mb-1.5">目标城市 <span class="text-red-500">*</span></label>
            <input v-model="interviewForm.targetCity" type="text" placeholder="如：深圳" class="w-full px-3 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-colors" />
          </div>
          <div>
            <label class="block text-sm font-medium text-dark mb-1.5">面试类型 <span class="text-red-500">*</span></label>
            <div class="flex gap-3">
              <label class="flex items-center gap-2 px-4 py-2.5 border rounded-lg cursor-pointer transition-colors" :class="interviewForm.interviewType === 'COMMON_INTERVIEW' ? 'border-primary bg-primary/5 text-primary' : 'border-gray-200 text-secondary hover:border-gray-300'">
                <input v-model="interviewForm.interviewType" type="radio" value="COMMON_INTERVIEW" class="hidden" />
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
                <span class="text-sm font-medium">通用面试</span>
              </label>
              <label class="flex items-center gap-2 px-4 py-2.5 border rounded-lg cursor-pointer transition-colors" :class="interviewForm.interviewType === 'SPECIAL_INTERVIEW' ? 'border-primary bg-primary/5 text-primary' : 'border-gray-200 text-secondary hover:border-gray-300'">
                <input v-model="interviewForm.interviewType" type="radio" value="SPECIAL_INTERVIEW" class="hidden" />
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
                <span class="text-sm font-medium">专项面试</span>
              </label>
            </div>
          </div>
          <div v-if="interviewForm.interviewType === 'SPECIAL_INTERVIEW'">
            <label class="block text-sm font-medium text-dark mb-1.5">专项内容</label>
            <textarea v-model="interviewForm.specialContent" rows="3" placeholder="请输入专项面试的具体内容方向，如：Redis、分布式系统" class="w-full px-3 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 transition-colors resize-none"></textarea>
          </div>
          <!-- 表单错误提示 -->
          <p v-if="interviewFormError" class="text-xs text-red-500 flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/></svg>
            {{ interviewFormError }}
          </p>
        </div>
        <!-- 弹窗底部 -->
        <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-100 flex-shrink-0">
          <button @click="showCreateInterviewModal = false" class="px-5 py-2 text-sm font-medium text-secondary border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors">
            取消
          </button>
          <button @click="handleCreateInterview" :disabled="isCreatingInterview" class="px-5 py-2 text-sm font-medium text-white bg-primary rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2">
            <div v-if="isCreatingInterview" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            {{ isCreatingInterview ? '创建中...' : '开始面试' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- ========== 简历分析报告弹窗 ========== -->
  <Teleport to="body">
    <div v-if="showReportModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showReportModal = false">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <!-- 弹窗内容 -->
      <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[85vh] flex flex-col">
        <!-- 弹窗头部 -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 flex-shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 bg-green-50 rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            </div>
            <div>
              <h2 class="text-lg font-bold text-dark">简历分析报告</h2>
              <p class="text-xs text-secondary">{{ reportResumeName }}</p>
            </div>
          </div>
          <button @click="showReportModal = false" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-gray-100 transition-colors text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <!-- 弹窗滚动内容 -->
        <div class="flex-1 overflow-y-auto px-6 py-5 space-y-6">
          <template v-if="parsedReport">
            <!-- 总体评价 -->
            <section>
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-primary rounded-full"></span>总体评价
              </h3>
              <p class="text-sm text-dark leading-relaxed bg-gray-50 rounded-xl p-4">{{ parsedReport.overallSummary?.evaluation || parsedReport.evaluation }}</p>
            </section>

            <!-- 定位分析 -->
            <section v-if="parsedReport.basicPosition">
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-blue-500 rounded-full"></span>定位分析
              </h3>
              <div class="space-y-3">
                <div class="flex items-center gap-2">
                  <span class="px-2 py-0.5 text-xs rounded-full bg-blue-100 text-blue-700 font-medium">{{ parsedReport.basicPosition.level === 'SENIOR' ? '高级' : parsedReport.basicPosition.level === 'MID' ? '中级' : parsedReport.basicPosition.level }}</span>
                  <span class="text-sm text-secondary">岗位匹配度：{{ parsedReport.basicPosition.positionFitDegree?.substring(0, 30) }}...</span>
                </div>
                <p class="text-xs text-secondary leading-relaxed">{{ parsedReport.basicPosition.levelJudgmentBasis }}</p>
                <div v-if="parsedReport.basicPosition.potentialGap" class="bg-yellow-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-yellow-700 mb-1">潜在差距</p>
                  <p class="text-xs text-yellow-600 leading-relaxed">{{ parsedReport.basicPosition.potentialGap }}</p>
                </div>
              </div>
            </section>

            <!-- 技能标签 -->
            <section v-if="parsedSkillTags.length > 0">
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-purple-500 rounded-full"></span>技能标签
              </h3>
              <div class="flex flex-wrap gap-2">
                <span v-for="tag in parsedSkillTags" :key="tag" class="px-2.5 py-1 text-xs rounded-full bg-purple-50 text-purple-700 font-medium">
                  {{ tag }}
                </span>
              </div>
            </section>

            <!-- 技能分析 -->
            <section v-if="parsedReport.skillAnalysis">
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-green-500 rounded-full"></span>技能分析
              </h3>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div class="bg-green-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-green-700 mb-2">优势</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.skillAnalysis.advantages" :key="idx" class="text-xs text-green-600 leading-relaxed flex gap-1.5">
                      <span class="text-green-400 flex-shrink-0">✓</span>{{ item }}
                    </li>
                  </ul>
                </div>
                <div class="bg-red-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-red-700 mb-2">不足</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.skillAnalysis.disadvantages" :key="idx" class="text-xs text-red-600 leading-relaxed flex gap-1.5">
                      <span class="text-red-400 flex-shrink-0">✗</span>{{ item }}
                    </li>
                  </ul>
                </div>
              </div>
            </section>

            <!-- 项目分析 -->
            <section v-if="parsedReport.projectAnalysis">
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-orange-500 rounded-full"></span>项目分析
              </h3>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div class="bg-green-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-green-700 mb-2">亮点</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.projectAnalysis.advantages" :key="idx" class="text-xs text-green-600 leading-relaxed flex gap-1.5">
                      <span class="text-green-400 flex-shrink-0">✓</span>{{ item }}
                    </li>
                  </ul>
                </div>
                <div class="bg-red-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-red-700 mb-2">不足</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.projectAnalysis.disadvantages" :key="idx" class="text-xs text-red-600 leading-relaxed flex gap-1.5">
                      <span class="text-red-400 flex-shrink-0">✗</span>{{ item }}
                    </li>
                  </ul>
                </div>
              </div>
            </section>

            <!-- 差距分析 -->
            <section v-if="parsedReport.benchmarkGap">
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-yellow-500 rounded-full"></span>与同级别差距
                <span class="px-2 py-0.5 text-xs rounded-full bg-yellow-100 text-yellow-700">{{ parsedReport.benchmarkGap.gapDegree }}</span>
              </h3>
              <div class="space-y-2">
                <div v-for="(item, idx) in parsedReport.benchmarkGap.sameLevelGapPoints" :key="idx" class="bg-yellow-50 rounded-lg px-3 py-2">
                  <p class="text-xs text-yellow-700 leading-relaxed">{{ item }}</p>
                </div>
              </div>
            </section>

            <!-- 改进建议 -->
            <section v-if="parsedReport.improvementSuggestion">
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-primary rounded-full"></span>改进建议
              </h3>
              <div class="space-y-3">
                <div class="bg-red-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-red-700 mb-2">紧急优化项</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.improvementSuggestion.emergencyItems" :key="idx" class="text-xs text-red-600 leading-relaxed flex gap-1.5">
                      <span class="text-red-400 flex-shrink-0">!</span>{{ item }}
                    </li>
                  </ul>
                </div>
                <div class="bg-blue-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-blue-700 mb-2">长期提升项</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.improvementSuggestion.longTermItems" :key="idx" class="text-xs text-blue-600 leading-relaxed flex gap-1.5">
                      <span class="text-blue-400 flex-shrink-0">→</span>{{ item }}
                    </li>
                  </ul>
                </div>
              </div>
            </section>

            <!-- 核心优势与不足 -->
            <section v-if="parsedReport.overallSummary">
              <h3 class="text-sm font-bold text-dark flex items-center gap-2 mb-3">
                <span class="w-1.5 h-4 bg-gray-500 rounded-full"></span>综合总结
              </h3>
              <div class="space-y-3">
                <div class="bg-green-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-green-700 mb-1">核心竞争力</p>
                  <p class="text-xs text-green-600 leading-relaxed">{{ parsedReport.overallSummary.coreCompetence }}</p>
                </div>
                <div v-if="parsedReport.overallSummary.coreShortcomings?.length" class="bg-gray-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-gray-700 mb-2">核心短板</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.overallSummary.coreShortcomings" :key="idx" class="text-xs text-gray-600 leading-relaxed">• {{ item }}</li>
                  </ul>
                </div>
                <div v-if="parsedReport.overallSummary.adaptationSuggestions?.length" class="bg-blue-50 rounded-lg p-3">
                  <p class="text-xs font-medium text-blue-700 mb-2">求职建议</p>
                  <ul class="space-y-1">
                    <li v-for="(item, idx) in parsedReport.overallSummary.adaptationSuggestions" :key="idx" class="text-xs text-blue-600 leading-relaxed">• {{ item }}</li>
                  </ul>
                </div>
              </div>
            </section>
          </template>
          <div v-else class="text-center py-10 text-secondary text-sm">报告数据解析失败</div>
        </div>
        <!-- 弹窗底部 -->
        <div class="flex items-center justify-end px-6 py-4 border-t border-gray-100 flex-shrink-0">
          <button @click="showReportModal = false" class="px-5 py-2 bg-primary text-white text-sm font-medium rounded-lg hover:bg-primary-dark transition-colors">
            关闭
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { InterviewApiService, ResumeApiService } from '@/services/interview'
import { OrderApiService } from '@/services/product'
import { AccountApiService } from '@/services/account'
import type { InterviewInfo, ResumeInfo, OrderInfo } from '@/types'
import { InterviewStatus, OrderState } from '@/types'

// Store / Router
const userStore = useUserStore()
const router = useRouter()

// 菜单配置
const menuItems = [
  { key: 'interview', label: 'AI面试', icon: '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>' },
  { key: 'ai-chat', label: 'AI对话助手', icon: '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>' },
  { key: 'resume', label: '简历管理', icon: '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>' },
  { key: 'order', label: '我的订单', icon: '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>' },
  { key: 'pricing', label: '权益套餐', icon: '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7"/></svg>' },
  { key: 'settings', label: '账号设置', icon: '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>' },
]

// 响应式状态
const isPageLoading = ref(true)
const activeMenu = ref('interview')

// 面试相关
const isInterviewLoading = ref(false)
const interviewList = ref<InterviewInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(5)
const totalRecord = ref(0)
const totalPage = ref(0)

// 简历相关
const isResumeLoading = ref(false)
const resumeList = ref<ResumeInfo[]>([])
const resumeActionLoading = ref<Record<number, string | null>>({})

// 简历分析报告弹窗
const showReportModal = ref(false)
const reportResumeName = ref('')
const parsedReport = ref<any>(null)
const parsedSkillTags = ref<string[]>([])

// 创建面试弹窗
const showCreateInterviewModal = ref(false)
const interviewFormResumeName = ref('')
const interviewFormResumeId = ref<number>(0)
const isCreatingInterview = ref(false)
const interviewFormError = ref('')
const interviewForm = ref({
  targetPosition: '',
  expectedSalary: '',
  workYears: '',
  targetCity: '',
  interviewType: 'COMMON_INTERVIEW',
  specialContent: '',
})

// 订单相关
const isOrderLoading = ref(false)
const orderList = ref<OrderInfo[]>([])
const orderCurrentPage = ref(1)
const orderTotalPage = ref(0)
const orderTotalRecord = ref(0)

// 权益相关
const isBenefitLoading = ref(false)
const benefitList = ref<any[]>([])
const benefitCurrentPage = ref(1)
const benefitTotalPage = ref(0)
const benefitTotalRecord = ref(0)

// 计算属性
const avatarText = computed(() => {
  const name = userStore.username
  return name ? name.charAt(0) : '?'
})

const maskedPhone = computed(() => {
  const phone = userStore.userInfo?.phone
  if (!phone) return '未绑定手机号'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
})

const hasValidBenefit = computed(() => {
  return userStore.benefits.some(b => {
    const endTime = new Date(b.endTime).getTime()
    return endTime > Date.now()
  })
})

const displayPages = computed(() => {
  const pages: number[] = []
  const total = totalPage.value
  const current = currentPage.value
  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    const start = Math.max(1, current - 2)
    const end = Math.min(total, start + 4)
    for (let i = start; i <= end; i++) pages.push(i)
  }
  return pages
})

const orderDisplayPages = computed(() => {
  const pages: number[] = []
  const total = orderTotalPage.value
  const current = orderCurrentPage.value
  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    const start = Math.max(1, current - 2)
    const end = Math.min(total, start + 4)
    for (let i = start; i <= end; i++) pages.push(i)
  }
  return pages
})

// 方法
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

const statusClass = (status: InterviewStatus | null) => {
  switch (status) {
    case InterviewStatus.COMPLETED: return 'bg-green-100 text-green-700'
    case InterviewStatus.IN_PROGRESS:
    case InterviewStatus.EVALUATING: return 'bg-primary/10 text-primary'
    case InterviewStatus.GENERATING:
    case InterviewStatus.GENERATE_QA: return 'bg-blue-100 text-blue-700'
    case InterviewStatus.FAILED_PARSE_RESUME:
    case InterviewStatus.FAILED_CREATE_INTERVIEW:
    case InterviewStatus.FAILED_GENERATE_QA: return 'bg-red-100 text-red-700'
    default: return 'bg-gray-100 text-gray-600'
  }
}

const statusText = (status: InterviewStatus | null) => {
  switch (status) {
    case InterviewStatus.COMPLETED: return '已完成'
    case InterviewStatus.IN_PROGRESS: return '进行中'
    case InterviewStatus.EVALUATING: return '评估中'
    case InterviewStatus.GENERATING:
    case InterviewStatus.GENERATE_QA: return '生成中'
    case InterviewStatus.FAILED_PARSE_RESUME:
    case InterviewStatus.FAILED_CREATE_INTERVIEW:
    case InterviewStatus.FAILED_GENERATE_QA: return '生成失败'
    default: return '未知状态'
  }
}

const statusDescription = (item: InterviewInfo) => {
  switch (item.status) {
    case InterviewStatus.COMPLETED: return '面试已完成，查看评估报告'
    case InterviewStatus.IN_PROGRESS: return '面试生成完成，可以开始答题'
    case InterviewStatus.EVALUATING: return '面试结束，正在评估中'
    case InterviewStatus.GENERATING:
    case InterviewStatus.GENERATE_QA: return '正在生成面试题目...'
    case InterviewStatus.FAILED_PARSE_RESUME: return '简历解析失败，请重新上传'
    case InterviewStatus.FAILED_CREATE_INTERVIEW: return '面试创建超时，已退还权益'
    case InterviewStatus.FAILED_GENERATE_QA: return '生成面试题目失败'
    default: return '未知状态'
  }
}

const orderStateClass = (state: OrderState | string) => {
  switch (state) {
    case OrderState.PAY: return 'bg-green-100 text-green-700'
    case OrderState.NEW: return 'bg-yellow-100 text-yellow-700'
    case OrderState.CANCEL: return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

const orderStateText = (state: OrderState | string) => {
  switch (state) {
    case OrderState.PAY: return '已支付'
    case OrderState.NEW: return '待支付'
    case OrderState.CANCEL: return '已取消'
    default: return '未知'
  }
}

const benefitName = (code: string | null) => {
  switch (code) {
    case 'INTERVIEW_COUNT':
    case 'COMMON_INTERVIEW': return '通用面试次数'
    case 'SPECIAL_INTERVIEW': return '专项面试次数'
    case 'RESUME_ANALYSE': return '简历分析次数'
    case 'QUESTION_BANK': return '题库权益'
    default: return '权益'
  }
}


/** 菜单点击 - 每次点击都发起新请求 */
const handleMenuClick = (key: string) => {
  // AI对话助手跳转独立页面
  if (key === 'ai-chat') {
    router.push('/ai-chat')
    return
  }
  activeMenu.value = key
  loadPanelData(key)
}

/** 根据面板类型加载数据 */
const loadPanelData = (key: string) => {
  switch (key) {
    case 'interview':
      currentPage.value = 1
      fetchInterviews()
      break
    case 'resume':
      fetchResumes()
      break
    case 'order':
      orderCurrentPage.value = 1
      fetchOrders()
      break
    case 'pricing':
      benefitCurrentPage.value = 1
      fetchBenefits()
      break
  }
}

const goToPage = async (page: number) => {
  if (page < 1 || page > totalPage.value || page === currentPage.value) return
  currentPage.value = page
  await fetchInterviews()
}

const goToOrderPage = async (page: number) => {
  if (page < 1 || page > orderTotalPage.value || page === orderCurrentPage.value) return
  orderCurrentPage.value = page
  await fetchOrders()
}

const handleLogout = async () => {
  await userStore.logout()
  router.push('/')
}

/** 加载面试记录 */
const fetchInterviews = async () => {
  try {
    isInterviewLoading.value = true
    const response = await InterviewApiService.getList({ page: currentPage.value, size: pageSize.value })
    if (response.code === 0 && response.data) {
      // 兼容后端两种分页格式：currentData 或 records
      const data = response.data as any
      interviewList.value = data.currentData || data.records || []
      totalRecord.value = data.totalRecord || 0
      totalPage.value = data.totalPage || 0
    }
  } catch (error) {
    console.error('获取面试记录失败:', error)
  } finally {
    isInterviewLoading.value = false
  }
}

/** 加载简历列表 */
const fetchResumes = async () => {
  try {
    isResumeLoading.value = true
    const response = await ResumeApiService.getList()
    if (response.code === 0 && response.data) {
      resumeList.value = response.data
    }
  } catch (error) {
    console.error('获取简历列表失败:', error)
  } finally {
    isResumeLoading.value = false
  }
}

/** 预览简历 */
const handleResumePreview = async (resume: ResumeInfo) => {
  try {
    resumeActionLoading.value[resume.id] = 'preview'
    const response = await ResumeApiService.preview(resume.id)
    if (response.code === 0 && response.data) {
      // 打开新窗口预览简历内容（返回的是预签名URL或内容）
      window.open(response.data as string, '_blank')
    } else {
      alert(response.message || '预览失败，请稍后重试')
    }
  } catch (error) {
    console.error('预览简历失败:', error)
    alert('预览简历失败，请稍后重试')
  } finally {
    resumeActionLoading.value[resume.id] = null
  }
}

/** 查看简历分析报告 */
const handleViewReport = (resume: ResumeInfo) => {
  reportResumeName.value = resume.filename || '未命名简历'
  // 解析 evaluation JSON
  try {
    parsedReport.value = typeof resume.evaluation === 'string' ? JSON.parse(resume.evaluation) : resume.evaluation
  } catch {
    parsedReport.value = null
  }
  // 解析 skillTags JSON
  try {
    const tags = typeof resume.skillTags === 'string' ? JSON.parse(resume.skillTags) : resume.skillTags
    parsedSkillTags.value = Array.isArray(tags) ? tags : []
  } catch {
    parsedSkillTags.value = []
  }
  showReportModal.value = true
}

/** 打开创建面试弹窗 */
const openCreateInterviewModal = (resume: ResumeInfo) => {
  interviewFormResumeName.value = resume.filename || '未命名简历'
  interviewFormResumeId.value = resume.id
  interviewFormError.value = ''
  interviewForm.value = {
    targetPosition: '',
    expectedSalary: '',
    workYears: '',
    targetCity: '',
    interviewType: 'COMMON_INTERVIEW',
    specialContent: '',
  }
  showCreateInterviewModal.value = true
}

/** 提交创建面试 */
const handleCreateInterview = async () => {
  // 表单验证
  interviewFormError.value = ''
  const form = interviewForm.value
  if (!form.targetPosition.trim()) {
    interviewFormError.value = '请输入目标岗位'
    return
  }
  if (!form.expectedSalary.trim()) {
    interviewFormError.value = '请输入期望薪资'
    return
  }
  if (!form.workYears.trim()) {
    interviewFormError.value = '请输入工作年限'
    return
  }
  if (!form.targetCity.trim()) {
    interviewFormError.value = '请输入目标城市'
    return
  }

  try {
    isCreatingInterview.value = true
    const response = await InterviewApiService.create({
      expectedSalary: form.expectedSalary.trim(),
      workYears: form.workYears.trim(),
      targetCity: form.targetCity.trim(),
      targetPosition: form.targetPosition.trim(),
      interviewType: form.interviewType as any,
      specialContent: form.specialContent.trim() || undefined,
      resumeId: interviewFormResumeId.value,
    })
    if (response.code === 0) {
      showCreateInterviewModal.value = false
      // 切换到面试列表并刷新
      activeMenu.value = 'interview'
      currentPage.value = 1
      await fetchInterviews()
    } else {
      interviewFormError.value = response.message || '创建面试失败，请稍后重试'
    }
  } catch (error) {
    console.error('创建面试失败:', error)
    interviewFormError.value = '创建面试失败，请稍后重试'
  } finally {
    isCreatingInterview.value = false
  }
}

/** 简历状态样式 */
const resumeStatusClass = (status: string | null) => {
  switch (status) {
    case 'ANALYZED': return 'bg-green-100 text-green-700'
    case 'IN_PROCESS': return 'bg-blue-100 text-blue-700'
    case 'ERROR': return 'bg-red-100 text-red-700'
    case 'UPLOADED':
    default: return 'bg-gray-100 text-gray-600'
  }
}

/** 简历状态文本 */
const resumeStatusText = (status: string | null) => {
  switch (status) {
    case 'ANALYZED': return '已分析'
    case 'IN_PROCESS': return '分析中'
    case 'ERROR': return '解析失败'
    case 'UPLOADED': return '已上传'
    default: return '待分析'
  }
}

/** AI分析简历 */
const handleResumeAnalyse = async (resume: ResumeInfo) => {
  if (resume.status === 'ANALYZED' || resume.status === 'IN_PROCESS') return
  try {
    resumeActionLoading.value[resume.id] = 'analyse'
    const response = await ResumeApiService.analyse(resume.id)
    if (response.code === 0) {
      // 分析成功，刷新简历列表获取最新状态
      await fetchResumes()
    } else {
      alert(response.message || '分析失败，请稍后重试')
    }
  } catch (error) {
    console.error('简历分析失败:', error)
    alert('简历分析失败，请稍后重试')
  } finally {
    resumeActionLoading.value[resume.id] = null
  }
}

/** 删除简历 */
const handleResumeDelete = async (resume: ResumeInfo) => {
  if (!confirm(`确定要删除简历「${resume.filename || '未命名简历'}」吗？删除后不可恢复。`)) return
  try {
    resumeActionLoading.value[resume.id] = 'delete'
    const response = await ResumeApiService.delete(resume.id)
    if (response.code === 0) {
      // 删除成功，从列表中移除
      resumeList.value = resumeList.value.filter(r => r.id !== resume.id)
    } else {
      alert(response.message || '删除失败，请稍后重试')
    }
  } catch (error) {
    console.error('删除简历失败:', error)
    alert('删除简历失败，请稍后重试')
  } finally {
    resumeActionLoading.value[resume.id] = null
  }
}

/** 加载订单列表 */
const fetchOrders = async () => {
  try {
    isOrderLoading.value = true
    const response = await OrderApiService.page({ page: orderCurrentPage.value, size: 10 })
    if (response.code === 0 && response.data) {
      // 兼容后端两种分页格式：currentData 或 records
      const data = response.data as any
      orderList.value = data.currentData || data.records || []
      orderTotalPage.value = data.totalPage || 0
      orderTotalRecord.value = data.totalRecord || 0
    }
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    isOrderLoading.value = false
  }
}

// 不再使用 watch 缓存逻辑，菜单切换由 handleMenuClick 中的 loadPanelData 控制

/** 加载权益列表 */
const fetchBenefits = async () => {
  try {
    isBenefitLoading.value = true
    const response = await AccountApiService.getBenefitPage({ page: benefitCurrentPage.value, size: 6 })
    if (response.code === 0 && response.data) {
      const data = response.data as any
      benefitList.value = data.currentData || data.records || []
      benefitTotalPage.value = data.totalPage || 0
      benefitTotalRecord.value = data.totalRecord || 0
    }
  } catch (error) {
    console.error('获取权益列表失败:', error)
  } finally {
    isBenefitLoading.value = false
  }
}

const goToBenefitPage = async (page: number) => {
  if (page < 1 || page > benefitTotalPage.value || page === benefitCurrentPage.value) return
  benefitCurrentPage.value = page
  await fetchBenefits()
}

const benefitDisplayPages = computed(() => {
  const pages: number[] = []
  const total = benefitTotalPage.value
  const current = benefitCurrentPage.value
  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    const start = Math.max(1, current - 2)
    const end = Math.min(total, start + 4)
    for (let i = start; i <= end; i++) pages.push(i)
  }
  return pages
})

// 生命周期
onMounted(async () => {
  try {
    await Promise.all([
      userStore.fetchUserInfo(),
      fetchInterviews(),
    ])
  } catch (error) {
    console.error('加载个人中心数据失败:', error)
  } finally {
    isPageLoading.value = false
  }
})
</script>
