import type { CoinQuantity } from './vending'

export type ApiErrorCode =
  | 'PRODUCT_NOT_FOUND'
  | 'INVALID_REQUEST'
  | 'INVALID_COIN_SELECTION'
  | 'OUT_OF_STOCK'
  | 'INSUFFICIENT_FUNDS'
  | 'EXACT_CHANGE_UNAVAILABLE'

export interface ApiProblem {
  type?: string
  title: string
  status: number
  detail?: string
  instance?: string
  errorCode?: ApiErrorCode
  errors?: Record<string, string>
  returnedCoins?: CoinQuantity[]
  price?: number
  insertedAmount?: number
  shortfall?: number
  changeDue?: number
}
