import { useEffect, useState } from 'react';
import axios from 'axios';

interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  url: string;
}

export default function Health() {
  const [services, setServices] = useState<ServiceHealth[]>([
    { name: 'Dividend Engine', status: 'UNKNOWN', url: 'http://localhost:8080/actuator/health' },
    { name: 'Webhook Gateway', status: 'UNKNOWN', url: 'http://localhost:8081/actuator/health' },
  ]);

  useEffect(() => {
    checkHealth();
  }, []);

  const checkHealth = async () => {
    const updated = await Promise.all(
      services.map(async (svc) => {
        try {
          const res = await axios.get(svc.url, { timeout: 3000 });
          return { ...svc, status: res.data.status === 'UP' ? 'UP' as const : 'DOWN' as const };
        } catch {
          return { ...svc, status: 'DOWN' as const };
        }
      })
    );
    setServices(updated);
  };

  return (
    <div>
      <h1 style={{ marginBottom: 30 }}>시스템 상태</h1>
      <div style={{ display: 'grid', gap: 16 }}>
        {services.map(svc => (
          <div key={svc.name} style={{
            background: '#fff',
            padding: 20,
            borderRadius: 8,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
          }}>
            <div>
              <strong>{svc.name}</strong>
              <div style={{ color: '#888', fontSize: 13 }}>{svc.url}</div>
            </div>
            <span style={{
              padding: '6px 12px',
              borderRadius: 20,
              fontSize: 13,
              fontWeight: 600,
              background: svc.status === 'UP' ? '#d4edda' : svc.status === 'DOWN' ? '#f8d7da' : '#e2e3e5',
              color: svc.status === 'UP' ? '#155724' : svc.status === 'DOWN' ? '#721c24' : '#383d41',
            }}>
              {svc.status === 'UP' ? '✅ UP' : svc.status === 'DOWN' ? '❌ DOWN' : '⏳ CHECKING'}
            </span>
          </div>
        ))}
      </div>
      <button
        onClick={checkHealth}
        style={{ marginTop: 20, padding: '10px 20px', borderRadius: 6, border: 'none', background: '#007bff', color: '#fff', cursor: 'pointer' }}
      >
        새로고침
      </button>
    </div>
  );
}
