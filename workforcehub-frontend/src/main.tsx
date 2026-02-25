import React from 'react'
import ReactDOM from 'react-dom/client'
import { AuthProvider } from 'react-oidc-context'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { activeOidcSettings } from '@/auth/oidcConfig'
import App from './App'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:        1000 * 60 * 2,  // 2 min
      retry:            1,
      refetchOnWindowFocus: false,
    },
  },
})

// Handle OIDC silent renew in an iframe
const onSigninCallback = () => {
  // Clean the URL query params after redirect
  window.history.replaceState({}, document.title, window.location.pathname)
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider {...activeOidcSettings} onSigninCallback={onSigninCallback}>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </AuthProvider>
  </React.StrictMode>
)
