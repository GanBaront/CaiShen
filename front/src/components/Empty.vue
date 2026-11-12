<script setup lang="ts">
withDefaults(defineProps<{ title?: string; desc?: string }>(), { title: '暂无数据', desc: '' })
</script>

<template>
  <div class="empty">
    <div class="t">{{ title }}</div>
    <div v-if="desc" class="d">{{ desc }}</div>
    <slot />
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: var(--text3);
  font-size: 13px;
}
.empty .t {
  color: var(--text2);
  font-size: 14px;
}
.empty .d {
  font-size: 12px;
}
</style>
