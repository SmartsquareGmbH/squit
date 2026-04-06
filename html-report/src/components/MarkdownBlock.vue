<template>
  <div class="prose prose-sm dark:prose-invert max-w-none" v-html="html" />
</template>

<script setup lang="ts">
import { computedAsync } from "@vueuse/core"
import DOMPurify from "dompurify"
import { marked } from "marked"

const props = defineProps<{ content: string }>()

const html = computedAsync(async () => DOMPurify.sanitize(await marked.parse(props.content)), "")
</script>
