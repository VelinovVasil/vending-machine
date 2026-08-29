import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getProducts } from '../api/productsApi'
import { getDenominations, purchaseProduct } from '../api/vendingApi'
import ProductGrid from '../components/products/ProductGrid'
import CoinSelector from '../components/vending/CoinSelector'
import PurchaseResult, {
  type PurchaseFailure,
} from '../components/vending/PurchaseResult'
import TransactionSummary from '../components/vending/TransactionSummary'
import type { Product, ProductPageResponse } from '../types/product'
import type { CoinQuantity, PurchaseResponse } from '../types/vending'

const PAGE_SIZE = 12

function VendingPage() {
  const [page, setPage] = useState(0)
  const [retryCount, setRetryCount] = useState(0)
  const [productPage, setProductPage] = useState<ProductPageResponse | null>(null)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [denominations, setDenominations] = useState<number[] | null>(null)
  const [denominationError, setDenominationError] = useState<string | null>(null)
  const [denominationRetryCount, setDenominationRetryCount] = useState(0)
  const [insertedCoins, setInsertedCoins] = useState<Record<number, number>>({})
  const [isPurchasing, setIsPurchasing] = useState(false)
  const [purchaseResult, setPurchaseResult] = useState<PurchaseResponse | null>(null)
  const [purchaseFailure, setPurchaseFailure] = useState<PurchaseFailure | null>(null)
  const [returnedCoins, setReturnedCoins] = useState<CoinQuantity[] | null>(null)

  useEffect(() => {
    let ignoreResult = false

    getProducts(page, PAGE_SIZE)
      .then((response) => {
        if (!ignoreResult) {
          setProductPage(response)
          setError(null)
          setSelectedProduct((currentSelection) => {
            if (currentSelection === null) {
              return null
            }

            const refreshedProduct = response.content.find(
              (product) => product.id === currentSelection.id,
            )
            if (refreshedProduct === undefined) {
              return currentSelection
            }

            return refreshedProduct.quantity === 0 ? null : refreshedProduct
          })
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

  useEffect(() => {
    let ignoreResult = false

    getDenominations()
      .then((response) => {
        if (!ignoreResult) {
          setDenominations(response.denominations)
          setDenominationError(null)
        }
      })
      .catch((reason: unknown) => {
        if (!ignoreResult) {
          setDenominationError(
            reason instanceof Error ? reason.message : 'Unable to load accepted coins.',
          )
        }
      })

    return () => {
      ignoreResult = true
    }
  }, [denominationRetryCount])

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

  function retryDenominations() {
    setDenominations(null)
    setDenominationError(null)
    setDenominationRetryCount((count) => count + 1)
  }

  function clearPurchaseFeedback() {
    setPurchaseResult(null)
    setPurchaseFailure(null)
    setReturnedCoins(null)
  }

  function selectProduct(product: Product) {
    clearPurchaseFeedback()
    setSelectedProduct(product)
  }

  function insertCoin(denomination: number) {
    clearPurchaseFeedback()
    setInsertedCoins((currentCoins) => ({
      ...currentCoins,
      [denomination]: (currentCoins[denomination] ?? 0) + 1,
    }))
  }

  function resetTransaction() {
    setSelectedProduct(null)
    setInsertedCoins({})
    setPurchaseResult(null)
    setPurchaseFailure(null)
    setReturnedCoins(coinSelection.length > 0 ? coinSelection : null)
  }

  const coinSelection: CoinQuantity[] = Object.entries(insertedCoins)
    .filter(([, quantity]) => quantity > 0)
    .map(([denomination, quantity]) => ({
      denomination: Number(denomination),
      quantity,
    }))
    .sort((first, second) => first.denomination - second.denomination)

  const insertedAmount = coinSelection.reduce(
    (total, coin) => total + coin.denomination * coin.quantity,
    0,
  )

  async function submitPurchase() {
    if (selectedProduct === null || coinSelection.length === 0 || isPurchasing) {
      return
    }

    setIsPurchasing(true)
    clearPurchaseFeedback()

    try {
      const result = await purchaseProduct({
        productId: selectedProduct.id,
        coins: coinSelection,
      })

      setPurchaseResult(result)
      setSelectedProduct(null)
      setInsertedCoins({})
      setProductPage(null)
      setError(null)
      setRetryCount((count) => count + 1)
    } catch (reason: unknown) {
      if (reason instanceof ApiError) {
        setPurchaseFailure({
          message: reason.problem.detail ?? reason.problem.title,
          returnedCoins: reason.problem.returnedCoins ?? [],
        })
        setInsertedCoins({})
      } else {
        setPurchaseFailure({
          message:
            reason instanceof Error
              ? reason.message
              : 'The purchase request could not be completed. Please try again.',
          returnedCoins: [],
        })
      }
    } finally {
      setIsPurchasing(false)
    }
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
          <ProductGrid
            products={productPage.content}
            selectedProductId={selectedProduct?.id ?? null}
            disabled={isPurchasing}
            onSelect={selectProduct}
          />

          <nav className="pagination" aria-label="Product catalog pages">
            <button
              type="button"
              onClick={() => loadPage(productPage.page - 1)}
              disabled={productPage.page === 0 || isPurchasing}
            >
              Previous
            </button>
            <span>
              Page {productPage.page + 1} of {Math.max(productPage.totalPages, 1)}
            </span>
            <button
              type="button"
              onClick={() => loadPage(productPage.page + 1)}
              disabled={productPage.page + 1 >= productPage.totalPages || isPurchasing}
            >
              Next
            </button>
          </nav>
        </>
      )}

      <div className="vending-controls">
        {denominations === null && denominationError === null && (
          <p className="transaction-panel catalog-status" role="status">
            Loading accepted coins…
          </p>
        )}

        {denominationError !== null && (
          <div className="transaction-panel catalog-status catalog-error" role="alert">
            <p>{denominationError}</p>
            <button type="button" onClick={retryDenominations}>
              Retry
            </button>
          </div>
        )}

        {denominations !== null && (
          <CoinSelector
            denominations={denominations}
            coinCounts={insertedCoins}
            disabled={isPurchasing}
            onInsert={insertCoin}
          />
        )}

        <TransactionSummary
          selectedProduct={selectedProduct}
          insertedAmount={insertedAmount}
          hasInsertedCoins={coinSelection.length > 0}
          isPurchasing={isPurchasing}
          onPurchase={submitPurchase}
          onReset={resetTransaction}
        />
      </div>

      <PurchaseResult
        result={purchaseResult}
        failure={purchaseFailure}
        returnedCoins={returnedCoins}
      />
    </section>
  )
}

export default VendingPage
