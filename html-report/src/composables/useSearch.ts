import { computed, ref } from "vue"
import { nodeMatchesSearch, type SquitResultNode } from "../data.ts"

export function useSearch(results: SquitResultNode) {
  const searchQuery = ref("")

  const hasSearchResults = computed(() =>
    Object.entries(results).some(([name, node]) => nodeMatchesSearch(node, name, searchQuery.value)),
  )

  return { searchQuery, hasSearchResults }
}
