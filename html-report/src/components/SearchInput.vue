<template>
  <div class="relative">
    <Search
      class="pointer-events-none absolute top-1/2 left-2.5 h-3.5 w-3.5 -translate-y-1/2 text-gray-400"
      aria-hidden="true"
    />
    <input
      v-model="model"
      type="search"
      placeholder="Search…"
      class="rounded-lg border border-gray-300 bg-white py-1.5 pr-3 pl-8 text-sm text-gray-900 placeholder-gray-400 focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400 focus:outline-none dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 dark:focus:border-indigo-500"
    />
  </div>
</template>

<script setup lang="ts">
import { Search } from "@lucide/vue"

const model = defineModel<string>({ required: true })
</script>
