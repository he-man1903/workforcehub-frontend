import { cn } from '@/utils/cn'
import { Loader2 } from 'lucide-react'

export function Spinner({ className, size = 20 }: { className?: string; size?: number }) {
  return <Loader2 size={size} className={cn('animate-spin text-brand', className)} />
}

export function FullPageSpinner() {
  return (
    <div className="min-h-screen bg-surface-0 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <Spinner size={32} />
        <p className="text-ink-muted text-sm font-mono">Loading…</p>
      </div>
    </div>
  )
}
