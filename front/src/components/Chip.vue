<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    variant?: 'default' | 'sel' | 'off' | 'add'
    closable?: boolean
  }>(),
  { variant: 'default', closable: false }
)
defineEmits<{ (e: 'click'): void; (e: 'close'): void }>()
</script>

<template>
  <span
    class="chip"
    :class="{ 'pill-sel': variant === 'sel', 'pill-off': variant === 'off', 'chip-add': variant === 'add' }"
    @click="$emit('click')"
  >
    <slot>{{ label }}</slot>
    <span v-if="closable" class="x" @click.stop="$emit('close')">×</span>
  </span>
</template>
