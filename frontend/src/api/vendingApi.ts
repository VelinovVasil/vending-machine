import { apiRequest } from './client'
import type {
  PurchaseRequest,
  PurchaseResponse,
  VendingConfigurationResponse,
} from '../types/vending'

export function getDenominations(): Promise<VendingConfigurationResponse> {
  return apiRequest<VendingConfigurationResponse>('/api/vending/denominations')
}

export function purchaseProduct(request: PurchaseRequest): Promise<PurchaseResponse> {
  return apiRequest<PurchaseResponse>('/api/vending/purchases', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}
