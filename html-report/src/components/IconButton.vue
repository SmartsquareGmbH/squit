<template>
  <button :title="title" class="cursor-pointer text-gray-400 transition-colors" :class="sizeClass">
    <slot />
    <span class="sr-only">{{ title }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue"

const props = defineProps<{ title: string; size?: "sm" | "md" }>()

const sizeClass = computed(() =>
  props.size === "sm"
    ? "rounded p-0.5 hover:bg-gray-200 hover:text-gray-700 dark:hover:bg-gray-700 dark:hover:text-gray-200"
    : "rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-100",
)
</script>
