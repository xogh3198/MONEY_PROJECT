'use client';
import { useState, useEffect } from 'react';
import { fetchNewsByCategory, voteArticle } from '@/lib/api';
import CommentSection from '@/components/CommentSection';

type Category = 'ALL' | 'DOMESTIC' | 'OVERSEAS' | 'FOREX' | 'RATE' | 'CRYPTO';

interface NewsArticle {
  id: string; title: string; summary: string; sourceName: string; sourceUrl: string;
  category: string; sentiment: string; viewCount: number; commentCount: number;
  positiveVotes: number; negativeVotes: number; publishedAt: string;
}

const CATEGORIES: { value: Category; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'DOMESTIC', label: '국내증시' },
  { value: 'OVERSEAS', label: '해외증시' },
  { value: 'FOREX', label: '환율' },
  { value: 'RATE', label: '금리' },
  { value: 'CRYPTO', label: '암호화폐' },
];

const SEED: NewsArticle[] = [
  { id:'1', title:'한은, 기준금리 3.0% 동결..."하반기 인하 검토"', summary:'한국은행 금융통화위원회가 기준금리를 동결. 하반기 경기 둔화 시 인하 가능성 시사.', sourceName:'한국경제', sourceUrl:'#', category:'RATE', sentiment:'NEUTRAL', viewCount:2341, commentCount:56, positiveVotes:23, negativeVotes:31, publishedAt:'2026-06-17T09:30:00' },
  { id:'2', title:'삼성전자, AI 반도체 수주 급증...목표가 상향', summary:'HBM3E 공급 확대로 2분기 영업이익 시장 예상 40% 상회 전망.', sourceName:'매일경제', sourceUrl:'#', category:'DOMESTIC', sentiment:'POSITIVE', viewCount:4521, commentCount:124, positiveVotes:89, negativeVotes:12, publishedAt:'2026-06-17T08:15:00' },
  { id:'3', title:'원/달러 1,350원 돌파...수출기업 수혜 vs 수입 부담', summary:'미국 고용 호조로 달러 강세 지속. 수출 호재이나 수입 원자재 부담 우려.', sourceName:'연합뉴스', sourceUrl:'#', category:'FOREX', sentiment:'NEGATIVE', viewCount:1876, commentCount:38, positiveVotes:15, negativeVotes:45, publishedAt:'2026-06-17T10:00:00' },
  { id:'4', title:'비트코인 10만 달러 재도전...기관 매수세 유입', summary:'기관투자자 비트코인 ETF 매수 지속, 10만달러 저항선 시험 중.', sourceName:'코인데스크', sourceUrl:'#', category:'CRYPTO', sentiment:'POSITIVE', viewCount:3210, commentCount:92, positiveVotes:67, negativeVotes:25, publishedAt:'2026-06-17T07:45:00' },
  { id:'5', title:'나스닥 신고가...AI 빅테크 실적 기대', summary:'엔비디아, MS 등 AI 대형주 실적 기대감 반영. 나스닥 사상 최고치.', sourceName:'서울경제', sourceUrl:'#', category:'OVERSEAS', sentiment:'POSITIVE', viewCount:2890, commentCount:67, positiveVotes:78, negativeVotes:8, publishedAt:'2026-06-17T06:30:00' },
];

