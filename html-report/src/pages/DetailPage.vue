<template>
  <div v-if="result" class="mx-auto max-w-5xl px-6 py-8">
    <div class="mb-6 flex flex-wrap items-center gap-3">
      <router-link
        to="/"
        class="cursor-pointer rounded p-0.5 transition-colors hover:bg-gray-200 dark:hover:bg-gray-700"
      >
        <ArrowLeft class="h-5 w-5" aria-hidden="true" />
      </router-link>

      <div class="flex flex-col">
        <h1 class="text-xl font-semibold wrap-break-word text-gray-900 dark:text-gray-100">{{ displayName }}</h1>
        <h2 v-if="result.alternativeName" class="text-base text-gray-500 dark:text-gray-400">({{ result.name }})</h2>
      </div>

      <status-badge :variant="resultVariant">{{ badgeText }}</status-badge>
    </div>

    <expansion-panel v-if="result.description" title="Description" class="mb-4">
      <markdown-block :content="result.description" />
    </expansion-panel>

    <template v-if="result.infoExpected && result.infoActual">
      <diff-viewer
        :expected="result.infoExpected"
        :actual="result.infoActual"
        language="json"
        class="mb-4"
      />
    </template>

    <diff-viewer :expected="result.expected" :actual="result.actual" :language="result.language" />
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from "@lucide/vue"
import { computed } from "vue"
import { useRoute } from "vue-router"
import DiffViewer from "../components/DiffViewer.vue"
import ExpansionPanel from "../components/ExpansionPanel.vue"
import MarkdownBlock from "../components/MarkdownBlock.vue"
import StatusBadge from "../components/StatusBadge.vue"
import { useSquitResult } from "../composables/useSquitData.ts"

const route = useRoute()
const result = useSquitResult(Number(route.params.id))

const displayName = computed(() => result?.alternativeName || result?.name || "")

const resultVariant = computed((): "success" | "failure" | "ignored" => {
  if (!result || result.ignored) return "ignored"
  return result.success ? "success" : "failure"
})

const badgeText = computed(() => {
  if (!result) return ""
  if (result.ignored) return "Ignored"
  return result.success ? "Passed" : "Failed"
})
</script>
