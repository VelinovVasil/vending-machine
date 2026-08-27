import type { Product } from '../../types/product'
import { formatMoney } from '../../utils/money'

interface ProductCardProps {
  product: Product
}

function ProductCard({ product }: ProductCardProps) {
  const isOutOfStock = product.quantity === 0

  return (
    <article className={`product-card${isOutOfStock ? ' product-card--out-of-stock' : ''}`}>
      <div>
        <h2>{product.name}</h2>
        <p className="product-price">{formatMoney(product.price)}</p>
      </div>

      <div className="product-stock">
        <span>Quantity: {product.quantity}</span>
        {isOutOfStock && <strong>Out of stock</strong>}
      </div>
    </article>
  )
}

export default ProductCard
