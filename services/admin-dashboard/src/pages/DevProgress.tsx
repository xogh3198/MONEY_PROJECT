import { useState } from 'react';

interface Task {
  id: string;
  name: string;
  service: string;
  status: 'done' | 'in-progress' | 'planned';
  phase: string;
  assignee: string;
  pr?: string;
  reviewResult?: 'approved' | 'changes-requested' | 'pending';
}

interface Cycle {
  id: number;
  phase: string;
  status: 'completed' | 'active' | 'planned';
  criticalIssues: number;
  result: string;
}

const CYCLES: Cycle[] = [
  { id: 1, phase: 'MVP v1 Initial', status: 'completed', criticalIssues: 3, result: 'Request Changes → 수정 후 Approve' },
  { id: 2, phase: 'MVP v1.1 Fixes', status: 'completed', criticalIssues: 0, result: '✅ Approve & Merged' },
  { id: 3, phase: 'Phase 2 - API 연동 & 대시보드', status: 'active', criticalIssues: 0, result: '진행 중...' },
];

const TASKS: Task[] = [
  // Phase 1 (완료)
  { id: 'T-001', name: '프로젝트 초기 세팅', service: 'infra', status: 'done', phase: 'Phase 1', assignee: 'Backend Dev', pr: 'main' },
  { id: 'T-002', name: '배당금 계산 로직', service: 'dividend-engine', status: 'done', phase: 'Phase 1', assignee: 'Backend Dev 1', reviewResult: 'approved' },
  { id: 'T-003', name: '포트폴리오 CRUD API', service: 'dividend-engine', status: 'done', phase: 'Phase 1', assignee: 'Backend Dev 1', reviewResult: 'approved' },
  { id: 'T-005', name: '카카오 스킬 서버 기본 구조', service: 'webhook-gateway', status: 'done', phase: 'Phase 1', assignee: 'Backend Dev 2', reviewResult: 'approved' },
  { id: 'T-009', name: 'React 대시보드 세팅', service: 'admin-dashboard', status: 'done', phase: 'Phase 1', assignee: 'Frontend Dev', reviewResult: 'approved' },
  { id: 'T-011', name: 'CI/CD GitHub Actions', service: 'infra', status: 'done', phase: 'Phase 1', assignee: 'Backend Dev' },
  // Fix Cycle
  { id: 'FIX-002', name: 'ISA 누적 배당소득 추적', service: 'dividend-engine', status: 'done', phase: 'Fix', assignee: 'Backend Dev 1', reviewResult: 'approved' },
  { id: 'FIX-004', name: '단위 테스트 8건 추가', service: 'dividend-engine', status: 'done', phase: 'Fix', assignee: 'Backend Dev 1', reviewResult: 'approved' },
  // Phase 2 (진행 중)
  { id: 'P2-001', name: 'webhook↔dividend 실제 API 연동', service: 'webhook-gateway', status: 'in-progress', phase: 'Phase 2', assignee: 'Backend Dev 2' },
  { id: 'P2-002', name: '한투 OpenAPI 실제 연동', service: 'dividend-engine', status: 'planned', phase: 'Phase 2', assignee: 'Backend Dev 1' },
  { id: 'P2-003', name: 'notification 서비스 구현', service: 'notification', status: 'planned', phase: 'Phase 2', assignee: 'Backend Dev 3' },
  { id: 'P2-004', name: '관리자 대시보드 API 연동', service: 'admin-dashboard', status: 'in-progress', phase: 'Phase 2', assignee: 'Frontend Dev' },
  { id: 'P2-005', name: '개발 진행 현황 대시보드', service: 'admin-dashboard', status: 'in-progress', phase: 'Phase 2', assignee: 'Frontend Dev' },
];

