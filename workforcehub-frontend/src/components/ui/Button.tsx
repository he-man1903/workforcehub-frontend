import { cn } from '@/utils/cn'
import { type ButtonHTMLAttributes, forwardRef } from 'react'
import { Loader2 } from 'lucide-react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
}

const variantClasses = {
  primary:   'bg-brand hover:bg-brand-bright text-white shadow-glow-brand/50 hover:shadow-glow-brand',
  secondary: 'bg-surface-3 hover:bg-surface-4 text-ink border border-surface-5 hover:border-ink-faint',
  ghost:     'bg-transparent hover:bg-surface-2 text-ink-dim hover:text-ink',
  danger:    'bg-accent-rose/10 hover:bg-accent-rose/20 text-accent-rose border border-accent-rose/30',
}

const sizeClasses = {
  sm: 'px-3 py-1.5 text-xs gap-1.5 rounded-lg',
  md: 'px-4 py-2 text-sm gap-2 rounded-xl',
  lg: 'px-6 py-3 text-base gap-2.5 rounded-xl',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', loading, className, children, disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center font-medium transition-all duration-200',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/50',
        'disabled:opacity-50 disabled:cursor-not-allowed',
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
      {...props}
    >
      {loading ? <Loader2 className="animate-spin" size={14} /> : null}
      {children}
    </button>
  )
)
Button.displayName = 'Button'
