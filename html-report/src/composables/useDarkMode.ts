import { useDark } from "@vueuse/core"

const isDark = useDark({
  storageKey: "squit-color-scheme",
  valueDark: "dark",
  valueLight: "light",
})

export function useDarkMode() {
  return { isDark }
}
