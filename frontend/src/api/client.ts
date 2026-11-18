import type { ApiErrorResponse } from '../types/api';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  const body = await response.json().catch(() => undefined) as T | ApiErrorResponse | undefined;
  if (!response.ok) {
    const error = body as ApiErrorResponse | undefined;
    const fieldErrors = error?.fieldErrors
      ? ` ${Object.values(error.fieldErrors).join(' ')}`
      : '';
    throw new Error(`${error?.message ?? `Request failed (${response.status})`}${fieldErrors}`);
  }
  return body as T;
}