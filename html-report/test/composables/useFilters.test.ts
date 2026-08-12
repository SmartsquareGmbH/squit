import { describe, expect, test } from "vitest"
import { useFilters } from "../../src/composables/useFilters.ts"
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
  "create-user": makeLeaf({ id: 2, alternativeName: "Create User", success: false }),
  "health-check": makeLeaf({ id: 3, alternativeName: "" }),
}

describe("useFilters", () => {
  test("starts with an empty search query", () => {
    const { searchQuery } = useFilters(results)
    expect(searchQuery.value).toEqual("")
  })

  test("starts with failed-only filter disabled", () => {
    const { failedOnly } = useFilters(results)
    expect(failedOnly.value).toEqual(false)
  })

  test("hasSearchResults is true when query is empty", () => {
    const { hasSearchResults } = useFilters(results)
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is true when query matches a result by alternativeName", () => {
    const { searchQuery, hasSearchResults } = useFilters(results)
    searchQuery.value = "get user"
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is true for case-insensitive match", () => {
    const { searchQuery, hasSearchResults } = useFilters(results)
    searchQuery.value = "GET USER"
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is true when query matches a result by node name", () => {
    const { searchQuery, hasSearchResults } = useFilters(results)
    searchQuery.value = "health"
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is false when nothing matches the query", () => {
    const { searchQuery, hasSearchResults } = useFilters(results)
    searchQuery.value = "nonexistent-xyz"
    expect(hasSearchResults.value).toEqual(false)
  })

  test("hasSearchResults is true when failed-only filter is on and a result failed", () => {
    const { failedOnly, hasSearchResults } = useFilters(results)
    failedOnly.value = true
    expect(hasSearchResults.value).toEqual(true)
  })

  test("hasSearchResults is false when failed-only filter is on and no result failed", () => {
    const passing: SquitResultNode = {
      "get-user": makeLeaf({ id: 1, alternativeName: "Get User" }),
      "health-check": makeLeaf({ id: 3, alternativeName: "" }),
    }

    const { failedOnly, hasSearchResults } = useFilters(passing)
    failedOnly.value = true
    expect(hasSearchResults.value).toEqual(false)
  })

  test("combines search query and failed-only filter", () => {
    const { searchQuery, failedOnly, hasSearchResults } = useFilters(results)
    failedOnly.value = true
    searchQuery.value = "create"
    expect(hasSearchResults.value).toEqual(true)
  })

  test("combines search query and failed-only filter excluding passing matches", () => {
    const { searchQuery, failedOnly, hasSearchResults } = useFilters(results)
    failedOnly.value = true
    searchQuery.value = "get user"
    expect(hasSearchResults.value).toEqual(false)
  })
})
