<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'
import AppFooter from '@/components/AppFooter.vue'
</script>

<template>
  <div class="page">
    <NavBar />
    <main class="main">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>
