import type { ProductStatus, TriggerReason } from '../types/api';

function label(value: string): string {
  return value.replace(/_/g, ' ');
}

export function ProductStatusBadge({ status }: { status: ProductStatus }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}>{label(status)}</span>;
}

export function TriggerBadge({ reason }: { reason: TriggerReason }) {
  return <span className={`trigger-badge trigger-${reason.toLowerCase()}`}>{label(reason)}</span>;
}