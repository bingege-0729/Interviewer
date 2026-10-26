<template>
  <footer class="bg-dark text-gray-300">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
        <!-- Brand -->
        <div class="col-span-1 md:col-span-2">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
              <span class="text-white font-bold text-sm">AI</span>
            </div>
            <span class="text-lg font-bold text-white">DCloud AI面试</span>
          </div>
          <p class="text-sm text-gray-400 max-w-md">
            AI驱动的智能面试平台，基于简历自动生成定制化面试题目，提供实时评分和专业建议，助你轻松拿下心仪offer。
          </p>
        </div>

        <!-- Quick Links -->
        <div>
          <h3 class="text-sm font-semibold text-white uppercase tracking-wider mb-4">快速导航</h3>
          <ul class="space-y-2">
            <li>
              <router-link to="/" class="text-sm text-gray-400 hover:text-white transition-custom">首页</router-link>
            </li>
            <li>
              <router-link to="/interview" class="text-sm text-gray-400 hover:text-white transition-custom">我的面试</router-link>
            </li>
            <li>
              <router-link to="/pricing" class="text-sm text-gray-400 hover:text-white transition-custom">套餐价格</router-link>
            </li>
            <li>
              <router-link to="/resume" class="text-sm text-gray-400 hover:text-white transition-custom">简历管理</router-link>
            </li>
          </ul>
        </div>

        <!-- Contact -->
        <div>
          <h3 class="text-sm font-semibold text-white uppercase tracking-wider mb-4">联系我们</h3>
          <ul class="space-y-2">
            <li class="text-sm text-gray-400">微信：xdclass6</li>
            <li class="text-sm text-gray-400">官网：xdclass.net</li>
          </ul>
        </div>
      </div>

      <!-- Bottom -->
      <div class="mt-8 pt-8 border-t border-gray-700">
        <p class="text-sm text-gray-400 text-center">
          &copy; {{ currentYear }} DCloud AI面试平台. All rights reserved.
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>
