import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import { useTokenSync } from '@/hooks/useTokenSync'
import { AppLayout } from '@/components/layout/AppLayout'
import { FullPageSpinner } from '@/components/ui/Spinner'
import { LoginPage } from '@/pages/LoginPage'
import { AuthCallbackPage } from '@/pages/AuthCallbackPage'
import { BackendCallbackPage } from '@/pages/BackendCallbackPage'
import { getBackendAccessToken } from '@/auth/backendAuth'
import { DashboardPage } from '@/pages/DashboardPage'
import { EmployeesPage } from '@/pages/EmployeesPage'
import { UploadsPage } from '@/pages/UploadsPage'
import { NotFoundPage } from '@/pages/NotFoundPage'

// Syncs token into Axios + handles 401 silent renew
function TokenSyncProvider() {
  useTokenSync()
  return <Outlet />
}

function RequireAuth() {
  const auth = useAuth()
  const hasBackendToken = getBackendAccessToken()
  if (hasBackendToken) return <Outlet />
  if (auth.isLoading) return <FullPageSpinner />
  if (!auth.isAuthenticated) return <Navigate to="/login" replace />
  return <Outlet />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route path="/auth/silent" element={<AuthCallbackPage />} />
        <Route path="/auth/backend-callback" element={<BackendCallbackPage />} />

        <Route element={<RequireAuth />}>
          <Route element={<TokenSyncProvider />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/employees" element={<EmployeesPage />} />
              <Route path="/uploads" element={<UploadsPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}
