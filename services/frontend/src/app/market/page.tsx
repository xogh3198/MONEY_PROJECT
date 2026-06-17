'use client';
import { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts';

type IndicatorType = 'KOSPI' | 'KOSDAQ' | 'USD_KRW' | 'SP500' | 'BTC';

interface MarketData {
  date: string;
  value: number;
}

interface Indicator {
  type: IndicatorType;
  name: string;
  current: number;
  change: number;
  changePercent: number;
  prediction: 'UP' | 'DOWN' | 'NEUTRAL';
  predictionReason: string;
  data: MarketData[];
}

const MOCK_INDICATORS: Indicator[] = [
  {
    type: 'KOSPI', name: '코스피', current: 2847.52, change: 34.67, changePercent: 1.23,
    prediction: 'UP', predictionReason: '삼성전자 실적 기대감 + 외국인 순매수 지속',
    data: generateMockData(2800, 2900, 30),
  },
  {
    type: 'KOSDAQ', name: '코스닥', current: 892.15, change: -4.02, changePercent: -0.45,
    prediction: 'NEUTRAL', predictionReason: '바이오 섹터 혼조세, 2차전지 관망',
    data: generateMockData(880, 910, 30),
  },
  {
    type: 'USD_KRW', name: '원/달러 환율', current: 1342.50, change: 1.60, changePercent: 0.12,
    prediction: 'UP', predictionReason: '미국 금리인하 지연 + 달러 강세 지속',
    data: generateMockData(1320, 1360, 30),
  },
  {
    type: 'SP500', name: 'S&P 500', current: 5892.30, change: 39.27, changePercent: 0.67,
    prediction: 'UP', predictionReason: 'AI 기업 실적 호조 + 고용 안정',
    data: generateMockData(5700, 5950, 30),
  },
  {
    type: 'BTC', name: '비트코인', current: 98452, change: -2103, changePercent: -2.10,
    prediction: 'NEUTRAL', predictionReason: '10만 달러 저항 부근 횡보, ETF 자금 유입 지속',
    data: generateMockData(92000, 102000, 30),
  },
];

function generateMockData(min: number, max: number, days: number): MarketData[] {
  const data: MarketData[] = [];
  let value = (min + max) / 2;
  for (let i = days; i >= 0; i--) {
    const date = new Date();
    date.setDate(date.getDate() - i);
    value += (Math.random() - 0.48) * (max - min) * 0.05;
    value = Math.max(min, Math.min(max, value));
    data.push({ date: `${date.getMonth() + 1}/${date.getDate()}`, value: Math.round(value * 100) / 100 });
  }
  return data;
}

export default function MarketPage() {
  const [selected, setSelected] = useState<IndicatorType>('KOSPI');
  const selectedIndicator = MOCK_INDICATORS.find(i => i.type === selected)!;

  return (
    <div>
      <h1 className="text-2xl font-bold mb-2">📊 시장 지표 & 예측</h1>
      <p className="text-gray-500 mb-6">AI 기반 시장 분석과 예상 흐름을 확인하세요</p>

      {/* 지표 카드 목록 */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-8">
        {MOCK_INDICATORS.map(ind => (
          <button
            key={ind.type}
            onClick={() => setSelected(ind.type)}
            className={`p-4 rounded-2xl text-left transition-all duration-200 ${
              selected === ind.type
                ? 'bg-accent text-white shadow-lg shadow-accent/25 scale-[1.02]'
                : 'bg-dark-card border border-dark-border hover:border-accent/30'
            }`}
          >
            <div className={`text-xs mb-1 ${selected === ind.type ? 'text-blue-100' : 'text-text-secondary'}`}>
              {ind.name}
            </div>
            <div className="text-lg font-bold">
              {ind.type === 'BTC' ? `$${ind.current.toLocaleString()}` : ind.current.toLocaleString()}
            </div>
            <div className={`text-sm font-medium ${
              ind.changePercent >= 0
                ? (selected === ind.type ? 'text-green-200' : 'text-positive')
                : (selected === ind.type ? 'text-red-200' : 'text-negative')
            }`}>
              {ind.changePercent >= 0 ? '+' : ''}{ind.changePercent}%
            </div>
          </button>
        ))}
      </div>

      {/* 메인 차트 */}
      <div className="bg-dark-card rounded-2xl p-6 border border-dark-border mb-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold">{selectedIndicator.name} 추이 (30일)</h2>
          <PredictionBadge prediction={selectedIndicator.prediction} />
        </div>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={selectedIndicator.data}>
            <defs>
              <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis dataKey="date" fontSize={12} />
            <YAxis fontSize={12} domain={['auto', 'auto']} />
            <Tooltip />
            <Area type="monotone" dataKey="value" stroke="#3b82f6" fill="url(#colorValue)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      {/* AI 예측 분석 */}
      <div className="bg-dark-card rounded-2xl p-6 border border-dark-border">
        <h3 className="font-bold text-lg mb-3">🤖 AI 예측 분석</h3>
        <div className="flex items-start gap-4">
          <PredictionBadge prediction={selectedIndicator.prediction} large />
          <div>
            <p className="text-gray-700 mb-2">{selectedIndicator.predictionReason}</p>
            <p className="text-xs text-gray-400">
              ※ AI 분석 결과이며 투자 조언이 아닙니다. 투자 판단의 책임은 사용자에게 있습니다.
            </p>
          </div>
        </div>
      </div>

      {/* 주요 이벤트 캘린더 */}
      <div className="bg-dark-card rounded-2xl p-6 border border-dark-border mt-6">
        <h3 className="font-bold text-lg mb-3">📅 주요 경제 이벤트</h3>
        <div className="space-y-2">
          <EventItem date="06/18" event="한국 금통위 의사록 공개" impact="금리" />
          <EventItem date="06/19" event="미국 주간 실업수당 청구건수" impact="환율" />
          <EventItem date="06/25" event="삼성전자 배당락일" impact="국내증시" />
          <EventItem date="06/26" event="미국 PCE 물가지수 발표" impact="글로벌" />
        </div>
      </div>
    </div>
  );
}

function PredictionBadge({ prediction, large }: { prediction: string; large?: boolean }) {
  const config = {
    UP: { emoji: '📈', label: '상승 예상', bg: 'bg-green-100', text: 'text-green-700' },
    DOWN: { emoji: '📉', label: '하락 예상', bg: 'bg-red-100', text: 'text-red-700' },
    NEUTRAL: { emoji: '➡️', label: '보합 예상', bg: 'bg-gray-100', text: 'text-gray-700' },
  }[prediction] || { emoji: '❓', label: '분석 중', bg: 'bg-gray-100', text: 'text-gray-500' };

  return (
    <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full ${config.bg} ${config.text} ${large ? 'text-base' : 'text-sm'} font-medium`}>
      {config.emoji} {config.label}
    </span>
  );
}

function EventItem({ date, event, impact }: { date: string; event: string; impact: string }) {
  return (
    <div className="flex items-center gap-3 py-2 border-b border-gray-100 last:border-0">
      <span className="text-sm font-mono text-gray-500 w-12">{date}</span>
      <span className="flex-1 text-sm">{event}</span>
      <span className="text-xs px-2 py-0.5 bg-blue-50 text-blue-600 rounded">{impact}</span>
    </div>
  );
}
