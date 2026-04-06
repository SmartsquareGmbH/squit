<template>
  <div class="mx-auto max-w-5xl px-6 py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-gray-100">Test Results</h1>
      <p class="mt-1 text-gray-500 dark:text-gray-400">{{ summary }}</p>
    </div>

    <overview-stats class="mb-6" />

    <div class="mb-4 flex flex-wrap items-center gap-4">
      <div class="mr-auto flex flex-wrap items-center gap-4">
        <search-input v-model="searchQuery" />
      </div>

      <icon-button size="sm" title="Expand all" @click.stop="expand">
        <ChevronsUpDown class="h-3.5 w-3.5" aria-hidden="true" />
      </icon-button>
      <icon-button size="sm" title="Collapse all" @click.stop="collapse">
        <ChevronsDownUp class="h-3.5 w-3.5" aria-hidden="true" />
      </icon-button>
    </div>

    <div class="flex flex-col gap-2">
      <result-tree
        ref="children"
        v-for="(node, name) in data.results"
        :key="name"
        :name="name"
        :node="node"
        :search-query="searchQuery"
      />

      <div
        v-if="searchQuery && !hasSearchResults"
        class="flex flex-col items-center gap-2 py-12 text-gray-400 dark:text-gray-500"
      >
        <Search class="h-8 w-8" aria-hidden="true" />
        <p class="text-sm">
          No results for "<span class="font-medium text-gray-600 dark:text-gray-400">{{ searchQuery }}</span
          >"
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChevronsDownUp, ChevronsUpDown, Search } from "@lucide/vue"
import { computed, useTemplateRef } from "vue"
import IconButton from "../components/IconButton.vue"
import OverviewStats from "../components/OverviewStats.vue"
import ResultTree from "../components/ResultTree.vue"
import SearchInput from "../components/SearchInput.vue"
import { useSearch } from "../composables/useSearch.ts"
import { useSquitData } from "../composables/useSquitData.ts"
import { getResultNodeStats } from "../data.ts"

const data = useSquitData()
const { searchQuery, hasSearchResults } = useSearch(data.results)

const summary = computed(() => {
  const { success, failed } = getResultNodeStats(data.results)
  const total = success + failed

  if (total === 0) return "No tests run."
  if (failed === 0) return `${total} ${total === 1 ? "test" : "tests"} run. All passed!`
  if (total === 1) return `One test run. ${failed} failed.`
  return `${total} tests run. ${failed} failed.`
})

const childrenRefs = useTemplateRef("children")

function collapse() {
  for (const child of childrenRefs.value ?? []) {
    child?.collapse()
  }
}

function expand() {
  for (const child of childrenRefs.value ?? []) {
    child?.expand()
  }
}
</script>
