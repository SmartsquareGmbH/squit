import { findSquitResult, getSquitData } from "../data.ts"

const data = await getSquitData()

export function useSquitData() {
  return data
}

export function useSquitResult(id: number) {
  return findSquitResult(data.results, id)
}
