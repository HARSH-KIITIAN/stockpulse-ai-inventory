import { useState } from 'react';
import type { PricingSuggestionResponse, ProductResponse, ReorderSuggestionResponse, SuggestionDecision } from '../types/api';
import { PricingSuggestionCard, ReorderSuggestionCard } from './SuggestionCard';
import { ProductStatusBadge } from './StatusBadge';

const money = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' });

interface ProductCardProps {
  product: ProductResponse;
  pricingSuggestion?: PricingSuggestionResponse;
  reorderSuggestion?: ReorderSuggestionResponse;
  busy: boolean;
  onSale: (quantity: number) => Promise<void>;
  onPricingDecision: (decision: SuggestionDecision) => Promise<void>;
  onReorderDecision: (decision: SuggestionDecision) => Promise<void>;
}

export function ProductCard({ product, pricingSuggestion, reorderSuggestion, busy, onSale,
  onPricingDecision, onReorderDecision }: ProductCardProps) {
  const [quantity, setQuantity] = useState(1);
  const [saleError, setSaleError] = useState<string | undefined>();

  async function submitSale() {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > product.stockLevel) {
      setSaleError(`Enter a whole number between 1 and ${product.stockLevel}.`);
      return;
    }
    setSaleError(undefined);
    await onSale(quantity);
  }

  return (
    <article className="product-card">
      <header className="product-header">
        <div>
          <p className="eyebrow">{product.category}</p>
          <h2>{product.name}</h2>
          <p className="sku">{product.sku}</p>
        </div>
        <ProductStatusBadge status={product.status} />
      </header>

      <div className="metrics">
        <div><span>Current price</span><strong>{money.format(product.currentPrice)}</strong></div>
        <div><span>Stock / threshold</span><strong>{product.stockLevel} / {product.reorderThreshold}</strong></div>
        <div><span>Demand velocity</span><strong>{product.demandVelocity}</strong></div>
      </div>

      <div className="product-content">
        {pricingSuggestion && <PricingSuggestionCard suggestion={pricingSuggestion} onDecision={onPricingDecision} disabled={busy} />}
        {reorderSuggestion && <ReorderSuggestionCard suggestion={reorderSuggestion} onDecision={onReorderDecision} disabled={busy} />}
        {!pricingSuggestion && !reorderSuggestion && <p className="no-suggestion">No pending recommendations.</p>}

        <div className="sale-control">
          <label htmlFor={`sale-${product.id}`}>Simulate sale</label>
          <input id={`sale-${product.id}`} type="number" min={1} max={Math.max(product.stockLevel, 1)}
            value={quantity} disabled={busy || product.stockLevel === 0}
            onChange={(event) => setQuantity(Number(event.target.value))} />
          <button className="button secondary" disabled={busy || product.stockLevel === 0} onClick={submitSale}>Record sale</button>
        </div>
        {saleError && <p className="inline-error">{saleError}</p>}
      </div>
    </article>
  );
}