import { useState, type FormEvent } from 'react'
import type { CreateProductRequest, Product } from '../../types/product'

type ProductField = keyof CreateProductRequest
type ProductFieldErrors = Partial<Record<ProductField, string>>

interface ProductFormProps {
  mode: 'create' | 'edit'
  product?: Product
  isSubmitting: boolean
  serverErrors?: Record<string, string>
  onSubmit: (product: CreateProductRequest) => void
  onCancel: () => void
  onChange: () => void
}

interface FormValues {
  name: string
  price: string
  quantity: string
}

function initialValues(product?: Product): FormValues {
  return {
    name: product?.name ?? '',
    price: product === undefined ? '' : String(product.price),
    quantity: product === undefined ? '' : String(product.quantity),
  }
}

function validate(values: FormValues): {
  errors: ProductFieldErrors
  product?: CreateProductRequest
} {
  const errors: ProductFieldErrors = {}
  const name = values.name.trim()
  const price = Number(values.price)
  const quantity = Number(values.quantity)

  if (name.length === 0) {
    errors.name = 'Name is required.'
  } else if (name.length > 100) {
    errors.name = 'Name must be 100 characters or fewer.'
  }

  if (
    values.price.trim().length === 0 ||
    !Number.isSafeInteger(price) ||
    price <= 0 ||
    price % 10 !== 0
  ) {
    errors.price = 'Price must be a positive whole number divisible by 10 cents.'
  }

  if (
    values.quantity.trim().length === 0 ||
    !Number.isSafeInteger(quantity) ||
    quantity < 0 ||
    quantity > 15
  ) {
    errors.quantity = 'Quantity must be a whole number between 0 and 15.'
  }

  if (Object.keys(errors).length > 0) {
    return { errors }
  }

  return { errors, product: { name, price, quantity } }
}

function ProductForm({
  mode,
  product,
  isSubmitting,
  serverErrors = {},
  onSubmit,
  onCancel,
  onChange,
}: ProductFormProps) {
  const [values, setValues] = useState<FormValues>(() => initialValues(product))
  const [clientErrors, setClientErrors] = useState<ProductFieldErrors>({})
  const heading = mode === 'create' ? 'Create product' : `Edit ${product?.name ?? 'product'}`
  const fieldPrefix = mode === 'create' ? 'create-product' : `edit-product-${product?.id}`

  function updateField(field: keyof FormValues, value: string) {
    setValues((currentValues) => ({ ...currentValues, [field]: value }))
    setClientErrors((currentErrors) => ({ ...currentErrors, [field]: undefined }))
    onChange()
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) {
      return
    }

    const validation = validate(values)
    setClientErrors(validation.errors)
    if (validation.product !== undefined) {
      onSubmit(validation.product)
    }
  }

  function fieldError(field: ProductField): string | undefined {
    return clientErrors[field] ?? serverErrors[field]
  }

  return (
    <section className="product-editor" aria-labelledby={`${fieldPrefix}-heading`}>
      <h2 id={`${fieldPrefix}-heading`}>{heading}</h2>
      <form className="product-form" noValidate onSubmit={handleSubmit}>
        <div className="form-field">
          <label htmlFor={`${fieldPrefix}-name`}>Name</label>
          <input
            id={`${fieldPrefix}-name`}
            name="name"
            type="text"
            maxLength={100}
            value={values.name}
            disabled={isSubmitting}
            aria-invalid={fieldError('name') !== undefined}
            aria-describedby={fieldError('name') === undefined ? undefined : `${fieldPrefix}-name-error`}
            onChange={(event) => updateField('name', event.target.value)}
          />
          {fieldError('name') !== undefined && (
            <span id={`${fieldPrefix}-name-error`} className="field-error">
              {fieldError('name')}
            </span>
          )}
        </div>

        <div className="form-field">
          <label htmlFor={`${fieldPrefix}-price`}>Price (cents)</label>
          <input
            id={`${fieldPrefix}-price`}
            name="price"
            type="number"
            inputMode="numeric"
            min="10"
            step="10"
            value={values.price}
            disabled={isSubmitting}
            aria-invalid={fieldError('price') !== undefined}
            aria-describedby={fieldError('price') === undefined ? undefined : `${fieldPrefix}-price-error`}
            onChange={(event) => updateField('price', event.target.value)}
          />
          {fieldError('price') !== undefined && (
            <span id={`${fieldPrefix}-price-error`} className="field-error">
              {fieldError('price')}
            </span>
          )}
        </div>

        <div className="form-field">
          <label htmlFor={`${fieldPrefix}-quantity`}>Quantity</label>
          <input
            id={`${fieldPrefix}-quantity`}
            name="quantity"
            type="number"
            inputMode="numeric"
            min="0"
            max="15"
            step="1"
            value={values.quantity}
            disabled={isSubmitting}
            aria-invalid={fieldError('quantity') !== undefined}
            aria-describedby={
              fieldError('quantity') === undefined ? undefined : `${fieldPrefix}-quantity-error`
            }
            onChange={(event) => updateField('quantity', event.target.value)}
          />
          {fieldError('quantity') !== undefined && (
            <span id={`${fieldPrefix}-quantity-error`} className="field-error">
              {fieldError('quantity')}
            </span>
          )}
        </div>

        <div className="form-actions">
          <button type="submit" className="primary-action" disabled={isSubmitting}>
            {isSubmitting
              ? mode === 'create'
                ? 'Creating…'
                : 'Saving…'
              : mode === 'create'
                ? 'Create product'
                : 'Save changes'}
          </button>
          <button
            type="button"
            className="secondary-action"
            disabled={isSubmitting}
            onClick={onCancel}
          >
            Cancel
          </button>
        </div>
      </form>
    </section>
  )
}

export default ProductForm
