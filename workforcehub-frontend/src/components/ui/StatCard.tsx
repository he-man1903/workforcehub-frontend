import { cn } from '@/utils/cn'
import type { LucideIcon } from 'lucide-react'

interface StatCardProps {
  label: string
  value: string | number
  icon: LucideIcon
  trend?: string
  trendUp?: boolean
  accent?: 'brand' | 'teal' | 'amber' | 'rose'
  delay?: number
}

const accentMap = {
  brand: 'text-brand-bright bg-brand/10 border-brand/20',
  teal:  'text-accent-teal bg-accent-teal/10 border-accent-teal/20',
  amber: 'text-accent-amber bg-accent-amber/10 border-accent-amber/20',
  rose:  'text-accent-rose bg-accent-rose/10 border-accent-rose/20',
}

export function StatCard({ label, value, icon: Icon, trend, trendUp, accent = 'brand', delay = 0 }: StatCardProps) {
  return (
    <div
      className="bg-surface-2 border border-surface-4 rounded-2xl p-5 shadow-card opacity-0-init animate-fade-up"
      style={{ animationDelay: `${delay}ms`, animationFillMode: 'forwards' }}
    >
      <div className="flex items-start justify-between mb-4">
        <div className={cn('w-10 h-10 rounded-xl border flex items-center justify-center', accentMap[accent])}>
          <Icon size={18} />
        </div>
        {trend && (
          <span className={cn('text-xs font-mono', trendUp ? 'text-accent-teal' : 'text-accent-rose')}>
            {trendUp ? '↑' : '↓'} {trend}
          </span>
        )}
      </div>
      <div className="text-2xl font-display text-ink-bright mb-1">{value.toLocaleString()}</div>
      <div className="text-sm text-ink-muted">{label}</div>
    </div>
  )
}
