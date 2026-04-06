import { describe, expect, test } from "vitest"
import { useSearch } from "../../src/composables/useSearch.ts"
import { type SquitResult, type SquitResultNode } from "../../src/data.ts"

const makeLeaf = (overrides: Partial<SquitResult> = {}): SquitResult => ({
  id: 1,
  alternativeName: "My Test",
  success: true,
  ignored: false,
  error: false,
  duration: 100,
  expected: "ok",
  actual: "ok",
  ...overrides,
})

const results: SquitResultNode = {
  "get-user": makeLeaf({ id: 1, alternativeName: "Get User" }),
  "create-user": makeLeaf({ id: 2, alternativeName: "Create User" }),
  "health-check": makeLeaf({ id: 3, alternativeName: "" }),
}

describe("useSearch", () => {
  test("starts with an empty search query", () => {
    const { searchQuery } = useSearch(results)
    expect(searchQuery.value).toEqual("")
  })

  test("hasSearchResults is true when query is empty", () => {
    const { hasSearchResults } = useSearch(results)
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is true when query matches a result by alternativeName", () => {
    const { searchQuery, hasSearchResults } = useSearch(results)
    searchQuery.value = "get user"
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is true for case-insensitive match", () => {
    const { searchQuery, hasSearchResults } = useSearch(results)
    searchQuery.value = "GET USER"
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is true when query matches a result by node name", () => {
    const { searchQuery, hasSearchResults } = useSearch(results)
    searchQuery.value = "health"
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is false when nothing matches the query", () => {
    const { searchQuery, hasSearchResults } = useSearch(results)
    searchQuery.value = "nonexistent-xyz"
    expect(hasSearchResults.value).toEqual(false)
  })
})
