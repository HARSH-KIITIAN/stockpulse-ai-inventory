import { apiRequest } from './client';
import type {
  OrderResponse,
  PageResponse,
  PricingActionResponse,
  PricingSuggestionResponse,
  ProductResponse,
  ReorderActionResponse,
  ReorderSuggestionResponse,
  SuggestionDecision,
} from '../types/api';

export function getProducts(): Promise<PageResponse<ProductResponse>> {
  return apiRequest('/products?page=0&size=100&sort=name,asc');
}

export function getPricingSuggestions(): Promise<PageResponse<PricingSuggestionResponse>> {
  return apiRequest('/pricing-suggestions?status=PENDING&page=0&size=100&sort=createdAt,desc');
}

export function getReorderSuggestions(): Promise<PageResponse<ReorderSuggestionResponse>> {
  return apiRequest('/reorder-suggestions?status=PENDING&page=0&size=100&sort=createdAt,desc');
}

export function simulateSale(productId: string, quantity: number): Promise<OrderResponse> {
  return apiRequest(`/products/${productId}/orders`, {
    method: 'POST',
    body: JSON.stringify({ quantity }),
  });
}

export function decidePricing(id: number, decision: SuggestionDecision): Promise<PricingActionResponse> {
  return apiRequest(`/pricing-suggestions/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ decision }),
  });
}

export function decideReorder(id: number, decision: SuggestionDecision): Promise<ReorderActionResponse> {
  return apiRequest(`/reorder-suggestions/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ decision }),
  });
}