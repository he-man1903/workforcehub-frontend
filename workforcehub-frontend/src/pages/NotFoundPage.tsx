import { Link } from 'react-router-dom'
import { Zap } from 'lucide-react'

export function NotFoundPage() {
  return (
    <div className="min-h-screen bg-surface-0 flex items-center justify-center">
      <div className="text-center">
        <div className="text-8xl font-display text-surface-4 mb-4">404</div>
        <p className="text-ink-dim mb-6">This page doesn't exist.</p>
        <Link to="/dashboard" className="text-brand hover:text-brand-bright text-sm transition-colors">
          Back to Dashboard →
        </Link>
      </div>
    </div>
  )
}
