import type { Product } from '../../types/product'
import { formatMoney } from '../../utils/money'

interface ProductCardProps {
  product: Product
  isSelected: boolean
  onSelect: (product: Product) => void
}

function ProductCard({ product, isSelected, onSelect }: ProductCardProps) {
  const isOutOfStock = product.quantity === 0
  const className = [
    'product-card',
    isSelected ? 'product-card--selected' : '',
    isOutOfStock ? 'product-card--out-of-stock' : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button
      type="button"
      role="radio"
      aria-checked={isSelected}
      className={className}
      disabled={isOutOfStock}
      onClick={() => onSelect(product)}
    >
      <span className="product-details">
        <span className="product-name">{product.name}</span>
        <span className="product-price">{formatMoney(product.price)}</span>
      </span>

      <span className="product-stock">
        <span>Quantity: {product.quantity}</span>
        {isOutOfStock && <strong>Out of stock</strong>}
      </span>
    </button>
  )
}

export default ProductCard
