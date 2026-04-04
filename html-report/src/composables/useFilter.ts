import { useStorage } from "@vueuse/core"

const failedOnly = useStorage("squit-failed-only", false)

export function useFilter() {
  return { failedOnly }
}
