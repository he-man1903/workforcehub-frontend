import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchEmployees } from '@/api/employees'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { formatDate, STATUS_LABELS } from '@/utils/format'
import { ChevronLeft, ChevronRight, Search, Filter, RefreshCw } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { EmployeeStatus } from '@/types'

const STATUS_FILTERS: Array<{ value: EmployeeStatus | ''; label: string }> = [
  { value: '', label: 'All' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'PENDING', label: 'Pending' },
]

export function EmployeeTable() {
  const [page, setPage]         = useState(0)
  const [size]                  = useState(15)
  const [statusFilter, setStatus] = useState<EmployeeStatus | ''>('')
  const [search, setSearch]     = useState('')

  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['employees', page, size, statusFilter],
    queryFn:  () => fetchEmployees({ page, size, status: statusFilter || undefined }),
    placeholderData: (prev) => prev,
  })

  const employees = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  const filtered = search
    ? employees.filter(e =>
        `${e.firstName} ${e.lastName} ${e.email} ${e.department ?? ''}`
          .toLowerCase().includes(search.toLowerCase())
      )
    : employees

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex items-center gap-3 flex-wrap">
        {/* Search */}
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted pointer-events-none" />
          <input
            type="text"
            placeholder="Search employees…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-surface-2 border border-surface-4 hover:border-surface-5 focus:border-brand/50 rounded-xl text-sm text-ink placeholder:text-ink-muted outline-none transition-colors"
          />
        </div>

        {/* Status filter pills */}
        <div className="flex items-center gap-1.5 bg-surface-2 border border-surface-4 rounded-xl p-1">
          <Filter size={14} className="text-ink-muted ml-1.5" />
          {STATUS_FILTERS.map(f => (
            <button
              key={f.value}
              onClick={() => { setStatus(f.value); setPage(0) }}
              className={cn(
                'px-3 py-1 rounded-lg text-xs font-medium transition-all',
                statusFilter === f.value
                  ? 'bg-brand text-white shadow-glow-brand/30'
                  : 'text-ink-dim hover:text-ink hover:bg-surface-3'
              )}
            >
              {f.label}
            </button>
          ))}
        </div>

        <Button variant="ghost" size="sm" onClick={() => refetch()} loading={isFetching}>
          <RefreshCw size={14} />
        </Button>
      </div>

      {/* Table */}
      <div className="bg-surface-2 border border-surface-4 rounded-2xl overflow-hidden shadow-card">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-surface-4">
                {['Name', 'Email', 'Department', 'Title', 'Hired', 'Status'].map(h => (
                  <th key={h} className="px-5 py-3.5 text-left text-xs font-semibold text-ink-muted uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-16 text-center">
                    <Spinner className="mx-auto" />
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-16 text-center text-ink-muted text-sm">
                    No employees found.
                  </td>
                </tr>
              ) : (
                filtered.map((emp, i) => (
                  <tr
                    key={emp.id}
                    className="border-b border-surface-3 last:border-0 hover:bg-surface-3/40 transition-colors group"
                  >
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-brand/30 to-accent-violet/30 flex items-center justify-center text-xs font-semibold text-ink-bright flex-shrink-0">
                          {emp.firstName.charAt(0)}{emp.lastName.charAt(0)}
                        </div>
                        <div>
                          <div className="text-sm font-medium text-ink-bright">{emp.firstName} {emp.lastName}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-sm text-ink-dim font-mono text-xs">{emp.email}</td>
                    <td className="px-5 py-3.5 text-sm text-ink-dim">{emp.department ?? '—'}</td>
                    <td className="px-5 py-3.5 text-sm text-ink-dim">{emp.jobTitle ?? '—'}</td>
                    <td className="px-5 py-3.5 text-sm text-ink-muted font-mono text-xs">{formatDate(emp.hireDate)}</td>
                    <td className="px-5 py-3.5">
                      <Badge status={emp.status} label={STATUS_LABELS[emp.status]} />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {!isLoading && totalPages > 1 && (
          <div className="px-5 py-4 border-t border-surface-4 flex items-center justify-between">
            <span className="text-xs text-ink-muted font-mono">
              {totalElements.toLocaleString()} employees · page {page + 1} of {totalPages}
            </span>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="p-1.5 rounded-lg text-ink-dim hover:text-ink hover:bg-surface-3 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
              >
                <ChevronLeft size={16} />
              </button>
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                const pageNum = Math.max(0, Math.min(page - 2 + i, totalPages - 5 + i))
                return (
                  <button
                    key={pageNum}
                    onClick={() => setPage(pageNum)}
                    className={cn(
                      'w-7 h-7 rounded-lg text-xs font-mono transition-all',
                      page === pageNum
                        ? 'bg-brand text-white'
                        : 'text-ink-dim hover:text-ink hover:bg-surface-3'
                    )}
                  >
                    {pageNum + 1}
                  </button>
                )
              })}
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="p-1.5 rounded-lg text-ink-dim hover:text-ink hover:bg-surface-3 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
