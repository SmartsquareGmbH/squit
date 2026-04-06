import { mount } from "@vue/test-utils"
import { describe, expect, test } from "vitest"
import ExpansionPanel from "../../src/components/ExpansionPanel.vue"

describe("ExpansionPanel", () => {
  test("is collapsed by default", () => {
    const wrapper = mount(ExpansionPanel, { props: { title: "Details" } })
    expect(wrapper.find("button").attributes("aria-expanded")).toEqual("false")
  })

  test("toggles open when the button is clicked", async () => {
    const wrapper = mount(ExpansionPanel, { props: { title: "Details" } })
    await wrapper.find("button").trigger("click")
    expect(wrapper.find("button").attributes("aria-expanded")).toEqual("true")
  })

  test("toggles closed again on a second click", async () => {
    const wrapper = mount(ExpansionPanel, { props: { title: "Details" } })
    await wrapper.find("button").trigger("click")
    await wrapper.find("button").trigger("click")
    expect(wrapper.find("button").attributes("aria-expanded")).toEqual("false")
  })

  test("starts open when modelValue is true", () => {
    const wrapper = mount(ExpansionPanel, {
      props: { title: "Details", modelValue: true },
    })

    expect(wrapper.find("button").attributes("aria-expanded")).toEqual("true")
  })

  test("renders slot content", async () => {
    const wrapper = mount(ExpansionPanel, {
      props: { title: "Details", modelValue: true },
      slots: { default: "<p class='body'>Content here</p>" },
    })

    expect(wrapper.find(".body").text()).toEqual("Content here")
  })

  test("renders custom title slot", () => {
    const wrapper = mount(ExpansionPanel, {
      props: { title: "Fallback" },
      slots: { title: "<span class='custom-title'>Custom</span>" },
    })

    expect(wrapper.find(".custom-title").text()).toEqual("Custom")
  })
})
