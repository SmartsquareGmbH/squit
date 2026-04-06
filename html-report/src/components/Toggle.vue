<template>
  <button
    role="switch"
    :aria-checked="model"
    :aria-label="ariaLabel"
    class="relative inline-flex h-5 w-9 cursor-pointer items-center rounded-full transition-colors focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:outline-none"
    :class="model ? 'bg-indigo-600' : 'bg-gray-300 dark:bg-gray-600'"
    @click="model = !model"
  >
    <span
      class="inline-block h-3.5 w-3.5 transform rounded-full bg-white shadow-sm transition-transform"
      :class="model ? 'translate-x-4' : 'translate-x-0.75'"
    />
  </button>
</template>

<script setup lang="ts">
const model = defineModel<boolean>()

defineProps<{ ariaLabel?: string }>()
</script>
