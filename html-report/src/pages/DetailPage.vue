<template>
  <div v-if="result" class="mx-auto max-w-5xl px-6 py-8">
    <div class="mb-6 flex flex-wrap items-center gap-3">
      <router-link
        to="/"
        class="cursor-pointer rounded p-0.5 transition-colors hover:bg-gray-200 dark:hover:bg-gray-700"
      >
        <ArrowLeft class="h-5 w-5" aria-hidden="true" />
      </router-link>

      <div class="flex min-w-0 flex-1 flex-col">
        <div
          v-if="result.path.length > 0"
          class="mb-0.5 flex flex-wrap items-center gap-1 text-xs text-gray-400 dark:text-gray-500"
        >
          <span v-for="(segment, i) in result.path" :key="i" class="flex items-center gap-1">
            <span>{{ segment }}</span>
            <ChevronRight v-if="i < result.path.length - 1" class="h-3 w-3" aria-hidden="true" />
          </span>
        </div>
        <h1 class="text-xl font-semibold wrap-break-word text-gray-900 dark:text-gray-100">{{ displayName }}</h1>
        <h2 v-if="result.alternativeName" class="text-base text-gray-500 dark:text-gray-400">({{ result.name }})</h2>
      </div>

      <div class="flex shrink-0 items-center gap-2">
        <span class="text-xs text-gray-400 dark:text-gray-500">{{ formatDuration(result.duration) }}</span>
        <status-badge :variant="resultVariant">{{ badgeText }}</status-badge>
      </div>
    </div>

    <expansion-panel v-if="result.description" title="Description" class="mb-4">
      <markdown-block :content="result.description" />
    </expansion-panel>

    <template v-if="result.infoExpected && result.infoActual">
      <diff-viewer :expected="result.infoExpected" :actual="result.infoActual" language="json" class="mb-4" />
    </template>

    <diff-viewer :expected="result.expected" :actual="result.actual" :language="result.language" />
  </div>

  <div v-else class="mx-auto flex max-w-5xl flex-col items-center gap-4 px-6 py-16 text-center">
    <p class="text-lg font-semibold text-gray-700 dark:text-gray-300">Test not found</p>
    <p class="text-sm text-gray-400 dark:text-gray-500">No test result exists for this ID.</p>
    <router-link to="/" class="text-sm font-medium text-indigo-600 hover:underline dark:text-indigo-400">
      Back to results
    </router-link>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, ChevronRight } from "@lucide/vue"
import { computed } from "vue"
import { useRoute } from "vue-router"
import DiffViewer from "../components/DiffViewer.vue"
import ExpansionPanel from "../components/ExpansionPanel.vue"
import MarkdownBlock from "../components/MarkdownBlock.vue"
import StatusBadge from "../components/StatusBadge.vue"
import { useSquitResult } from "../composables/useSquitData.ts"
import { formatDuration } from "../utils.ts"

const route = useRoute()
const result = useSquitResult(Number(route.params.id))

const displayName = computed(() => result?.alternativeName || result?.name || "")

const resultVariant = computed((): "success" | "failure" | "ignored" | "error" => {
  if (!result || result.ignored) return "ignored"
  if (result.error) return "error"
  return result.success ? "success" : "failure"
})

const badgeText = computed(() => {
  if (!result) return ""
  if (result.ignored) return "Ignored"
  if (result.error) return "Error"
  return result.success ? "Passed" : "Failed"
})
</script>
