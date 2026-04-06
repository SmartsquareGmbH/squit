import { describe, expect, test } from "vitest"
import {
  findSquitResult,
  getResultNodeStats,
  isSquitResult,
  nodeMatchesSearch,
  type SquitResult,
  type SquitResultNode,
} from "../src/data.ts"

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

describe("isSquitResult", () => {
  test("returns true for a leaf result node", () => {
    expect(isSquitResult(makeLeaf())).toEqual(true)
  })

  test("returns false for a branch node", () => {
    const branch: SquitResultNode = { child: makeLeaf() }
    expect(isSquitResult(branch)).toEqual(false)
  })

  test("returns false for an empty object", () => {
    expect(isSquitResult({})).toEqual(false)
  })
})

describe("getResultNodeStats", () => {
  test("counts a passing leaf as success", () => {
    expect(getResultNodeStats(makeLeaf({ success: true, ignored: false }))).toEqual({
      success: 1,
      failed: 0,
      ignored: 0,
    })
  })

  test("counts a failing leaf as failed", () => {
    expect(getResultNodeStats(makeLeaf({ success: false, ignored: false }))).toEqual({
      success: 0,
      failed: 1,
      ignored: 0,
    })
  })

  test("counts an ignored leaf as ignored regardless of success", () => {
    expect(getResultNodeStats(makeLeaf({ ignored: true, success: false }))).toEqual({
      success: 0,
      failed: 0,
      ignored: 1,
    })
  })

  test("aggregates stats from all children in a branch", () => {
    const branch: SquitResultNode = {
      a: makeLeaf({ id: 1, success: true, ignored: false }),
      b: makeLeaf({ id: 2, success: false, ignored: false }),
      c: makeLeaf({ id: 3, ignored: true, success: false }),
    }

    expect(getResultNodeStats(branch)).toEqual({ success: 1, failed: 1, ignored: 1 })
  })

  test("aggregates stats recursively through nested branchs", () => {
    const tree: SquitResultNode = {
      branch: {
        sub: makeLeaf({ id: 1, success: true }),
      },
      leaf: makeLeaf({ id: 2, success: false }),
    }

    expect(getResultNodeStats(tree)).toEqual({ success: 1, failed: 1, ignored: 0 })
  })
})

describe("findSquitResult", () => {
  test("finds a direct leaf by id", () => {
    const tree: SquitResultNode = { "my-test": makeLeaf({ id: 42 }) }
    const result = findSquitResult(tree, 42)

    expect(result?.id).toEqual(42)
    expect(result?.name).toEqual("my-test")
    expect(result?.path).toEqual([])
  })

  test("finds a nested leaf and returns the correct path", () => {
    const tree: SquitResultNode = {
      api: {
        users: makeLeaf({ id: 7 }),
      },
    }

    const result = findSquitResult(tree, 7)

    expect(result?.name).toEqual("users")
    expect(result?.path).toEqual(["api"])
  })

  test("returns undefined when id does not exist", () => {
    const tree: SquitResultNode = { "my-test": makeLeaf({ id: 1 }) }

    expect(findSquitResult(tree, 999)).toBeUndefined()
  })

  test("returns undefined for an empty tree", () => {
    expect(findSquitResult({}, 1)).toBeUndefined()
  })
})

describe("nodeMatchesSearch", () => {
  test("returns true when query is empty", () => {
    expect(nodeMatchesSearch(makeLeaf(), "name", "")).toEqual(true)
  })

  test("matches leaf by alternativeName (case-insensitive)", () => {
    const leaf = makeLeaf({ alternativeName: "Get User By ID" })

    expect(nodeMatchesSearch(leaf, "get-user", "get user")).toEqual(true)
    expect(nodeMatchesSearch(leaf, "get-user", "GET USER")).toEqual(true)
  })

  test("matches leaf by node name when alternativeName is empty", () => {
    const leaf = makeLeaf({ alternativeName: "" })

    expect(nodeMatchesSearch(leaf, "create-order", "create")).toEqual(true)
    expect(nodeMatchesSearch(leaf, "create-order", "delete")).toEqual(false)
  })

  test("returns true for a branch when any child matches", () => {
    const branch: SquitResultNode = {
      matching: makeLeaf({ id: 1, alternativeName: "matching leaf" }),
      other: makeLeaf({ id: 2, alternativeName: "something else" }),
    }

    expect(nodeMatchesSearch(branch, "branch", "matching")).toEqual(true)
  })

  test("returns false for a branch when no child matches", () => {
    const branch: SquitResultNode = {
      a: makeLeaf({ id: 1, alternativeName: "alpha" }),
      b: makeLeaf({ id: 2, alternativeName: "beta" }),
    }

    expect(nodeMatchesSearch(branch, "branch", "gamma")).toEqual(false)
  })
})
