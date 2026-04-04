<template>
  <div class="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-800">
    <div
      v-if="hasChanges"
      class="flex items-center justify-end gap-2 border-b border-gray-200 px-3 py-2 dark:border-gray-800"
    >
      <btn @click="toggleDiffStyle">
        {{ diffStyle === "word" ? "Char diff" : "Word diff" }}
      </btn>
      <btn @click="toggleOutputFormat">
        {{ outputFormat === "side-by-side" ? "Inline" : "Side by side" }}
      </btn>
    </div>
    <div :class="{ 'no-changes': !hasChanges }">
      <code-diff
        :old-string="expected"
        :new-string="actual"
        :output-format="effectiveOutputFormat"
        :diff-style="diffStyle"
        :theme="isDark ? 'dark' : 'light'"
        :language="language ?? 'plaintext'"
        hide-header
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import { CodeDiff } from "v-code-diff"
import { useDarkMode } from "../composables/useDarkMode.ts"
import Btn from "./Btn.vue"

const props = defineProps<{
  expected: string
  actual: string
  language?: string
}>()

const { isDark } = useDarkMode()

const hasChanges = computed(() => props.expected !== props.actual)

const outputFormat = ref<"side-by-side" | "line-by-line">("side-by-side")
const diffStyle = ref<"word" | "char">("word")

function toggleOutputFormat() {
  outputFormat.value = outputFormat.value === "side-by-side" ? "line-by-line" : "side-by-side"
}

function toggleDiffStyle() {
  diffStyle.value = diffStyle.value === "word" ? "char" : "word"
}

// When there are no changes, always use line-by-line to show the full file
const effectiveOutputFormat = computed(() => (hasChanges.value ? outputFormat.value : "line-by-line"))
</script>

<style scoped>
:deep(.code-diff-view) {
  margin: 0;
  border: none;
  border-radius: 0;
  overflow: visible;
  font-family: ui-monospace, "Cascadia Code", "Source Code Pro", Menlo, Consolas, monospace;
}

div :deep(.code-diff-view[theme="dark"]) {
  --bgColor-default: #111827;
  --color-canvas-default: #111827;
  --color-canvas-overlay: #111827;
  --color-canvas-inset: #1f2937;
  --color-canvas-subtle: #1f2937;
  --color-border-default: #374151;
  --color-codemirror-bg: #111827;
  --color-codemirror-gutters-bg: #1f2937;
  --color-codemirror-lines-bg: #111827;
}

div :deep(.code-diff-view[theme="light"]) {
  --color-canvas-subtle: #f9fafb;
  --color-canvas-inset: #f9fafb;
  --color-border-default: #e5e7eb;
  --color-codemirror-gutters-bg: #f9fafb;
}

/* When there are no changes, hide the redundant "new" line number column */
.no-changes :deep(td.blob-num:nth-child(2)) {
  display: none;
}
</style>