export default function ForumPage() {
  const [category, setCategory] = useState<Category>('ALL');
  const [articles, setArticles] = useState<NewsArticle[]>(SEED);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const data = await fetchNewsByCategory(category === 'ALL' ? undefined : category);
        if (data?.content?.length > 0) setArticles(data.content);
      } catch { /* 시드 유지 */ }
    })();
  }, [category]);

  const handleVote = async (id: string, type: 'positive' | 'negative') => {
    try { await voteArticle(id, type); } catch {}
    setArticles(prev => prev.map(a => a.id === id
      ? { ...a, [type === 'positive' ? 'positiveVotes' : 'negativeVotes']: (type === 'positive' ? a.positiveVotes : a.negativeVotes) + 1 }
      : a));
  };

  const filtered = category === 'ALL' ? articles : articles.filter(a => a.category === category);

  return (
    <div>
      {/* 헤더 */}
      <div className="mb-5">
        <h1 className="text-xl font-bold text-text-main">경제뉴스</h1>
      </div>

      {/* 카테고리 탭 */}
      <div className="flex gap-1 mb-5 border-b border-border">
        {CATEGORIES.map(c => (
          <button key={c.value} onClick={() => setCategory(c.value)}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              category === c.value ? 'border-primary text-primary' : 'border-transparent text-text-sub hover:text-text-main'
            }`}>
            {c.label}
          </button>
        ))}
      </div>

      {/* 뉴스 목록 */}
      <div className="bg-white rounded-lg border border-border divide-y divide-border">
        {filtered.map(article => (
          <div key={article.id}>
            <div className="p-5 hover:bg-page-bg transition-colors cursor-pointer" onClick={() => setExpandedId(expandedId === article.id ? null : article.id)}>
              {/* 메타 */}
              <div className="flex items-center gap-2 mb-2 text-xs">
                <span className="text-text-light">{article.sourceName}</span>
                <span className="text-border">·</span>
                <span className="text-text-light">{timeAgo(article.publishedAt)}</span>
                <span className={`ml-auto px-2 py-0.5 rounded text-xs font-medium ${
                  article.sentiment === 'POSITIVE' ? 'bg-red-50 text-positive' :
                  article.sentiment === 'NEGATIVE' ? 'bg-blue-50 text-negative' :
                  'bg-gray-50 text-text-light'
                }`}>
                  {article.sentiment === 'POSITIVE' ? '▲ 긍정' : article.sentiment === 'NEGATIVE' ? '▼ 부정' : '— 중립'}
                </span>
              </div>

              {/* 제목 + 요약 */}
              <h3 className="text-[15px] font-semibold text-text-main mb-1">{article.title}</h3>
              <p className="text-sm text-text-sub leading-relaxed">{article.summary}</p>

              {/* 하단 */}
              <div className="flex items-center justify-between mt-3">
                <div className="flex items-center gap-2">
                  <button onClick={e => { e.stopPropagation(); handleVote(article.id, 'positive'); }}
                    className="flex items-center gap-1 px-2.5 py-1 rounded text-xs text-positive bg-red-50 hover:bg-red-100">
                    👍 {article.positiveVotes}
                  </button>
                  <button onClick={e => { e.stopPropagation(); handleVote(article.id, 'negative'); }}
                    className="flex items-center gap-1 px-2.5 py-1 rounded text-xs text-negative bg-blue-50 hover:bg-blue-100">
                    👎 {article.negativeVotes}
                  </button>
                  <div className="w-16 h-1.5 bg-gray-100 rounded-full overflow-hidden ml-1">
                    <div className="h-full bg-positive rounded-full"
                      style={{ width: `${(article.positiveVotes + article.negativeVotes) > 0 ? Math.round(article.positiveVotes / (article.positiveVotes + article.negativeVotes) * 100) : 50}%` }} />
                  </div>
                </div>
                <div className="flex gap-3 text-xs text-text-light">
                  <span>💬 {article.commentCount}</span>
                  <span>👁 {article.viewCount.toLocaleString()}</span>
                </div>
              </div>
            </div>

            {/* 댓글 (펼침) */}
            {expandedId === article.id && (
              <div className="px-5 pb-5 border-t border-border bg-page-bg">
                <CommentSection articleId={article.id} />
              </div>
            )}
          </div>
        ))}
      </div>

      <p className="text-center text-xs text-text-light mt-8">
        ※ 본 정보는 투자 조언이 아니며, 투자 판단의 책임은 사용자에게 있습니다.
      </p>
    </div>
  );
}

function timeAgo(dateStr: string): string {
  const diff = Math.floor((Date.now() - new Date(dateStr).getTime()) / 60000);
  if (diff < 1) return '방금';
  if (diff < 60) return `${diff}분 전`;
  if (diff < 1440) return `${Math.floor(diff / 60)}시간 전`;
  return `${Math.floor(diff / 1440)}일 전`;
}
