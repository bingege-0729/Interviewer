<template>
  <div id="app" class="font-inter">
    <NavBar v-if="showNavbar" />
    <main>
      <router-view />
    </main>
    <FooterBar v-if="showFooter" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from '@/components/common/NavBar.vue'
import FooterBar from '@/components/common/FooterBar.vue'

const route = useRoute()

const showNavbar = computed(() => route.meta.showNavbar !== false)
const showFooter = computed(() => route.meta.showFooter !== false)
</script>
