import { computed, ref } from "vue"
import { nodeMatchesFilter, type SquitResultNode } from "../data.ts"

export function useFilters(results: SquitResultNode) {
  const searchQuery = ref("")
  const failedOnly = ref(false)

  const hasSearchResults = computed(() =>
    Object.entries(results).some(([name, node]) => nodeMatchesFilter(node, name, searchQuery.value, failedOnly.value)),
  )

  return { searchQuery, failedOnly: failedOnly, hasSearchResults }
}
