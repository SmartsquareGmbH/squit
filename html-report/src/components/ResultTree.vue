<template>
  <RouterLink
    v-if="isSquitResult(node)"
    v-show="!failedOnly || !node.success"
    :to="`/detail/${node.id}`"
    class="flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-3.5 py-2.5 text-sm text-gray-800 transition-colors hover:border-gray-300 hover:bg-gray-50 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-200 dark:hover:border-gray-700 dark:hover:bg-gray-800"
  >
    <span class="min-w-0 flex-1 truncate">{{ node.alternativeName || name }}</span>
    <status-badge :variant="leafVariant(node)">{{ leafBadgeText(node) }}</status-badge>
  </RouterLink>

  <expansion-panel v-else v-show="!failedOnly || stats.failed > 0" v-model="isOpen">
    <template #title>
      <span class="min-w-0 flex-1 truncate text-sm font-medium text-gray-800 dark:text-gray-200">
        {{ node.alternativeName || name}}
      </span>
      <icon-button size="sm" title="Expand all" @click.stop="expand">
        <ChevronsUpDown class="h-3.5 w-3.5" aria-hidden="true" />
      </icon-button>
      <icon-button size="sm" title="Collapse all" @click.stop="collapse">
        <ChevronsDownUp class="h-3.5 w-3.5" aria-hidden="true" />
      </icon-button>
      <status-badge :variant="stats.failed === 0 ? 'success' : 'failure'">
        {{ stats.success }}/{{ stats.success + stats.failed }} passed
      </status-badge>
    </template>

    <ResultTree
      v-for="(child, childName) in node"
      :key="childName"
      :name="childName"
      :node="child"
      :failed-only="failedOnly"
      ref="children"
    />
  </expansion-panel>
</template>

<script setup lang="ts">
import { ChevronsUpDown, ChevronsDownUp } from "@lucide/vue"
import { ref, useTemplateRef } from "vue"
import { RouterLink } from "vue-router"
import { getResultNodeStats, isSquitResult, type SquitResult, type SquitResultNode } from "../data.ts"
import ExpansionPanel from "./ExpansionPanel.vue"
import IconButton from "./IconButton.vue"
import ResultTree from "./ResultTree.vue"
import StatusBadge from "./StatusBadge.vue"

const props = defineProps<{
  name: string
  node: SquitResultNode | SquitResult
  failedOnly: boolean
}>()

const stats = getResultNodeStats(props.node)
const isOpen = ref(stats.failed > 0)

const childrenRefs = useTemplateRef("children")

function leafVariant(leaf: SquitResult): "success" | "failure" | "ignored" {
  if (leaf.ignored) return "ignored"
  return leaf.success ? "success" : "failure"
}

function leafBadgeText(leaf: SquitResult) {
  if (leaf.ignored) return "Ignored"
  return leaf.success ? "Passed" : "Failed"
}

function expand() {
  isOpen.value = true

  for (const child of childrenRefs.value ?? []) {
    child?.expand()
  }
}

function collapse() {
  isOpen.value = false

  for (const child of childrenRefs.value ?? []) {
    child?.collapse()
  }
}

defineExpose({ expand, collapse })
</script>