export default function DevProgress() {
  const [filter, setFilter] = useState<string>('all');

  const filteredTasks = filter === 'all' ? TASKS : TASKS.filter(t => t.status === filter);

  const stats = {
    total: TASKS.length,
    done: TASKS.filter(t => t.status === 'done').length,
    inProgress: TASKS.filter(t => t.status === 'in-progress').length,
    planned: TASKS.filter(t => t.status === 'planned').length,
  };

  return (
    <div>
      <h1 style={{ marginBottom: 10 }}>개발 진행 현황</h1>
      <p style={{ color: '#666', marginBottom: 30 }}>AI Agent 팀의 개발 사이클 & 태스크 현황</p>

      {/* 진행률 바 */}
      <div style={{ background: '#e0e0e0', borderRadius: 8, height: 24, marginBottom: 30, overflow: 'hidden' }}>
        <div style={{
          width: `${(stats.done / stats.total) * 100}%`,
          height: '100%',
          background: 'linear-gradient(90deg, #28a745, #20c997)',
          display: 'flex',
          alignItems: 'center',
          paddingLeft: 10,
          color: '#fff',
          fontWeight: 600,
          fontSize: 13,
        }}>
          {Math.round((stats.done / stats.total) * 100)}% 완료
        </div>
      </div>

      {/* 통계 카드 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 30 }}>
        <MiniCard label="전체 태스크" value={stats.total} color="#333" />
        <MiniCard label="완료" value={stats.done} color="#28a745" />
        <MiniCard label="진행 중" value={stats.inProgress} color="#007bff" />
        <MiniCard label="예정" value={stats.planned} color="#6c757d" />
      </div>

      {/* 개발 사이클 이력 */}
      <h2 style={{ marginBottom: 16 }}>🔄 개발 사이클 이력</h2>
      <div style={{ marginBottom: 30 }}>
        {CYCLES.map(cycle => (
          <div key={cycle.id} style={{
            background: '#fff',
            padding: 16,
            borderRadius: 8,
            marginBottom: 8,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderLeft: `4px solid ${cycle.status === 'completed' ? '#28a745' : cycle.status === 'active' ? '#007bff' : '#ccc'}`,
          }}>
            <div>
              <strong>Cycle {cycle.id}: {cycle.phase}</strong>
              <div style={{ fontSize: 13, color: '#666' }}>{cycle.result}</div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <StatusBadge status={cycle.status} />
              {cycle.criticalIssues > 0 && (
                <div style={{ fontSize: 12, color: '#dc3545', marginTop: 4 }}>
                  Critical: {cycle.criticalIssues}건
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* 태스크 목록 */}
      <h2 style={{ marginBottom: 16 }}>📋 태스크 목록</h2>
      <div style={{ marginBottom: 16 }}>
        {['all', 'done', 'in-progress', 'planned'].map(f => (
          <button key={f} onClick={() => setFilter(f)} style={{
            padding: '6px 12px', marginRight: 8, borderRadius: 4, border: '1px solid #ddd',
            background: filter === f ? '#007bff' : '#fff', color: filter === f ? '#fff' : '#333',
            cursor: 'pointer',
          }}>
            {f === 'all' ? '전체' : f === 'done' ? '✅ 완료' : f === 'in-progress' ? '🔄 진행중' : '📅 예정'}
          </button>
        ))}
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8 }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #eee' }}>
            <th style={{ padding: 10, textAlign: 'left' }}>ID</th>
            <th style={{ padding: 10, textAlign: 'left' }}>태스크</th>
            <th style={{ padding: 10, textAlign: 'left' }}>서비스</th>
            <th style={{ padding: 10, textAlign: 'left' }}>담당</th>
            <th style={{ padding: 10, textAlign: 'left' }}>상태</th>
            <th style={{ padding: 10, textAlign: 'left' }}>리뷰</th>
          </tr>
        </thead>
        <tbody>
          {filteredTasks.map(task => (
            <tr key={task.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
              <td style={{ padding: 10, fontFamily: 'monospace', fontSize: 13 }}>{task.id}</td>
              <td style={{ padding: 10 }}>{task.name}</td>
              <td style={{ padding: 10 }}><code style={{ background: '#f0f0f0', padding: '2px 6px', borderRadius: 3 }}>{task.service}</code></td>
              <td style={{ padding: 10, fontSize: 13 }}>{task.assignee}</td>
              <td style={{ padding: 10 }}><TaskStatus status={task.status} /></td>
              <td style={{ padding: 10 }}>{task.reviewResult ? <ReviewBadge result={task.reviewResult} /> : '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MiniCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ background: '#fff', padding: 16, borderRadius: 8, textAlign: 'center', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
      <div style={{ fontSize: 24, fontWeight: 700, color }}>{value}</div>
      <div style={{ fontSize: 13, color: '#666' }}>{label}</div>
    </div>
  );
}

function TaskStatus({ status }: { status: string }) {
  const config = {
    'done': { bg: '#d4edda', color: '#155724', label: '✅ 완료' },
    'in-progress': { bg: '#cce5ff', color: '#004085', label: '🔄 진행중' },
    'planned': { bg: '#e2e3e5', color: '#383d41', label: '📅 예정' },
  }[status] || { bg: '#eee', color: '#333', label: status };

  return <span style={{ padding: '3px 8px', borderRadius: 4, fontSize: 12, background: config.bg, color: config.color }}>{config.label}</span>;
}

function StatusBadge({ status }: { status: string }) {
  const config = {
    'completed': { bg: '#d4edda', color: '#155724', label: '완료' },
    'active': { bg: '#cce5ff', color: '#004085', label: '진행중' },
    'planned': { bg: '#e2e3e5', color: '#383d41', label: '예정' },
  }[status] || { bg: '#eee', color: '#333', label: status };

  return <span style={{ padding: '3px 8px', borderRadius: 12, fontSize: 11, background: config.bg, color: config.color }}>{config.label}</span>;
}

function ReviewBadge({ result }: { result: string }) {
  const config = {
    'approved': { bg: '#d4edda', color: '#155724', label: '✅ Approved' },
    'changes-requested': { bg: '#f8d7da', color: '#721c24', label: '❌ Changes' },
    'pending': { bg: '#fff3cd', color: '#856404', label: '⏳ Pending' },
  }[result] || { bg: '#eee', color: '#333', label: result };

  return <span style={{ padding: '2px 6px', borderRadius: 3, fontSize: 11, background: config.bg, color: config.color }}>{config.label}</span>;
}
