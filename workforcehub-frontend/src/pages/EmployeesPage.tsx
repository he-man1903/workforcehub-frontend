import { EmployeeTable } from '@/components/employees/EmployeeTable'
import { Users } from 'lucide-react'

export function EmployeesPage() {
  return (
    <div className="space-y-6">
      <div className="opacity-0-init animate-fade-up">
        <div className="flex items-center gap-3 mb-1">
          <div className="w-9 h-9 rounded-xl bg-brand/10 border border-brand/20 flex items-center justify-center">
            <Users size={18} className="text-brand" />
          </div>
          <h1 className="text-2xl font-display text-ink-bright">Employees</h1>
        </div>
        <p className="text-ink-muted text-sm ml-12">Search, filter and browse your entire workforce.</p>
      </div>

      <div className="opacity-0-init animate-fade-up" style={{ animationDelay: '100ms', animationFillMode: 'forwards' }}>
        <EmployeeTable />
      </div>
    </div>
  )
}
