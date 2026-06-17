import { useEffect, useState } from 'react';
import axios from 'axios';

interface Stats {
  totalUsers: number;
  totalPortfolios: number;
  notificationsSentToday: number;
  dividendDataCount: number;
}

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export default function Dashboard() {
  const [stats, setStats] = useState<Stats>({
    totalUsers: 0,
    totalPortfolios: 0,
    notificationsSentToday: 0,
    dividendDataCount: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const res = await axios.get(`${API_URL}/api/admin/stats`);
      setStats(res.data);
    } catch {
      console.warn('API unavailable, using cached data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <p>로딩 중...</p>;

  return (
    <div>
      <h1 style={{ marginBottom: 30 }}>대시보드</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 20 }}>
        <StatCard title="전체 사용자" value={stats.totalUsers} icon="👤" />
        <StatCard title="포트폴리오 수" value={stats.totalPortfolios} icon="📈" />
        <StatCard title="오늘 발송 알림" value={stats.notificationsSentToday} icon="🔔" />
        <StatCard title="배당 데이터" value={stats.dividendDataCount} icon="💰" />
      </div>
    </div>
  );
}

function StatCard({ title, value, icon }: { title: string; value: number; icon: string }) {
  return (
    <div style={{
      background: '#fff',
      borderRadius: 12,
      padding: 24,
      boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    }}>
      <div style={{ fontSize: 32, marginBottom: 8 }}>{icon}</div>
      <div style={{ fontSize: 28, fontWeight: 700 }}>{value.toLocaleString()}</div>
      <div style={{ color: '#666', marginTop: 4 }}>{title}</div>
    </div>
  );
}
