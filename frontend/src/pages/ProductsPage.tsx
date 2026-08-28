import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/client'
import {
  createProduct,
  deleteProduct,
  getProducts,
  updateProduct,
} from '../api/productsApi'
import DeleteProductConfirmation from '../components/products/DeleteProductConfirmation'
import ProductForm from '../components/products/ProductForm'
import ProductManagementGrid from '../components/products/ProductManagementGrid'
import type { CreateProductRequest, Product, ProductPageResponse } from '../types/product'

const PAGE_SIZE = 12

type EditorState = { mode: 'create' } | { mode: 'edit'; product: Product } | null

interface RequestFailure {
  message: string
  fieldErrors: Record<string, string>
}

function requestFailure(reason: unknown, fallback: string): RequestFailure {
  if (reason instanceof ApiError) {
    return {
      message: reason.problem.detail ?? reason.problem.title,
      fieldErrors: reason.problem.errors ?? {},
    }
  }

  return {
    message: reason instanceof Error ? reason.message : fallback,
    fieldErrors: {},
  }
}

function ProductsPage() {
  const [page, setPage] = useState(0)
  const [refreshCount, setRefreshCount] = useState(0)
  const [productPage, setProductPage] = useState<ProductPageResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [editor, setEditor] = useState<EditorState>(null)
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null)
  const [mutationFailure, setMutationFailure] = useState<RequestFailure | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [mutationState, setMutationState] = useState<'idle' | 'saving' | 'deleting'>('idle')
  const mutationInFlight = useRef(false)

  useEffect(() => {
    let ignoreResult = false

    getProducts(page, PAGE_SIZE)
      .then((response) => {
        if (ignoreResult) {
          return
        }

        if (response.content.length === 0 && page > 0 && response.totalPages <= page) {
          setPage(Math.max(0, response.totalPages - 1))
          return
        }

        setProductPage(response)
        setLoadError(null)
      })
      .catch((reason: unknown) => {
        if (!ignoreResult) {
          setLoadError(requestFailure(reason, 'Unable to load products.').message)
        }
      })

    return () => {
      ignoreResult = true
    }
  }, [page, refreshCount])

  const isMutating = mutationState !== 'idle'
  const isLoading = productPage === null && loadError === null

  function clearMutationFeedback() {
    setMutationFailure(null)
  }

  function refreshProducts() {
    setProductPage(null)
    setLoadError(null)
    setRefreshCount((count) => count + 1)
  }

  function retryLoading() {
    refreshProducts()
  }

  function loadPage(nextPage: number) {
    setProductPage(null)
    setLoadError(null)
    setEditor(null)
    setDeleteTarget(null)
    clearMutationFeedback()
    setPage(nextPage)
  }

  function startCreating() {
    setEditor({ mode: 'create' })
    setDeleteTarget(null)
    setMutationFailure(null)
    setSuccessMessage(null)
  }

  function startEditing(product: Product) {
    setEditor({ mode: 'edit', product })
    setDeleteTarget(null)
    setMutationFailure(null)
    setSuccessMessage(null)
  }

  function startDeleting(product: Product) {
    setDeleteTarget(product)
    setEditor(null)
    setMutationFailure(null)
    setSuccessMessage(null)
  }

  function cancelAction() {
    if (isMutating) {
      return
    }

    setEditor(null)
    setDeleteTarget(null)
    clearMutationFeedback()
  }

  async function saveProduct(request: CreateProductRequest) {
    if (editor === null || mutationInFlight.current) {
      return
    }

    mutationInFlight.current = true
    setMutationState('saving')
    setMutationFailure(null)
    setSuccessMessage(null)

    try {
      if (editor.mode === 'create') {
        const createdProduct = await createProduct(request)
        setSuccessMessage(`${createdProduct.name} was created successfully.`)
      } else {
        const updatedProduct = await updateProduct(editor.product.id, request)
        setSuccessMessage(`${updatedProduct.name} was updated successfully.`)
      }

      setEditor(null)
      refreshProducts()
    } catch (reason: unknown) {
      setMutationFailure(requestFailure(reason, 'Unable to save the product.'))
    } finally {
      mutationInFlight.current = false
      setMutationState('idle')
    }
  }

  async function confirmDelete() {
    if (deleteTarget === null || mutationInFlight.current) {
      return
    }

    const productToDelete = deleteTarget
    mutationInFlight.current = true
    setMutationState('deleting')
    setMutationFailure(null)
    setSuccessMessage(null)

    try {
      const deletedProduct = await deleteProduct(productToDelete.id)
      setDeleteTarget(null)
      setSuccessMessage(`${deletedProduct.name} was deleted successfully.`)

      if (page > 0 && productPage?.content.length === 1) {
        setProductPage(null)
        setLoadError(null)
        setPage(page - 1)
      } else {
        refreshProducts()
      }
    } catch (reason: unknown) {
      setMutationFailure(requestFailure(reason, 'Unable to delete the product.'))
    } finally {
      mutationInFlight.current = false
      setMutationState('idle')
    }
  }

  return (
    <section className="products-page">
      <div className="management-heading">
        <div className="page-introduction">
          <h1>Product Management</h1>
          <p>Create products and maintain the catalog stored in backend application memory.</p>
        </div>
        <button
          type="button"
          className="primary-action"
          disabled={isMutating}
          onClick={startCreating}
        >
          Create product
        </button>
      </div>

      {successMessage !== null && (
        <p className="management-message management-message--success" role="status">
          {successMessage}
        </p>
      )}

      {mutationFailure !== null && (
        <div className="management-message management-message--error" role="alert">
          <strong>{mutationFailure.message}</strong>
        </div>
      )}

      {editor !== null && (
        <ProductForm
          key={editor.mode === 'create' ? 'create' : `edit-${editor.product.id}`}
          mode={editor.mode}
          product={editor.mode === 'edit' ? editor.product : undefined}
          isSubmitting={mutationState === 'saving'}
          serverErrors={mutationFailure?.fieldErrors}
          onSubmit={saveProduct}
          onCancel={cancelAction}
          onChange={clearMutationFeedback}
        />
      )}

      {deleteTarget !== null && (
        <DeleteProductConfirmation
          product={deleteTarget}
          isDeleting={mutationState === 'deleting'}
          onConfirm={confirmDelete}
          onCancel={cancelAction}
        />
      )}

      {isLoading && (
        <p className="catalog-status" role="status">
          Loading products…
        </p>
      )}

      {loadError !== null && (
        <div className="catalog-status catalog-error" role="alert">
          <p>{loadError}</p>
          <button type="button" onClick={retryLoading}>
            Retry
          </button>
        </div>
      )}

      {productPage !== null && (
        <>
          <div className="management-list-heading">
            <h2>Products</h2>
            <span>{productPage.totalElements} total</span>
          </div>

          <ProductManagementGrid
            products={productPage.content}
            disabled={isMutating}
            onEdit={startEditing}
            onDelete={startDeleting}
          />

          <nav className="pagination" aria-label="Product management pages">
            <button
              type="button"
              onClick={() => loadPage(productPage.page - 1)}
              disabled={productPage.page === 0 || isMutating}
            >
              Previous
            </button>
            <span>
              Page {productPage.page + 1} of {Math.max(productPage.totalPages, 1)}
            </span>
            <button
              type="button"
              onClick={() => loadPage(productPage.page + 1)}
              disabled={productPage.page + 1 >= productPage.totalPages || isMutating}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </section>
  )
}

export default ProductsPage
