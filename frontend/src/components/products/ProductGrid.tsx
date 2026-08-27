import type { Product } from '../../types/product'
import ProductCard from './ProductCard'

interface ProductGridProps {
  products: Product[]
  selectedProductId: number | null
  onSelect: (product: Product) => void
}

function ProductGrid({ products, selectedProductId, onSelect }: ProductGridProps) {
  if (products.length === 0) {
    return <p className="catalog-empty">No products are currently available.</p>
  }

  return (
    <div className="product-grid" role="radiogroup" aria-label="Available products">
      {products.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          isSelected={product.id === selectedProductId}
          onSelect={onSelect}
        />
      ))}
    </div>
  )
}

export default ProductGrid
