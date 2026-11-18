import { useCallback, useEffect, useMemo, useState } from 'react';
import { decidePricing, decideReorder, getPricingSuggestions, getProducts, getReorderSuggestions, simulateSale } from '../api/stockpulseApi';
import type { PricingSuggestionResponse, ProductResponse, ReorderSuggestionResponse, SuggestionDecision } from '../types/api';
import { ProductCard } from '../components/ProductCard';

interface DashboardData {
  products: ProductResponse[];
  pricing: PricingSuggestionResponse[];
  reorder: ReorderSuggestionResponse[];
}

const emptyData: DashboardData = { products: [], pricing: [], reorder: [] };

export function DashboardPage() {
  const [data, setData] = useState<DashboardData>(emptyData);
  const [loading, setLoading] = useState(true);
  const [busyProductId, setBusyProductId] = useState<string>();
  const [error, setError] = useState<string>();
  const [lastUpdated, setLastUpdated] = useState<Date>();

  const load = useCallback(async (showLoading = true): Promise<DashboardData | undefined> => {
    if (showLoading) setLoading(true);
    try {
      const [products, pricing, reorder] = await Promise.all([
        getProducts(),
        getPricingSuggestions(),
        getReorderSuggestions(),
      ]);
      const nextData = { products: products.content, pricing: pricing.content, reorder: reorder.content };
      setData(nextData);
      setLastUpdated(new Date());
      setError(undefined);
      return nextData;
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load StockPulse data.');
    } finally {
      if (showLoading) setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const pendingCount = data.pricing.length + data.reorder.length;
  const reviewCount = data.products.filter((product) => product.status !== 'ACTIVE').length;

  const pricingByProduct = useMemo(() => new Map(data.pricing.map((item) => [item.productId, item])), [data.pricing]);
  const reorderByProduct = useMemo(() => new Map(data.reorder.map((item) => [item.productId, item])), [data.reorder]);

  async function withProductBusy(productId: string, action: () => Promise<void>) {
    setBusyProductId(productId);
    try { await action(); } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'The action could not be completed.');
    } finally { setBusyProductId(undefined); }
  }

  async function handleSale(product: ProductResponse, quantity: number) {
    await withProductBusy(product.id, async () => {
      await simulateSale(product.id, quantity);
      // Automatic recommendations are eventually consistent. Poll for a short bounded period.
      for (let attempt = 0; attempt < 8; attempt += 1) {
        await new Promise((resolve) => window.setTimeout(resolve, 500));
        const refreshed = await load(false);
        const current = refreshed?.products.find((item) => item.id === product.id);
        const suggestionQueued = refreshed?.pricing.some((item) => item.productId === product.id)
          || refreshed?.reorder.some((item) => item.productId === product.id);
        if (current && (suggestionQueued
          || current.stockLevel < current.reorderThreshold
          || current.demandVelocity > product.demandVelocity)) break;
      }
    });
  }

  async function handlePricingDecision(productId: string, id: number, decision: SuggestionDecision) {
    await withProductBusy(productId, async () => { await decidePricing(id, decision); await load(false); });
  }

  async function handleReorderDecision(productId: string, id: number, decision: SuggestionDecision) {
    await withProductBusy(productId, async () => { await decideReorder(id, decision); await load(false); });
  }

  return (
    <main className="shell">
      <header className="hero">
        <div>
          <p className="eyebrow">STOCKPULSE / MERCHANDISING</p>
          <h1>Inventory intelligence, with a human checkpoint.</h1>
          <p className="subtitle">Observe stock and demand signals, review recommendations, and approve only the changes you want to publish.</p>
        </div>
        <button className="button secondary" disabled={loading} onClick={() => void load()}>Refresh data</button>
      </header>

      {error && <section className="error-banner" role="alert">{error}</section>}

      <section className="summary" aria-label="Catalog summary">
        <div><strong>{data.products.length}</strong><span>products</span></div>
        <div><strong>{pendingCount}</strong><span>pending suggestions</span></div>
        <div><strong>{reviewCount}</strong><span>products needing review</span></div>
        <div><strong>{lastUpdated ? lastUpdated.toLocaleTimeString() : '—'}</strong><span>last updated</span></div>
      </section>

      {loading && <p className="loading">Loading catalog…</p>}
      {!loading && data.products.length === 0 && <section className="empty-state"><h2>No products found</h2><p>Enable demo seed data or create a product through <code>POST /products</code>.</p></section>}
      {!loading && data.products.length > 0 && <section className="catalog" aria-live="polite">
        {data.products.map((product) => {
          const busy = busyProductId === product.id;
          return <ProductCard key={product.id} product={product} busy={busy}
            pricingSuggestion={pricingByProduct.get(product.id)} reorderSuggestion={reorderByProduct.get(product.id)}
            onSale={(quantity) => handleSale(product, quantity)}
            onPricingDecision={(decision) => handlePricingDecision(product.id, pricingByProduct.get(product.id)!.id, decision)}
            onReorderDecision={(decision) => handleReorderDecision(product.id, reorderByProduct.get(product.id)!.id, decision)} />;
        })}
      </section>}
    </main>
  );
}