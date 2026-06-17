import { useEffect, useState } from 'react';

interface NotificationLog {
  id: string;
  userId: string;
  type: string;
  message: string;
  status: 'SENT' | 'FAILED' | 'PENDING';
  sentAt: string;
}

export default function Notifications() {
  const [logs, setLogs] = useState<NotificationLog[]>([]);

  useEffect(() => {
    // MVP: 더미 데이터 (API 연동 전)
    setLogs([
      { id: '1', userId: 'user-001', type: 'EX_DATE_D3', message: '삼성전자 배당락일 3일 전', status: 'SENT', sentAt: '2026-06-17 08:00' },
      { id: '2', userId: 'user-002', type: 'EX_DATE_D1', message: 'SK하이닉스 배당락일 1일 전', status: 'SENT', sentAt: '2026-06-17 08:01' },
      { id: '3', userId: 'user-003', type: 'ISA_LIMIT', message: 'ISA 비과세 한도 임박', status: 'FAILED', sentAt: '2026-06-17 08:02' },
    ]);
  }, []);

  return (
    <div>
      <h1 style={{ marginBottom: 30 }}>알림 발송 이력</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8 }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #eee' }}>
            <th style={{ padding: 12, textAlign: 'left' }}>시각</th>
            <th style={{ padding: 12, textAlign: 'left' }}>유형</th>
            <th style={{ padding: 12, textAlign: 'left' }}>메시지</th>
            <th style={{ padding: 12, textAlign: 'left' }}>상태</th>
          </tr>
        </thead>
        <tbody>
          {logs.map(log => (
            <tr key={log.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
              <td style={{ padding: 12 }}>{log.sentAt}</td>
              <td style={{ padding: 12 }}><code>{log.type}</code></td>
              <td style={{ padding: 12 }}>{log.message}</td>
              <td style={{ padding: 12 }}>
                <span style={{
                  padding: '4px 8px',
                  borderRadius: 4,
                  fontSize: 12,
                  background: log.status === 'SENT' ? '#d4edda' : log.status === 'FAILED' ? '#f8d7da' : '#fff3cd',
                  color: log.status === 'SENT' ? '#155724' : log.status === 'FAILED' ? '#721c24' : '#856404',
                }}>
                  {log.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
