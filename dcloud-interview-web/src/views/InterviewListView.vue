<template>
  <div class="pt-20 pb-12 px-4 sm:px-6 lg:px-8 min-h-screen">
    <div class="max-w-7xl mx-auto">
      <!-- Header -->
      <div class="flex items-center justify-between mb-8">
        <h1 class="text-2xl font-bold text-dark">我的面试</h1>
        <router-link to="/interview/create" class="btn-primary text-sm">
          创建面试
        </router-link>
      </div>

      <!-- Interview List -->
      <div class="space-y-4">
        <p class="text-center text-secondary py-12">暂无面试记录，点击右上角"创建面试"开始你的第一次面试吧！</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// InterviewListView - 面试列表页
</script>
