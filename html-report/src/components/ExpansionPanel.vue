<template>
  <div class="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-800">
    <button
      type="button"
      :aria-expanded="isOpen"
      @click="toggle"
      class="flex w-full cursor-pointer items-center gap-2 bg-gray-50 px-4 py-3 text-left text-sm font-medium text-gray-700 transition-colors select-none hover:bg-gray-100 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800"
    >
      <ChevronRight
        class="h-3.5 w-3.5 shrink-0 text-gray-400 transition-transform duration-200"
        :class="{ 'rotate-90': isOpen }"
        aria-hidden="true"
      />
      <slot name="title">
        {{ title }}
      </slot>
    </button>

    <CollapseTransition>
      <div v-show="isOpen">
        <div class="flex flex-col gap-2 bg-white px-4 py-3 dark:bg-gray-950">
          <slot />
        </div>
      </div>
    </CollapseTransition>
  </div>
</template>

<script setup lang="ts">
import { ChevronRight } from "@lucide/vue"
import { ref, watch } from "vue"
import CollapseTransition from "./CollapseTransition.vue"

const { title = "" } = defineProps<{ title?: string }>()
const model = defineModel<boolean>()

const isOpen = ref(model.value ?? false)

watch(model, (val) => {
  isOpen.value = val ?? false
})

function toggle() {
  isOpen.value = !isOpen.value
  model.value = isOpen.value
}
</script>
