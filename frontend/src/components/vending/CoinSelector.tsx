import { formatMoney } from '../../utils/money'

interface CoinSelectorProps {
  denominations: number[]
  coinCounts: Record<number, number>
  disabled: boolean
  onInsert: (denomination: number) => void
}

function CoinSelector({
  denominations,
  coinCounts,
  disabled,
  onInsert,
}: CoinSelectorProps) {
  return (
    <section className="transaction-panel" aria-labelledby="coin-selector-heading">
      <div className="panel-heading">
        <h2 id="coin-selector-heading">Insert coins</h2>
        <p>Select a denomination each time you insert a coin.</p>
      </div>

      <div className="coin-selector">
        {denominations.map((denomination) => {
          const count = coinCounts[denomination] ?? 0

          return (
            <button
              key={denomination}
              type="button"
              className="coin-button"
              disabled={disabled}
              onClick={() => onInsert(denomination)}
              aria-label={`Insert ${formatMoney(denomination)}`}
            >
              <span>{formatMoney(denomination)}</span>
              {count > 0 && <strong aria-label={`${count} inserted`}>× {count}</strong>}
            </button>
          )
        })}
      </div>
    </section>
  )
}

export default CoinSelector
