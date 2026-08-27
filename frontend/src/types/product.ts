export interface Product {
  id: number
  name: string
  price: number
  quantity: number
}

export interface CreateProductRequest {
  name: string
  price: number
  quantity: number
}

export interface UpdateProductRequest {
  name: string
  price: number
  quantity: number
}

export interface ProductPageResponse {
  content: Product[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
