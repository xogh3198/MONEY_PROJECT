import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Notifications from './pages/Notifications';
import Health from './pages/Health';
import DevProgress from './pages/DevProgress';

function App() {
  return (
    <BrowserRouter>
      <div style={{ display: 'flex', minHeight: '100vh' }}>
        <nav style={{ width: 220, background: '#1a1a2e', color: '#fff', padding: 20 }}>
          <h2 style={{ fontSize: 18, marginBottom: 30 }}>배당금 봇 관리</h2>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            <li style={{ marginBottom: 12 }}>
              <Link to="/" style={{ color: '#e0e0e0', textDecoration: 'none' }}>📊 대시보드</Link>
            </li>
            <li style={{ marginBottom: 12 }}>
              <Link to="/dev-progress" style={{ color: '#e0e0e0', textDecoration: 'none' }}>🚀 개발 현황</Link>
            </li>
            <li style={{ marginBottom: 12 }}>
              <Link to="/notifications" style={{ color: '#e0e0e0', textDecoration: 'none' }}>🔔 알림 이력</Link>
            </li>
            <li style={{ marginBottom: 12 }}>
              <Link to="/health" style={{ color: '#e0e0e0', textDecoration: 'none' }}>💚 시스템 상태</Link>
            </li>
          </ul>
        </nav>
        <main style={{ flex: 1, padding: 30, background: '#f5f5f5' }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/dev-progress" element={<DevProgress />} />
            <Route path="/notifications" element={<Notifications />} />
            <Route path="/health" element={<Health />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
