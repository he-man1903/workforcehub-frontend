import client from './axiosClient'
import type { UploadJob } from '@/types'

export async function uploadEmployeeFile(
  file: File,
  description?: string,
  onProgress?: (pct: number) => void
): Promise<UploadJob> {
  const form = new FormData()
  form.append('file', file)
  if (description) form.append('description', description)

  const { data } = await client.post<UploadJob>('/api/v1/uploads', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (e.total) onProgress?.(Math.round((e.loaded / e.total) * 100))
    },
  })
  return data
}

export async function fetchUploadJob(id: string): Promise<UploadJob> {
  const { data } = await client.get<UploadJob>(`/api/v1/uploads/${id}`)
  return data
}

export async function fetchUploadJobs(): Promise<UploadJob[]> {
  const { data } = await client.get<{ content: UploadJob[] }>('/api/v1/uploads')
  return data.content
}
