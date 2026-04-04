<template>
  <span
    class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap"
    :class="variantClass"
  >
    <slot />
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue"

const props = defineProps<{ variant: "success" | "failure" | "ignored" }>()

const variantClass = computed(() => {
  if (props.variant === "ignored") return "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400"
  if (props.variant === "success") return "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400"
  return "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400"
})
</script>
