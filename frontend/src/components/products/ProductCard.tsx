import type { Product } from '../../types/product'
import { formatMoney } from '../../utils/money'

interface ProductCardProps {
  product: Product
  isSelected: boolean
  disabled?: boolean
  onSelect: (product: Product) => void
}

function ProductCard({
  product,
  isSelected,
  disabled = false,
  onSelect,
}: ProductCardProps) {
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
      disabled={isOutOfStock || disabled}
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
