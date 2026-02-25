import { format, parseISO } from 'date-fns'

export function formatDate(iso: string | null | undefined, fmt = 'MMM d, yyyy'): string {
  if (!iso) return '—'
  try { return format(parseISO(iso), fmt) } catch { return iso }
}

export function formatDateTime(iso: string | null | undefined): string {
  return formatDate(iso, 'MMM d, yyyy · HH:mm')
}

export const STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  PENDING: 'Pending',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  PARTIAL: 'Partial',
  PROCESSING: 'Processing',
}

export function truncate(str: string, n = 30): string {
  return str.length > n ? str.slice(0, n) + '…' : str
}
