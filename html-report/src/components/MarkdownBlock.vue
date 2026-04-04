<template>
  <div class="prose prose-sm dark:prose-invert max-w-none" v-html="html" />
</template>

<script setup lang="ts">
import { computedAsync } from "@vueuse/core"
import { marked } from "marked"

const props = defineProps<{ content: string }>()

const html = computedAsync(() => marked.parse(props.content), "")
</script>
