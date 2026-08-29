import type { CoinQuantity, PurchaseResponse } from '../../types/vending'
import { formatMoney } from '../../utils/money'

export interface PurchaseFailure {
  message: string
  returnedCoins: CoinQuantity[]
}

interface PurchaseResultProps {
  result: PurchaseResponse | null
  failure: PurchaseFailure | null
  returnedCoins: CoinQuantity[] | null
}

function CoinBreakdown({ coins }: { coins: CoinQuantity[] }) {
  if (coins.length === 0) {
    return <span>None</span>
  }

  return (
    <ul className="coin-breakdown">
      {coins.map((coin) => (
        <li key={coin.denomination}>
          {formatMoney(coin.denomination)} × {coin.quantity}
        </li>
      ))}
    </ul>
  )
}

function PurchaseResult({ result, failure, returnedCoins }: PurchaseResultProps) {
  if (result !== null) {
    return (
      <section className="purchase-result purchase-result--success" aria-live="polite">
        <h2>Purchase complete</h2>
        <dl className="purchase-details">
          <div>
            <dt>Product</dt>
            <dd>
              {result.product.name} — {formatMoney(result.product.price)}
            </dd>
          </div>
          <div>
            <dt>Inserted amount</dt>
            <dd>{formatMoney(result.insertedAmount)}</dd>
          </div>
          <div>
            <dt>Change amount</dt>
            <dd>{formatMoney(result.changeAmount)}</dd>
          </div>
          <div>
            <dt>Returned change</dt>
            <dd>
              <CoinBreakdown coins={result.change} />
            </dd>
          </div>
        </dl>
      </section>
    )
  }

  if (failure !== null) {
    return (
      <section className="purchase-result purchase-result--error" role="alert">
        <h2>Purchase could not be completed</h2>
        <p>{failure.message}</p>
        {failure.returnedCoins.length > 0 && (
          <div className="refund-details">
            <strong>Refunded coins</strong>
            <CoinBreakdown coins={failure.returnedCoins} />
          </div>
        )}
      </section>
    )
  }

  if (returnedCoins !== null) {
    return (
      <section className="purchase-result purchase-result--return" aria-live="polite">
        <h2>Coins returned</h2>
        <p>The transaction was reset without a purchase. Your pending coins were returned:</p>
        <CoinBreakdown coins={returnedCoins} />
      </section>
    )
  }

  return null
}

export default PurchaseResult
