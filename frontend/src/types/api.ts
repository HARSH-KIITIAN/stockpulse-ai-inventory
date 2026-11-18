export type ProductCategory = 'ELECTRONICS' | 'APPAREL' | 'HOME';
export type ProductStatus = 'ACTIVE' | 'PRICE_REVIEW_PENDING' | 'OUT_OF_STOCK';
export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';
export type PricingDirection = 'INCREASE' | 'DECREASE' | 'HOLD';
export type TriggerReason = 'INITIAL' | 'INVENTORY_LOW' | 'DEMAND_SPIKE' | 'MANUAL';
export type SuggestionDecision = 'ACCEPT' | 'REJECT';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ProductResponse {
  id: string;
  sku: string;
  name: string;
  category: ProductCategory;
  currentPrice: number;
  costPrice: number | null;
  stockLevel: number;
  reorderThreshold: number;
  demandVelocity: number;
  status: ProductStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrderResponse {
  productId: string;
  quantity: number;
  previousStockLevel: number;
  newStockLevel: number;
  previousDemandVelocity: number;
  newDemandVelocity: number;
  recommendationsQueued: boolean;
}

export interface PricingSuggestionResponse {
  id: number;
  productId: string;
  currentPrice: number;
  recommendedPrice: number;
  direction: PricingDirection;
  confidence: number;
  reasoning: string;
  status: SuggestionStatus;
  triggerReason: TriggerReason;
  createdAt: string;
  updatedAt: string;
}

export interface ReorderSuggestionResponse {
  id: number;
  productId: string;
  currentStock: number;
  recommendedQuantity: number;
  suggestedLeadTimeDays: number;
  confidence: number;
  reasoning: string;
  status: SuggestionStatus;
  triggerReason: TriggerReason;
  createdAt: string;
  updatedAt: string;
}

export interface PricingActionResponse {
  suggestion: PricingSuggestionResponse;
  product: ProductResponse;
}

export interface ReorderActionResponse {
  suggestion: ReorderSuggestionResponse;
  product: ProductResponse;
}

export interface ApiErrorResponse {
  message?: string;
  code?: string;
  fieldErrors?: Record<string, string>;
}