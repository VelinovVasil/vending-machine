import type { Product } from './product'

export interface VendingConfigurationResponse {
  currency: string
  denominations: number[]
}

export interface CoinQuantity {
  denomination: number
  quantity: number
}

export interface PurchaseRequest {
  productId: number
  coins: CoinQuantity[]
}

export interface PurchaseResponse {
  product: Product
  insertedAmount: number
  changeAmount: number
  change: CoinQuantity[]
}
