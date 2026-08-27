import type { Product } from '../../types/product'
import { formatMoney } from '../../utils/money'

interface TransactionSummaryProps {
  selectedProduct: Product | null
  insertedAmount: number
  hasInsertedCoins: boolean
  isPurchasing: boolean
  onPurchase: () => void
  onReset: () => void
}

function TransactionSummary({
  selectedProduct,
  insertedAmount,
  hasInsertedCoins,
  isPurchasing,
  onPurchase,
  onReset,
}: TransactionSummaryProps) {
  const canPurchase = selectedProduct !== null && hasInsertedCoins && !isPurchasing
  const canReset = (selectedProduct !== null || hasInsertedCoins) && !isPurchasing

  return (
    <section className="transaction-panel" aria-labelledby="transaction-summary-heading">
      <div className="panel-heading">
        <h2 id="transaction-summary-heading">Current transaction</h2>
      </div>

      <dl className="transaction-details" aria-live="polite">
        <div>
          <dt>Selected product</dt>
          <dd>
            {selectedProduct === null
              ? 'None selected'
              : `${selectedProduct.name} — ${formatMoney(selectedProduct.price)}`}
          </dd>
        </div>
        <div>
          <dt>Inserted total</dt>
          <dd>{formatMoney(insertedAmount)}</dd>
        </div>
      </dl>

      <div className="transaction-actions">
        <button
          type="button"
          className="primary-action"
          disabled={!canPurchase}
          onClick={onPurchase}
        >
          {isPurchasing ? 'Purchasing…' : 'Purchase'}
        </button>
        <button
          type="button"
          className="secondary-action"
          disabled={!canReset}
          onClick={onReset}
        >
          Reset
        </button>
      </div>
    </section>
  )
}

export default TransactionSummary
