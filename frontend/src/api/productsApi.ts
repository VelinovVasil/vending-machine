import { apiRequest } from './client'
import type {
  CreateProductRequest,
  Product,
  ProductPageResponse,
  UpdateProductRequest,
} from '../types/product'

export function getProducts(page = 0, size = 20): Promise<ProductPageResponse> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  return apiRequest<ProductPageResponse>(`/api/products?${query}`)
}

export function getProduct(id: number): Promise<Product> {
  return apiRequest<Product>(`/api/products/${id}`)
}

export function createProduct(request: CreateProductRequest): Promise<Product> {
  return apiRequest<Product>('/api/products', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function updateProduct(id: number, request: UpdateProductRequest): Promise<Product> {
  return apiRequest<Product>(`/api/products/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function deleteProduct(id: number): Promise<Product> {
  return apiRequest<Product>(`/api/products/${id}`, { method: 'DELETE' })
}
