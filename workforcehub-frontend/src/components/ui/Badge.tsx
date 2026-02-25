import { cn } from '@/utils/cn'

type Variant = 'active' | 'inactive' | 'pending' | 'completed' | 'failed' | 'partial' | 'processing' | 'default'

const variantClasses: Record<Variant, string> = {
  active:     'bg-accent-teal/15 text-accent-teal border-accent-teal/30',
  inactive:   'bg-ink-faint/30 text-ink-dim border-ink-faint/40',
  pending:    'bg-accent-amber/15 text-accent-amber border-accent-amber/30',
  completed:  'bg-accent-teal/15 text-accent-teal border-accent-teal/30',
  failed:     'bg-accent-rose/15 text-accent-rose border-accent-rose/30',
  partial:    'bg-accent-amber/15 text-accent-amber border-accent-amber/30',
  processing: 'bg-brand/15 text-brand-bright border-brand/30',
  default:    'bg-surface-4 text-ink-dim border-surface-5',
}

const statusToVariant: Record<string, Variant> = {
  ACTIVE: 'active', INACTIVE: 'inactive', PENDING: 'pending',
  COMPLETED: 'completed', FAILED: 'failed', PARTIAL: 'partial', PROCESSING: 'processing',
}

interface BadgeProps {
  status: string
  label?: string
  className?: string
}

export function Badge({ status, label, className }: BadgeProps) {
  const variant = statusToVariant[status] ?? 'default'
  return (
    <span className={cn(
      'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border',
      variantClasses[variant],
      className
    )}>
      <span className={cn('w-1.5 h-1.5 rounded-full bg-current', variant === 'processing' && 'animate-pulse')} />
      {label ?? status}
    </span>
  )
}
