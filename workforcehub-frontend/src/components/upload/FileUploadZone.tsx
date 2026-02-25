import { useState, useRef, useCallback } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { uploadEmployeeFile } from '@/api/uploads'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/utils/cn'
import { Upload, FileSpreadsheet, X, CheckCircle, AlertCircle, CloudUpload } from 'lucide-react'
import toast from 'react-hot-toast'
import type { UploadJob } from '@/types'

export function FileUploadZone() {
  const [dragging, setDragging]   = useState(false)
  const [file, setFile]           = useState<File | null>(null)
  const [progress, setProgress]   = useState(0)
  const [result, setResult]       = useState<UploadJob | null>(null)
  const inputRef                  = useRef<HTMLInputElement>(null)
  const queryClient               = useQueryClient()

  const mutation = useMutation({
    mutationFn: (f: File) => uploadEmployeeFile(f, undefined, setProgress),
    onSuccess: (data) => {
      setResult(data)
      queryClient.invalidateQueries({ queryKey: ['uploads'] })
      toast.success(`Upload accepted — Job ID: ${data.id.slice(0, 8)}…`)
    },
    onError: () => {
      toast.error('Upload failed. Please check the file format and try again.')
    },
  })

  const accept = (f: File) => {
    const ok = f.name.endsWith('.csv') || f.name.endsWith('.xlsx') || f.name.endsWith('.xls')
    if (!ok) { toast.error('Only CSV and Excel files are supported.'); return }
    setFile(f)
    setResult(null)
    setProgress(0)
  }

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const f = e.dataTransfer.files[0]
    if (f) accept(f)
  }, [])

  const reset = () => { setFile(null); setResult(null); setProgress(0) }

  const fileSizeMB = file ? (file.size / 1024 / 1024).toFixed(2) : '0'

  return (
    <div className="space-y-4">
      {/* Drop zone */}
      {!file ? (
        <div
          onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          onClick={() => inputRef.current?.click()}
          className={cn(
            'relative border-2 border-dashed rounded-2xl p-12 flex flex-col items-center gap-4',
            'cursor-pointer transition-all duration-200 group',
            dragging
              ? 'border-brand bg-brand/10 shadow-glow-brand'
              : 'border-surface-5 hover:border-brand/50 hover:bg-brand/5'
          )}
        >
          <input
            ref={inputRef}
            type="file"
            accept=".csv,.xlsx,.xls"
            className="hidden"
            onChange={e => { const f = e.target.files?.[0]; if (f) accept(f) }}
          />

          <div className={cn(
            'w-16 h-16 rounded-2xl flex items-center justify-center transition-all duration-200',
            dragging ? 'bg-brand/20 text-brand-bright' : 'bg-surface-3 text-ink-muted group-hover:bg-brand/10 group-hover:text-brand'
          )}>
            <CloudUpload size={28} />
          </div>

          <div className="text-center">
            <p className="text-sm font-medium text-ink mb-1">
              {dragging ? 'Drop your file here' : 'Drop CSV or Excel file here'}
            </p>
            <p className="text-xs text-ink-muted">
              or <span className="text-brand hover:text-brand-bright">click to browse</span> · max 50 MB
            </p>
          </div>

          <div className="flex gap-2">
            {['.csv', '.xlsx', '.xls'].map(ext => (
              <span key={ext} className="px-2 py-1 bg-surface-3 rounded-lg text-xs font-mono text-ink-muted">{ext}</span>
            ))}
          </div>
        </div>
      ) : (
        /* Selected file */
        <div className="bg-surface-2 border border-surface-4 rounded-2xl p-5">
          <div className="flex items-start gap-4">
            <div className="w-12 h-12 bg-brand/10 rounded-xl flex items-center justify-center flex-shrink-0">
              <FileSpreadsheet size={22} className="text-brand" />
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-0.5">
                <p className="text-sm font-medium text-ink truncate">{file.name}</p>
                {!mutation.isPending && (
                  <button onClick={reset} className="text-ink-muted hover:text-accent-rose transition-colors flex-shrink-0">
                    <X size={15} />
                  </button>
                )}
              </div>
              <p className="text-xs text-ink-muted font-mono">{fileSizeMB} MB</p>

              {/* Progress bar */}
              {mutation.isPending && (
                <div className="mt-3">
                  <div className="flex justify-between text-xs text-ink-muted mb-1.5">
                    <span>Uploading…</span>
                    <span className="font-mono">{progress}%</span>
                  </div>
                  <div className="h-1.5 bg-surface-4 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-brand to-accent-teal rounded-full transition-all duration-300"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              )}

              {/* Result */}
              {result && (
                <div className="mt-3 flex items-center gap-2">
                  {result.status === 'FAILED' ? (
                    <AlertCircle size={15} className="text-accent-rose" />
                  ) : (
                    <CheckCircle size={15} className="text-accent-teal" />
                  )}
                  <span className="text-xs text-ink-dim font-mono">{result.id}</span>
                  <Badge status={result.status} />
                </div>
              )}
            </div>
          </div>

          {!mutation.isPending && !result && (
            <div className="mt-4 flex gap-2">
              <Button
                variant="primary"
                size="sm"
                onClick={() => mutation.mutate(file)}
                className="flex-1"
              >
                <Upload size={14} />
                Upload File
              </Button>
              <Button variant="ghost" size="sm" onClick={reset}>Cancel</Button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
