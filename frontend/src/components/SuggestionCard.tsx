import type { PricingSuggestionResponse, ReorderSuggestionResponse, SuggestionDecision } from '../types/api';
import { TriggerBadge } from './StatusBadge';

const money = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' });

interface ActionProps {
  onDecision: (decision: SuggestionDecision) => void;
  disabled: boolean;
}

export function PricingSuggestionCard({ suggestion, onDecision, disabled }: {
  suggestion: PricingSuggestionResponse;
} & ActionProps) {
  return (
    <section className="suggestion-card">
      <div className="suggestion-header">
        <h3>Pricing suggestion</h3>
        <TriggerBadge reason={suggestion.triggerReason} />
      </div>
      <div className="recommendation-value">
        {money.format(suggestion.recommendedPrice)}
        <span>{Math.round(suggestion.confidence * 100)}% confidence · {suggestion.direction}</span>
      </div>
      <p>{suggestion.reasoning}</p>
      <div className="suggestion-actions">
        <button className="button primary" disabled={disabled} onClick={() => onDecision('ACCEPT')}>Accept price</button>
        <button className="button danger" disabled={disabled} onClick={() => onDecision('REJECT')}>Reject</button>
      </div>
    </section>
  );
}

export function ReorderSuggestionCard({ suggestion, onDecision, disabled }: {
  suggestion: ReorderSuggestionResponse;
} & ActionProps) {
  return (
    <section className="suggestion-card">
      <div className="suggestion-header">
        <h3>Reorder suggestion</h3>
        <TriggerBadge reason={suggestion.triggerReason} />
      </div>
      <div className="recommendation-value">
        +{suggestion.recommendedQuantity} units
        <span>{Math.round(suggestion.confidence * 100)}% confidence · {suggestion.suggestedLeadTimeDays}d lead time</span>
      </div>
      <p>{suggestion.reasoning}</p>
      <div className="suggestion-actions">
        <button className="button primary" disabled={disabled} onClick={() => onDecision('ACCEPT')}>Accept reorder</button>
        <button className="button danger" disabled={disabled} onClick={() => onDecision('REJECT')}>Reject</button>
      </div>
    </section>
  );
}