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
import OverviewCard from "./OverviewCard.vue"

const data = useSquitData()

function formatDateTime(dateTime?: string) {
  if (!dateTime) return ""

  const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "medium" })

  return dateFormat.format(new Date(dateTime))
}
function formatDuration(ms: number) {
  if (ms < 1000) return `${ms}ms`

  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}d ${hours % 24}h`
  if (hours > 0) return `${hours}h ${minutes % 60}m`
  if (minutes > 0) return `${minutes}m ${seconds % 60}s`
  return `${seconds}s`
}
</script>
