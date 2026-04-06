import { describe, expect, test } from "vitest"
import { formatDuration } from "../src/utils.ts"

describe("formatDuration", () => {
  test("returns milliseconds for values under 1000ms", () => {
    expect(formatDuration(0)).toEqual("0ms")
    expect(formatDuration(500)).toEqual("500ms")
    expect(formatDuration(999)).toEqual("999ms")
  })

  test("returns seconds for values between 1000ms and 60s", () => {
    expect(formatDuration(1000)).toEqual("1s")
    expect(formatDuration(30000)).toEqual("30s")
    expect(formatDuration(59999)).toEqual("59s")
  })

  test("returns minutes and seconds for values between 1m and 1h", () => {
    expect(formatDuration(60000)).toEqual("1m 0s")
    expect(formatDuration(90000)).toEqual("1m 30s")
    expect(formatDuration(3599000)).toEqual("59m 59s")
  })

  test("returns hours and minutes for values between 1h and 24h", () => {
    expect(formatDuration(3600000)).toEqual("1h 0m")
    expect(formatDuration(5400000)).toEqual("1h 30m")
    expect(formatDuration(86399000)).toEqual("23h 59m")
  })

  test("returns days and hours for values of 24h or more", () => {
    expect(formatDuration(86400000)).toEqual("1d 0h")
    expect(formatDuration(90000000)).toEqual("1d 1h")
    expect(formatDuration(172800000)).toEqual("2d 0h")
  })
})
