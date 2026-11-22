import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { AppLayout } from './components/layout/AppLayout';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Monitors } from './pages/Monitors';
import { AddMonitor } from './pages/AddMonitor';
import { EditMonitor } from './pages/EditMonitor';
import { MonitorDetail } from './pages/MonitorDetail';
import { Incidents } from './pages/Incidents';
import { Analytics } from './pages/Analytics';
import { Settings } from './pages/Settings';
import { Notifications } from './pages/Notifications';
import { StatusPages } from './pages/StatusPages';
import { PublicStatusPage } from './pages/PublicStatusPage';
import { Reports } from './pages/Reports';

export function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Anonymous Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/status/:slug" element={<PublicStatusPage />} />

            {/* Protected Application Layout */}
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/monitors" element={<Monitors />} />
              <Route path="/monitors/new" element={<AddMonitor />} />
              <Route path="/monitors/:id" element={<MonitorDetail />} />
              <Route path="/monitors/:id/edit" element={<EditMonitor />} />
              <Route path="/incidents" element={<Incidents />} />
              <Route path="/analytics" element={<Analytics />} />
              <Route path="/status-pages" element={<StatusPages />} />
              <Route path="/reports" element={<Reports />} />
              <Route path="/notifications" element={<Notifications />} />
              <Route path="/settings" element={<Settings />} />
            </Route>

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
