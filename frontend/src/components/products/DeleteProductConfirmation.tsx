import type { Product } from '../../types/product'

interface DeleteProductConfirmationProps {
  product: Product
  isDeleting: boolean
  onConfirm: () => void
  onCancel: () => void
}

function DeleteProductConfirmation({
  product,
  isDeleting,
  onConfirm,
  onCancel,
}: DeleteProductConfirmationProps) {
  return (
    <section
      className="delete-confirmation"
      role="alertdialog"
      aria-modal="false"
      aria-labelledby="delete-product-heading"
      aria-describedby="delete-product-description"
    >
      <h2 id="delete-product-heading">Delete {product.name}?</h2>
      <p id="delete-product-description">
        This removes the product from the active catalog. This action requires confirmation.
      </p>
      <div className="form-actions">
        <button
          type="button"
          className="danger-action"
          disabled={isDeleting}
          onClick={onConfirm}
        >
          {isDeleting ? 'Deleting…' : 'Delete product'}
        </button>
        <button
          type="button"
          className="secondary-action"
          disabled={isDeleting}
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </section>
  )
}

export default DeleteProductConfirmation
