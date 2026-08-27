import type { ApiProblem } from '../types/api'

export class ApiError extends Error {
  readonly problem: ApiProblem

  constructor(problem: ApiProblem) {
    super(problem.detail ?? problem.title)
    this.name = 'ApiError'
    this.problem = problem
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json, application/problem+json')

  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, { ...init, headers })
  const body = await parseJson(response)

  if (!response.ok) {
    throw new ApiError(toApiProblem(response, body))
  }

  return body as T
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text()
  if (text.length === 0) {
    return undefined
  }

  try {
    return JSON.parse(text) as unknown
  } catch {
    return undefined
  }
}

function toApiProblem(response: Response, body: unknown): ApiProblem {
  if (isApiProblem(body)) {
    return body
  }

  return {
    title: response.statusText || 'Request failed',
    status: response.status,
  }
}

function isApiProblem(value: unknown): value is ApiProblem {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Record<string, unknown>
  return typeof candidate.title === 'string' && typeof candidate.status === 'number'
}
