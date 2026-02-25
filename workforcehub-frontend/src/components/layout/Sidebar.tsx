import { NavLink } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import { useAuthUser } from '@/auth/useAuthUser'
import { clearBackendTokens } from '@/auth/backendAuth'
import { cn } from '@/utils/cn'
import {
  LayoutDashboard, Users, Upload, FileText,
  LogOut, ChevronRight, Zap
} from 'lucide-react'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/employees', label: 'Employees', icon: Users },
  { to: '/uploads', label: 'Uploads', icon: Upload },
]

export function Sidebar() {
  const auth = useAuth()
  const { user } = useAuthUser()

  return (
    <aside className="w-64 flex-shrink-0 bg-surface-1 border-r border-surface-3 flex flex-col h-screen sticky top-0">
      {/* Logo */}
      <div className="px-6 py-6 border-b border-surface-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-brand flex items-center justify-center shadow-glow-brand">
            <Zap size={16} className="text-white" />
          </div>
          <div>
            <div className="text-sm font-semibold text-ink-bright leading-none">WorkforceHub</div>
            <div className="text-xs text-ink-muted mt-0.5 font-mono truncate max-w-[120px]">
              {user?.tenantId ?? '—'}
            </div>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => cn(
              'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150 group',
              isActive
                ? 'bg-brand/15 text-brand-bright border border-brand/25'
                : 'text-ink-dim hover:text-ink hover:bg-surface-3'
            )}
          >
            {({ isActive }) => (
              <>
                <Icon size={17} className={cn('flex-shrink-0', isActive ? 'text-brand-bright' : 'text-ink-muted group-hover:text-ink')} />
                <span className="flex-1">{label}</span>
                {isActive && <ChevronRight size={14} className="text-brand-bright/60" />}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* User + Logout */}
      <div className="border-t border-surface-3 p-4">
        <div className="flex items-center gap-3 mb-3">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="" className="w-8 h-8 rounded-full ring-2 ring-surface-4" />
          ) : (
            <div className="w-8 h-8 rounded-full bg-brand/20 flex items-center justify-center text-brand-bright text-xs font-semibold">
              {user?.name?.charAt(0).toUpperCase() ?? 'U'}
            </div>
          )}
          <div className="flex-1 min-w-0">
            <div className="text-xs font-medium text-ink truncate">{user?.name}</div>
            <div className="text-xs text-ink-muted truncate">{user?.role}</div>
          </div>
        </div>
        <button
          onClick={() => {
            clearBackendTokens()
            auth.removeUser()
          }}
          className="w-full flex items-center gap-2 px-3 py-2 rounded-xl text-sm text-ink-dim hover:text-accent-rose hover:bg-accent-rose/10 transition-all duration-150"
        >
          <LogOut size={15} />
          Sign out
        </button>
      </div>
    </aside>
  )
}
