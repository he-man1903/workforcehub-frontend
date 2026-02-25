import { useQuery } from '@tanstack/react-query'
import { fetchEmployees } from '@/api/employees'
import { fetchUploadJobs } from '@/api/uploads'
import { useAuthUser } from '@/auth/useAuthUser'
import { StatCard } from '@/components/ui/StatCard'
import { Badge } from '@/components/ui/Badge'
import { Card, CardBody } from '@/components/ui/Card'
import { FileUploadZone } from '@/components/upload/FileUploadZone'
import { formatDateTime, STATUS_LABELS } from '@/utils/format'
import { Users, Upload, CheckCircle, AlertTriangle, Clock } from 'lucide-react'

export function DashboardPage() {
  const { user } = useAuthUser()

  const { data: employeePage } = useQuery({
    queryKey: ['employees', 0, 1],
    queryFn:  () => fetchEmployees({ page: 0, size: 1 }),
  })

  const { data: activeEmployees } = useQuery({
    queryKey: ['employees', 0, 1, 'ACTIVE'],
    queryFn:  () => fetchEmployees({ page: 0, size: 1, status: 'ACTIVE' }),
  })

  const { data: uploads } = useQuery({
    queryKey: ['uploads'],
    queryFn:  fetchUploadJobs,
  })

  const totalEmployees = employeePage?.totalElements ?? 0
  const totalActive    = activeEmployees?.totalElements ?? 0
  const totalUploads   = uploads?.length ?? 0
  const failedUploads  = uploads?.filter(u => u.status === 'FAILED').length ?? 0
  const recentUploads  = (uploads ?? []).slice(0, 5)

  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening'

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="opacity-0-init animate-fade-up">
        <h1 className="text-3xl font-display text-ink-bright mb-1">
          {greeting}{user?.name ? `, ${user.name.split(' ')[0]}` : ''}.
        </h1>
        <p className="text-ink-muted">
          Here's what's happening with your workforce data.
        </p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Employees" value={totalEmployees} icon={Users}    accent="brand" delay={0}   />
        <StatCard label="Active"          value={totalActive}    icon={CheckCircle} accent="teal" delay={100} />
        <StatCard label="Upload Jobs"     value={totalUploads}   icon={Upload}   accent="amber" delay={200} />
        <StatCard label="Failed Uploads"  value={failedUploads}  icon={AlertTriangle} accent="rose" delay={300} />
      </div>

      {/* Main content: 2-col */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Upload widget — 2 cols */}
        <div className="lg:col-span-2 opacity-0-init animate-fade-up" style={{ animationDelay: '200ms', animationFillMode: 'forwards' }}>
          <Card>
            <CardBody>
              <div className="flex items-center gap-2 mb-5">
                <div className="w-7 h-7 rounded-lg bg-brand/10 flex items-center justify-center">
                  <Upload size={14} className="text-brand" />
                </div>
                <h2 className="text-sm font-semibold text-ink">New Upload</h2>
              </div>
              <FileUploadZone />
            </CardBody>
          </Card>
        </div>

        {/* Recent uploads — 3 cols */}
        <div className="lg:col-span-3 opacity-0-init animate-fade-up" style={{ animationDelay: '300ms', animationFillMode: 'forwards' }}>
          <Card>
            <CardBody>
              <div className="flex items-center gap-2 mb-5">
                <div className="w-7 h-7 rounded-lg bg-surface-3 flex items-center justify-center">
                  <Clock size={14} className="text-ink-muted" />
                </div>
                <h2 className="text-sm font-semibold text-ink">Recent Uploads</h2>
              </div>

              {recentUploads.length === 0 ? (
                <div className="py-8 text-center text-sm text-ink-muted">No uploads yet.</div>
              ) : (
                <div className="space-y-2">
                  {recentUploads.map(job => (
                    <div
                      key={job.id}
                      className="flex items-center gap-3 p-3 bg-surface-3 rounded-xl hover:bg-surface-4 transition-colors"
                    >
                      <div className="w-8 h-8 rounded-lg bg-surface-4 flex items-center justify-center flex-shrink-0">
                        <Upload size={14} className="text-ink-muted" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium text-ink truncate">{job.originalFilename}</p>
                        <p className="text-xs text-ink-muted font-mono">{formatDateTime(job.createdAt)}</p>
                      </div>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        {job.processedRows > 0 && (
                          <span className="text-xs font-mono text-ink-muted">{job.processedRows.toLocaleString()} rows</span>
                        )}
                        <Badge status={job.status} label={STATUS_LABELS[job.status]} />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  )
}
