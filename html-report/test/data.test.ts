import { describe, expect, test } from "vitest"
import {
  findSquitResult,
  getResultNodeStats,
  isSquitResult,
  nodeMatchesFilter,
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

describe("nodeMatchesFilter", () => {
  test("returns true when query is empty and failed-only is off", () => {
    expect(nodeMatchesFilter(makeLeaf(), "name", "")).toEqual(true)
  })

  test("matches leaf by alternativeName (case-insensitive)", () => {
    const leaf = makeLeaf({ alternativeName: "Get User By ID" })

    expect(nodeMatchesFilter(leaf, "get-user", "get user")).toEqual(true)
    expect(nodeMatchesFilter(leaf, "get-user", "GET USER")).toEqual(true)
  })

  test("matches leaf by node name when alternativeName is empty", () => {
    const leaf = makeLeaf({ alternativeName: "" })

    expect(nodeMatchesFilter(leaf, "create-order", "create")).toEqual(true)
    expect(nodeMatchesFilter(leaf, "create-order", "delete")).toEqual(false)
  })

  test("returns true for a branch when any child matches", () => {
    const branch: SquitResultNode = {
      matching: makeLeaf({ id: 1, alternativeName: "matching leaf" }),
      other: makeLeaf({ id: 2, alternativeName: "something else" }),
    }

    expect(nodeMatchesFilter(branch, "branch", "matching")).toEqual(true)
  })

  test("returns false for a branch when no child matches", () => {
    const branch: SquitResultNode = {
      a: makeLeaf({ id: 1, alternativeName: "alpha" }),
      b: makeLeaf({ id: 2, alternativeName: "beta" }),
    }

    expect(nodeMatchesFilter(branch, "branch", "gamma")).toEqual(false)
  })

  test("hides a passing leaf when failed-only is on", () => {
    const leaf = makeLeaf({ success: true, ignored: false })

    expect(nodeMatchesFilter(leaf, "passing", "", true)).toEqual(false)
  })

  test("shows a failing leaf when failed-only is on", () => {
    const leaf = makeLeaf({ success: false, ignored: false })

    expect(nodeMatchesFilter(leaf, "failing", "", true)).toEqual(true)
  })

  test("hides an ignored leaf when failed-only is on", () => {
    const leaf = makeLeaf({ success: false, ignored: true })

    expect(nodeMatchesFilter(leaf, "ignored", "", true)).toEqual(false)
  })

  test("failed-only keeps a passing leaf matching the query hidden", () => {
    const leaf = makeLeaf({ alternativeName: "Get User", success: true })

    expect(nodeMatchesFilter(leaf, "get-user", "get", true)).toEqual(false)
  })

  test("failed-only keeps a failing leaf matching the query visible", () => {
    const leaf = makeLeaf({ alternativeName: "Get User", success: false })

    expect(nodeMatchesFilter(leaf, "get-user", "get", true)).toEqual(true)
  })

  test("returns false for a branch with only passing children when failed-only is on", () => {
    const branch: SquitResultNode = {
      a: makeLeaf({ id: 1, success: true }),
      b: makeLeaf({ id: 2, success: true }),
    }

    expect(nodeMatchesFilter(branch, "branch", "", true)).toEqual(false)
  })

  test("returns true for a branch with any failing child when failed-only is on", () => {
    const branch: SquitResultNode = {
      a: makeLeaf({ id: 1, success: true }),
      b: makeLeaf({ id: 2, success: false }),
    }

    expect(nodeMatchesFilter(branch, "branch", "", true)).toEqual(true)
  })

  test("failed-only requires the failing child to also match the query", () => {
    const branch: SquitResultNode = {
      failing: makeLeaf({ id: 1, alternativeName: "Create Order", success: false }),
      passing: makeLeaf({ id: 2, alternativeName: "Get User", success: true }),
    }

    expect(nodeMatchesFilter(branch, "branch", "get user", true)).toEqual(false)
    expect(nodeMatchesFilter(branch, "branch", "create", true)).toEqual(true)
  })

  test("returns true for a branch matching the query when failed-only is off", () => {
    const branch: SquitResultNode = {
      a: makeLeaf({ id: 1, alternativeName: "alpha" }),
      b: makeLeaf({ id: 2, alternativeName: "beta" }),
    }

    expect(nodeMatchesFilter(branch, "branch", "alpha", false)).toEqual(true)
  })
})
