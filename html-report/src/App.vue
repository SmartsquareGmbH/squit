<template>
  <div class="min-h-screen bg-white text-gray-900 transition-colors dark:bg-gray-950 dark:text-gray-100">
    <header
      class="sticky top-0 z-50 border-b border-gray-200 bg-white/90 backdrop-blur dark:border-gray-800 dark:bg-gray-950/90"
    >
      <div class="mx-auto flex h-12 max-w-5xl items-center gap-2.5 px-6">
        <router-link to="/" class="flex items-center gap-2">
          <span class="text-base font-bold tracking-tight">Squit</span>
          <span
            class="rounded bg-indigo-100 px-1.5 py-0.5 text-[10px] font-semibold tracking-widest text-indigo-600 uppercase dark:bg-indigo-900/40 dark:text-indigo-400"
          >
            report
          </span>
        </router-link>

        <div class="ml-auto flex items-center gap-2">
          <Sun class="h-3.5 w-3.5 text-gray-400 dark:text-gray-600" aria-hidden="true" />
          <toggle v-model="isDark" :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'" />
          <Moon class="h-3.5 w-3.5 text-gray-400 dark:text-gray-400" aria-hidden="true" />
        </div>
      </div>
    </header>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { Moon, Sun } from "@lucide/vue"
import { RouterView } from "vue-router"
import Toggle from "./components/Toggle.vue"
import { useDarkMode } from "./composables/useDarkMode.ts"

const { isDark } = useDarkMode()
</script>
