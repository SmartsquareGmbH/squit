<template>
  <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
    <overview-card title="Started at">
      <p class="text-sm font-medium text-gray-900 dark:text-gray-100">{{ formatDateTime(data.startedAt) }}</p>
    </overview-card>
    <overview-card title="Total duration">
      <p class="text-sm font-medium text-gray-900 dark:text-gray-100">{{ formatDuration(data.totalDuration) }}</p>
    </overview-card>
    <overview-card title="Average duration">
      <p class="text-sm font-medium text-gray-900 dark:text-gray-100">{{ formatDuration(data.averageDuration) }}</p>
    </overview-card>
    <overview-card v-if="data.slowestTest" title="Slowest test">
      <router-link
        :to="`/detail/${data.slowestTest.id}`"
        class="text-sm font-medium text-indigo-600 hover:underline dark:text-indigo-400"
      >
        <code class="font-mono text-xs">{{ data.slowestTest.name }}</code>
        <span class="text-gray-500 dark:text-gray-400"> ({{ formatDuration(data.slowestTest.duration) }})</span>
      </router-link>
    </overview-card>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from "vue-router"
import { useSquitData } from "../composables/useSquitData.ts"
import { formatDuration } from "../utils.ts"
import OverviewCard from "./OverviewCard.vue"

const data = useSquitData()

function formatDateTime(dateTime?: string) {
  if (!dateTime) return ""

  const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "medium" })

  return dateFormat.format(new Date(dateTime))
}
</script>
