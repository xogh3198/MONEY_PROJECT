'use client';
import { useState } from 'react';

interface PortfolioItem {
  stockCode: string;
  stockName: string;
  quantity: number;
  dividendPerShare: number;
  exDividendDate: string;
  paymentDate: string;
}

interface MonthlyDividend {
  month: string;
  totalPreTax: number;
  totalAfterTax: number;
}

const MOCK_PORTFOLIO: PortfolioItem[] = [
  { stockCode: '005930', stockName: '삼성전자', quantity: 100, dividendPerShare: 361, exDividendDate: '2026-06-25', paymentDate: '2026-07-15' },
  { stockCode: '000660', stockName: 'SK하이닉스', quantity: 50, dividendPerShare: 1200, exDividendDate: '2026-06-25', paymentDate: '2026-07-20' },
  { stockCode: '017670', stockName: 'SK텔레콤', quantity: 30, dividendPerShare: 3500, exDividendDate: '2026-03-28', paymentDate: '2026-04-15' },
];

const MOCK_MONTHLY: MonthlyDividend[] = [
  { month: '2026-01', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-02', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-03', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-04', totalPreTax: 105000, totalAfterTax: 88830 },
  { month: '2026-05', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-06', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-07', totalPreTax: 96100, totalAfterTax: 81301 },
  { month: '2026-08', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-09', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-10', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-11', totalPreTax: 0, totalAfterTax: 0 },
  { month: '2026-12', totalPreTax: 0, totalAfterTax: 0 },
];

export default function DividendPage() {
  const [accountType, setAccountType] = useState<string>('GENERAL');

  const totalAnnual = MOCK_MONTHLY.reduce((sum, m) => sum + m.totalAfterTax, 0);

  return (
    <div>
      <h1 className="text-2xl font-bold mb-2">💰 배당금 관리</h1>
      <p className="text-gray-500 mb-6">포트폴리오 배당 캘린더 & ISA 절세 최적화</p>

      {/* 연간 요약 */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        <SummaryCard label="연간 예상 배당 (세후)" value={`${totalAnnual.toLocaleString()}원`} />
        <SummaryCard label="보유 종목" value={`${MOCK_PORTFOLIO.length}종목`} />
        <SummaryCard label="ISA 비과세 잔여" value="1,829,869원" />
        <SummaryCard label="다음 배당락일" value="06/25 (D-8)" />
      </div>

      {/* 계좌 유형 선택 */}
      <div className="flex gap-2 mb-6">
        {[
          { value: 'GENERAL', label: '일반 계좌 (15.4%)' },
          { value: 'ISA_GENERAL', label: 'ISA 일반형 (비과세 200만)' },
          { value: 'ISA_SPECIAL', label: 'ISA 서민형 (비과세 400만)' },
          { value: 'IRP', label: 'IRP (과세이연)' },
        ].map(opt => (
          <button
            key={opt.value}
            onClick={() => setAccountType(opt.value)}
            className={`px-4 py-2 rounded-lg text-sm font-medium ${
              accountType === opt.value ? 'bg-blue-600 text-white' : 'bg-white border text-gray-700'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {/* 월별 배당 캘린더 */}
      <div className="bg-white rounded-xl p-6 shadow-sm border mb-6">
        <h2 className="font-bold text-lg mb-4">📅 월별 배당 캘린더 (2026)</h2>
        <div className="grid grid-cols-6 gap-3">
          {MOCK_MONTHLY.map(m => {
            const monthNum = parseInt(m.month.split('-')[1]);
            const hasDiv = m.totalAfterTax > 0;
            return (
              <div key={m.month} className={`p-3 rounded-lg text-center ${hasDiv ? 'bg-green-50 border-green-200 border' : 'bg-gray-50'}`}>
                <div className="text-sm font-medium text-gray-600">{monthNum}월</div>
                {hasDiv ? (
                  <div className="text-sm font-bold text-positive mt-1">{m.totalAfterTax.toLocaleString()}원</div>
                ) : (
                  <div className="text-xs text-gray-400 mt-1">-</div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 포트폴리오 목록 */}
      <div className="bg-white rounded-xl p-6 shadow-sm border">
        <div className="flex justify-between items-center mb-4">
          <h2 className="font-bold text-lg">📋 나의 포트폴리오</h2>
          <button className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">
            + 종목 추가
          </button>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b-2 border-gray-100 text-sm text-gray-500">
              <th className="py-3 text-left">종목</th>
              <th className="py-3 text-right">수량</th>
              <th className="py-3 text-right">주당배당</th>
              <th className="py-3 text-right">예상배당(세전)</th>
              <th className="py-3 text-right">배당락일</th>
              <th className="py-3 text-right">지급일</th>
            </tr>
          </thead>
          <tbody>
            {MOCK_PORTFOLIO.map(item => (
              <tr key={item.stockCode} className="border-b border-gray-50 hover:bg-gray-50">
                <td className="py-3">
                  <div className="font-medium">{item.stockName}</div>
                  <div className="text-xs text-gray-400">{item.stockCode}</div>
                </td>
                <td className="py-3 text-right">{item.quantity}주</td>
                <td className="py-3 text-right">{item.dividendPerShare.toLocaleString()}원</td>
                <td className="py-3 text-right font-medium">
                  {(item.quantity * item.dividendPerShare).toLocaleString()}원
                </td>
                <td className="py-3 text-right text-sm">{item.exDividendDate}</td>
                <td className="py-3 text-right text-sm">{item.paymentDate}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="text-center text-xs text-gray-400 mt-6">
        ※ 본 정보는 투자 조언이 아니며, 투자 판단의 책임은 사용자에게 있습니다.
      </p>
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white rounded-xl p-5 shadow-sm border">
      <div className="text-xs text-gray-500 mb-1">{label}</div>
      <div className="text-lg font-bold">{value}</div>
    </div>
  );
}
