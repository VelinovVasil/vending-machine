import type { Product } from '../../types/product'
import { formatMoney } from '../../utils/money'

interface ProductManagementGridProps {
  products: Product[]
  disabled: boolean
  onEdit: (product: Product) => void
  onDelete: (product: Product) => void
}

function ProductManagementGrid({
  products,
  disabled,
  onEdit,
  onDelete,
}: ProductManagementGridProps) {
  if (products.length === 0) {
    return <p className="catalog-empty">No products are currently available.</p>
  }

  return (
    <div className="product-management-grid">
      {products.map((product) => (
        <article className="management-card" key={product.id}>
          <div>
            <span className="management-card-id">Product #{product.id}</span>
            <h2>{product.name}</h2>
          </div>

          <dl className="management-card-details">
            <div>
              <dt>Price</dt>
              <dd>{formatMoney(product.price)}</dd>
            </div>
            <div>
              <dt>Quantity</dt>
              <dd>{product.quantity}</dd>
            </div>
          </dl>

          <div className="management-card-actions">
            <button
              type="button"
              className="secondary-action"
              disabled={disabled}
              onClick={() => onEdit(product)}
            >
              Edit
            </button>
            <button
              type="button"
              className="danger-action"
              disabled={disabled}
              onClick={() => onDelete(product)}
            >
              Delete
            </button>
          </div>
        </article>
      ))}
    </div>
  )
}

export default ProductManagementGrid
