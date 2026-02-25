import { useQuery } from '@tanstack/react-query'
import { fetchUploadJobs } from '@/api/uploads'
import { FileUploadZone } from '@/components/upload/FileUploadZone'
import { Badge } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { Spinner } from '@/components/ui/Spinner'
import { formatDateTime, STATUS_LABELS } from '@/utils/format'
import { Upload, FileSpreadsheet, BarChart2 } from 'lucide-react'

export function UploadsPage() {
  const { data: uploads, isLoading } = useQuery({
    queryKey: ['uploads'],
    queryFn:  fetchUploadJobs,
    refetchInterval: (data) =>
      data?.some(u => u.status === 'PENDING' || u.status === 'PROCESSING') ? 3000 : false,
  })

  return (
    <div className="space-y-6">
      <div className="opacity-0-init animate-fade-up">
        <div className="flex items-center gap-3 mb-1">
          <div className="w-9 h-9 rounded-xl bg-accent-teal/10 border border-accent-teal/20 flex items-center justify-center">
            <Upload size={18} className="text-accent-teal" />
          </div>
          <h1 className="text-2xl font-display text-ink-bright">Uploads</h1>
        </div>
        <p className="text-ink-muted text-sm ml-12">Upload CSV or Excel files to bulk-import employee data.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Upload zone */}
        <div className="lg:col-span-2 opacity-0-init animate-fade-up" style={{ animationDelay: '100ms', animationFillMode: 'forwards' }}>
          <Card>
            <CardHeader>
              <h2 className="text-sm font-semibold text-ink">Upload a file</h2>
              <p className="text-xs text-ink-muted mt-0.5">CSV with headers: first_name, last_name, email, department, job_title, hire_date</p>
            </CardHeader>
            <CardBody>
              <FileUploadZone />
            </CardBody>
          </Card>
        </div>

        {/* Upload history */}
        <div className="lg:col-span-3 opacity-0-init animate-fade-up" style={{ animationDelay: '200ms', animationFillMode: 'forwards' }}>
          <Card>
            <CardHeader>
              <h2 className="text-sm font-semibold text-ink">Upload history</h2>
            </CardHeader>
            <CardBody className="p-0">
              {isLoading ? (
                <div className="py-16 flex justify-center">
                  <Spinner />
                </div>
              ) : !uploads?.length ? (
                <div className="py-12 text-center text-ink-muted text-sm">
                  No uploads yet. Upload your first file →
                </div>
              ) : (
                <div className="divide-y divide-surface-3">
                  {uploads.map(job => (
                    <div key={job.id} className="flex items-start gap-4 px-6 py-4 hover:bg-surface-3/30 transition-colors">
                      <div className="w-9 h-9 rounded-xl bg-surface-3 flex items-center justify-center flex-shrink-0 mt-0.5">
                        <FileSpreadsheet size={16} className="text-ink-muted" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap mb-1.5">
                          <p className="text-sm font-medium text-ink truncate">{job.originalFilename}</p>
                          <Badge status={job.status} label={STATUS_LABELS[job.status]} />
                        </div>
                        <p className="text-xs text-ink-muted font-mono mb-2">{formatDateTime(job.createdAt)}</p>

                        {/* Row stats */}
                        {job.totalRows != null && (
                          <div className="flex items-center gap-3">
                            <div className="flex-1 h-1.5 bg-surface-4 rounded-full overflow-hidden">
                              <div
                                className="h-full bg-gradient-to-r from-brand to-accent-teal rounded-full transition-all"
                                style={{ width: job.totalRows > 0 ? `${(job.processedRows / job.totalRows) * 100}%` : '0%' }}
                              />
                            </div>
                            <span className="text-xs font-mono text-ink-muted whitespace-nowrap">
                              {job.processedRows.toLocaleString()} / {job.totalRows.toLocaleString()}
                              {job.failedRows > 0 && (
                                <span className="text-accent-rose ml-1">· {job.failedRows} failed</span>
                              )}
                            </span>
                          </div>
                        )}
                      </div>
                      <div className="text-xs font-mono text-ink-muted flex-shrink-0 mt-0.5">
                        {job.fileType}
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
