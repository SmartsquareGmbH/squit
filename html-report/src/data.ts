export type SquitData = {
  startedAt: string
  totalDuration: number
  averageDuration: number
  slowestTest?: {
    id: number
    name: string
    duration: number
  }
  results: SquitResultNode
}

export type SquitResultNode = { [key: string]: SquitResultNode | SquitResult }

export type SquitResult = {
  id: number
  alternativeName: string
  description?: string
  success: boolean
  ignored: boolean
  error: boolean
  duration: number
  expected: string
  actual: string
  infoExpected?: string
  infoActual?: string
  language?: string
}

export async function getSquitData(): Promise<SquitData> {
  if (import.meta.env.DEV) {
    const { default: data } = await import("../sample-data.json")

    return data
  } else {
    const raw = document.getElementById("squit-data")?.textContent ?? ""

    return JSON.parse(raw)
  }
}

export type SquitResultNodeStats = { success: number; failed: number }

export function getResultNodeStats(node: SquitResultNode | SquitResult): SquitResultNodeStats {
  if (isSquitResult(node)) {
    if (node.ignored) return { success: 0, failed: 0 }

    return {
      success: node.success ? 1 : 0,
      failed: node.success ? 0 : 1,
    }
  } else {
    return Object.values(node).reduce(
      (acc, child) => {
        const stats = getResultNodeStats(child)

        return {
          success: acc.success + stats.success,
          failed: acc.failed + stats.failed,
        }
      },
      { success: 0, failed: 0 },
    )
  }
}

export function findSquitResult(
  node: SquitResultNode | SquitResult,
  id: number,
): (SquitResult & { name: string }) | undefined {
  for (const [name, value] of Object.entries(node)) {
    if (isSquitResult(value)) {
      if (value.id === id) return { ...value, name }
    } else {
      const found = findSquitResult(value, id)
      if (found) return found
    }
  }
}

export function isSquitResult(node: SquitResultNode | SquitResult): node is SquitResult {
  return !Object.values(node).every((value) => typeof value === "object" && value != null)
}
