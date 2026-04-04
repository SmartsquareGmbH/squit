<template>
  <div class="rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 dark:border-gray-800 dark:bg-gray-900">
    <p class="mb-1 text-xs font-semibold tracking-wide text-gray-400 uppercase dark:text-gray-500">{{ title }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
const { title } = defineProps<{ title: string }>()
</script>
