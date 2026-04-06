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

    try {
      return JSON.parse(raw)
    } catch {
      throw new Error("Failed to parse test results data. The report file may be corrupted or incomplete.")
    }
  }
}

export type SquitResultNodeStats = { success: number; failed: number; ignored: number }

export function getResultNodeStats(node: SquitResultNode | SquitResult): SquitResultNodeStats {
  if (isSquitResult(node)) {
    if (node.ignored) return { success: 0, failed: 0, ignored: 1 }

    return {
      success: node.success ? 1 : 0,
      failed: node.success ? 0 : 1,
      ignored: 0,
    }
  } else {
    return Object.values(node).reduce(
      (acc, child) => {
        const stats = getResultNodeStats(child)

        return {
          success: acc.success + stats.success,
          failed: acc.failed + stats.failed,
          ignored: acc.ignored + stats.ignored,
        }
      },
      { success: 0, failed: 0, ignored: 0 },
    )
  }
}

export type SquitResultData = SquitResult & {
  name: string
  path: string[]
}

export function findSquitResult(node: SquitResultNode | SquitResult, id: number): SquitResultData | undefined {
  return findSquitResultRec(node, id, [])
}

function findSquitResultRec(
  node: SquitResultNode | SquitResult,
  id: number,
  path: string[],
): SquitResultData | undefined {
  for (const [name, value] of Object.entries(node)) {
    if (isSquitResult(value)) {
      if (value.id === id) return { ...value, name, path }
    } else {
      const found = findSquitResultRec(value, id, [...path, name])
      if (found) return found
    }
  }
}

export function nodeMatchesSearch(node: SquitResultNode | SquitResult, name: string, query: string): boolean {
  if (!query) return true

  const q = query.toLowerCase()

  if (isSquitResult(node)) {
    return (node.alternativeName || name).toLowerCase().includes(q)
  }

  return Object.entries(node).some(([childName, child]) => nodeMatchesSearch(child, childName, q))
}

export function isSquitResult(node: SquitResultNode | SquitResult): node is SquitResult {
  return "id" in node && "success" in node
}
