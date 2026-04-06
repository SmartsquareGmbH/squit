import { mount } from "@vue/test-utils"
import { describe, expect, test } from "vitest"
import type { SquitResult, SquitResultNode } from "../../src/data.ts"
import ResultTree from "../../src/components/ResultTree.vue"

const makeLeaf = (overrides: Partial<SquitResult> = {}): SquitResult => ({
  id: 1,
  alternativeName: "",
  success: true,
  ignored: false,
  error: false,
  duration: 250,
  expected: "ok",
  actual: "ok",
  ...overrides,
})

const globalStubs = {
  RouterLink: { template: '<a class="router-link"><slot /></a>' },
  ResultTree: { template: '<div class="result-tree-child" />' },
}

describe("ResultTree – leaf node", () => {
  test("renders a link for a leaf result", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "my-test", node: makeLeaf() },
      global: { stubs: globalStubs },
    })

    expect(wrapper.find(".router-link").exists()).toEqual(true)
    expect(wrapper.find(".result-tree-child").exists()).toEqual(false)
  })

  test("displays the node name when alternativeName is empty", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "my-test", node: makeLeaf({ alternativeName: "" }) },
      global: { stubs: globalStubs },
    })

    expect(wrapper.find(".router-link").text()).toContain("my-test")
  })

  test("displays alternativeName when set", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "my-test", node: makeLeaf({ alternativeName: "Get User" }) },
      global: { stubs: globalStubs },
    })

    expect(wrapper.find(".router-link").text()).toContain("Get User")
  })
})

describe("ResultTree – branch node", () => {
  const branch: SquitResultNode = {
    "child-a": makeLeaf({ id: 1, success: true }),
    "child-b": makeLeaf({ id: 2, success: false }),
  }

  test("renders an expansion panel for a branch node", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "suite", node: branch },
      global: { stubs: globalStubs },
    })

    expect(wrapper.find(".router-link").exists()).toEqual(false)
    expect(wrapper.findAll(".result-tree-child")).toHaveLength(2)
  })

  test("starts open when the branch has failures", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "suite", node: branch },
      global: { stubs: globalStubs },
    })

    expect(wrapper.find("button").attributes("aria-expanded")).toEqual("true")
  })

  test("starts closed when all tests pass", () => {
    const passingBranch: SquitResultNode = {
      a: makeLeaf({ id: 1, success: true }),
      b: makeLeaf({ id: 2, success: true }),
    }

    const wrapper = mount(ResultTree, {
      props: { name: "suite", node: passingBranch },
      global: { stubs: globalStubs },
    })

    expect(wrapper.find("button").attributes("aria-expanded")).toEqual("false")
  })
})

describe("ResultTree – search filtering", () => {
  const isHidden = (style: string | undefined) => style?.includes("display: none") ?? false

  test("leaf is visible when searchQuery is empty", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "my-test", node: makeLeaf(), searchQuery: "" },
      global: { stubs: globalStubs },
    })

    expect(isHidden(wrapper.find(".router-link").attributes("style"))).toEqual(false)
  })

  test("leaf is visible when its name matches the search query", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "create-order", node: makeLeaf({ alternativeName: "" }), searchQuery: "create" },
      global: { stubs: globalStubs },
    })

    expect(isHidden(wrapper.find(".router-link").attributes("style"))).toEqual(false)
  })

  test("leaf is hidden when its name does not match the search query", () => {
    const wrapper = mount(ResultTree, {
      props: { name: "create-order", node: makeLeaf({ alternativeName: "" }), searchQuery: "delete" },
      global: { stubs: globalStubs },
    })

    expect(isHidden(wrapper.find(".router-link").attributes("style"))).toEqual(true)
  })

  test("leaf matches by alternativeName (case-insensitive)", () => {
    const wrapper = mount(ResultTree, {
      props: {
        name: "t",
        node: makeLeaf({ alternativeName: "Get User By ID" }),
        searchQuery: "GET USER",
      },
      global: { stubs: globalStubs },
    })

    expect(isHidden(wrapper.find(".router-link").attributes("style"))).toEqual(false)
  })

  test("branch is visible when a child matches the search query", () => {
    const branch: SquitResultNode = {
      matching: makeLeaf({ id: 1, alternativeName: "matching leaf" }),
      other: makeLeaf({ id: 2, alternativeName: "something else" }),
    }

    const wrapper = mount(ResultTree, {
      props: { name: "suite", node: branch, searchQuery: "matching" },
      global: { stubs: globalStubs },
    })

    expect(isHidden(wrapper.find("button").element.closest("[style]")?.getAttribute("style") ?? "")).toEqual(false)
  })

  test("branch is hidden when no child matches the search query", () => {
    const branch: SquitResultNode = {
      a: makeLeaf({ id: 1, alternativeName: "alpha" }),
      b: makeLeaf({ id: 2, alternativeName: "beta" }),
    }

    const wrapper = mount(ResultTree, {
      props: { name: "suite", node: branch, searchQuery: "gamma" },
      global: { stubs: globalStubs },
    })

    expect(isHidden(wrapper.find("button").element.closest("[style]")?.getAttribute("style") ?? "")).toEqual(true)
  })
})
