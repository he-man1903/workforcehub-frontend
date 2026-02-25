// ── Employee ─────────────────────────────────────────────────────────────────
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING'

export interface Employee {
  id: string
  uploadJobId: string
  firstName: string
  lastName: string
  email: string
  department: string | null
  jobTitle: string | null
  hireDate: string | null
  status: EmployeeStatus
  createdAt: string
}

// ── Pagination ────────────────────────────────────────────────────────────────
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface PageParams {
  page: number
  size: number
  sort?: string
}

// ── Upload ────────────────────────────────────────────────────────────────────
export type UploadStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'PARTIAL'
export type FileType = 'CSV' | 'EXCEL'

export interface UploadJob {
  id: string
  originalFilename: string
  fileType: FileType
  status: UploadStatus
  totalRows: number | null
  processedRows: number
  failedRows: number
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

// ── Auth ──────────────────────────────────────────────────────────────────────
export interface AuthUser {
  id: string
  email: string
  name: string
  tenantId: string
  role: string
  avatarUrl?: string
}

// ── API Errors ────────────────────────────────────────────────────────────────
export interface ApiError {
  status: number
  error: string
  message: string
  path?: string
  timestamp?: string
  fieldErrors?: Record<string, string>
}
