import { useEffect, useState } from 'react'
import { getProducts } from '../api/productsApi'
import ProductGrid from '../components/products/ProductGrid'
import type { ProductPageResponse } from '../types/product'

const PAGE_SIZE = 12

function VendingPage() {
  const [page, setPage] = useState(0)
  const [retryCount, setRetryCount] = useState(0)
  const [productPage, setProductPage] = useState<ProductPageResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignoreResult = false

    getProducts(page, PAGE_SIZE)
      .then((response) => {
        if (!ignoreResult) {
          setProductPage(response)
        }
      })
      .catch((reason: unknown) => {
        if (!ignoreResult) {
          setError(reason instanceof Error ? reason.message : 'Unable to load products.')
        }
      })

    return () => {
      ignoreResult = true
    }
  }, [page, retryCount])

  function loadPage(nextPage: number) {
    setProductPage(null)
    setError(null)
    setPage(nextPage)
  }

  function retry() {
    setProductPage(null)
    setError(null)
    setRetryCount((count) => count + 1)
  }

  const isLoading = productPage === null && error === null

  return (
    <section className="vending-page">
      <div className="page-introduction">
        <h1>Vending Machine</h1>
        <p>Browse the products currently available in the machine.</p>
      </div>

      {isLoading && (
        <p className="catalog-status" role="status">
          Loading products…
        </p>
      )}

      {error !== null && (
        <div className="catalog-status catalog-error" role="alert">
          <p>{error}</p>
          <button type="button" onClick={retry}>
            Retry
          </button>
        </div>
      )}

      {productPage !== null && (
        <>
          <ProductGrid products={productPage.content} />

          <nav className="pagination" aria-label="Product catalog pages">
            <button
              type="button"
              onClick={() => loadPage(productPage.page - 1)}
              disabled={productPage.page === 0}
            >
              Previous
            </button>
            <span>
              Page {productPage.page + 1} of {Math.max(productPage.totalPages, 1)}
            </span>
            <button
              type="button"
              onClick={() => loadPage(productPage.page + 1)}
              disabled={productPage.page + 1 >= productPage.totalPages}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </section>
  )
}

export default VendingPage
