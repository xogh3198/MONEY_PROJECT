'use client';
import { useState, useEffect } from 'react';
import { fetchNewsByCategory, voteArticle } from '@/lib/api';
import CommentSection from '@/components/CommentSection';

type Category = 'ALL' | 'DOMESTIC' | 'OVERSEAS' | 'FOREX' | 'RATE' | 'CRYPTO';

interface NewsArticle {
  id: string;
  title: string;
  summary: string;
  sourceName: string;
  sourceUrl: string;
  category: string;
  sentiment: string;
  viewCount: number;
  commentCount: number;
  positiveVotes: number;
  negativeVotes: number;
  publishedAt: string;
}

const CATEGORIES: { value: Category; label: string; icon: string }[] = [
  { value: 'ALL', label: '전체', icon: '📋' },
  { value: 'DOMESTIC', label: '국내증시', icon: '🇰🇷' },
  { value: 'OVERSEAS', label: '해외증시', icon: '🇺🇸' },
  { value: 'FOREX', label: '환율', icon: '💱' },
  { value: 'RATE', label: '금리', icon: '📈' },
  { value: 'CRYPTO', label: '암호화폐', icon: '₿' },
];

// 백엔드 연결 불가 시 표시할 시드 데이터
const SEED_ARTICLES: NewsArticle[] = [
  { id: '1', title: '한은, 기준금리 3.0% 동결..."하반기 인하 검토"', summary: '한국은행 금융통화위원회가 기준금리를 3.0%로 동결했다. 하반기 경기 둔화 시 인하 가능성을 시사.', sourceName: '한국경제', sourceUrl: '#', category: 'RATE', sentiment: 'NEUTRAL', viewCount: 2341, commentCount: 56, positiveVotes: 23, negativeVotes: 31, publishedAt: '2026-06-17T09:30:00' },
  { id: '2', title: '삼성전자, AI 반도체 수주 급증...목표가 상향', summary: 'HBM3E 공급 확대로 2분기 영업이익 시장 예상 40% 상회 전망. 주요 증권사 목표가 잇따라 상향 조정.', sourceName: '매일경제', sourceUrl: '#', category: 'DOMESTIC', sentiment: 'POSITIVE', viewCount: 4521, commentCount: 124, positiveVotes: 89, negativeVotes: 12, publishedAt: '2026-06-17T08:15:00' },
  { id: '3', title: '원/달러 1,350원 돌파...수출기업 수혜 vs 수입물가 부담', summary: '미국 고용 호조로 달러 강세 지속. 수출기업에는 호재이나 수입 원자재 부담 가중 우려.', sourceName: '연합뉴스', sourceUrl: '#', category: 'FOREX', sentiment: 'NEGATIVE', viewCount: 1876, commentCount: 38, positiveVotes: 15, negativeVotes: 45, publishedAt: '2026-06-17T10:00:00' },
  { id: '4', title: '비트코인 10만 달러 재도전...ETF 자금 유입 지속', summary: '기관투자자의 비트코인 ETF 매수세가 지속되며 10만 달러 저항선을 재차 시험 중.', sourceName: '코인데스크', sourceUrl: '#', category: 'CRYPTO', sentiment: 'POSITIVE', viewCount: 3210, commentCount: 92, positiveVotes: 67, negativeVotes: 25, publishedAt: '2026-06-17T07:45:00' },
  { id: '5', title: '나스닥 신고가 경신...AI 빅테크 실적 기대', summary: '엔비디아, 마이크로소프트 등 AI 관련 대형주의 실적 기대감이 반영되며 나스닥이 사상 최고치를 경신.', sourceName: '서울경제', sourceUrl: '#', category: 'OVERSEAS', sentiment: 'POSITIVE', viewCount: 2890, commentCount: 67, positiveVotes: 78, negativeVotes: 8, publishedAt: '2026-06-17T06:30:00' },
];

export default function ForumPage() {
  const [selectedCategory, setSelectedCategory] = useState<Category>('ALL');
  const [articles, setArticles] = useState<NewsArticle[]>(SEED_ARTICLES);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadArticles();
  }, [selectedCategory]);

  const loadArticles = async () => {
    setLoading(true);
    try {
      const data = await fetchNewsByCategory(selectedCategory === 'ALL' ? undefined : selectedCategory);
      if (data?.content && data.content.length > 0) {
        setArticles(data.content);
      }
    } catch {
      // API 미연결 시 시드 데이터 유지
    } finally {
      setLoading(false);
    }
  };

  const handleVote = async (id: string, type: 'positive' | 'negative') => {
    try {
      await voteArticle(id, type);
    } catch { /* 무시 */ }
    setArticles(prev => prev.map(a =>
      a.id === id
        ? { ...a, [type === 'positive' ? 'positiveVotes' : 'negativeVotes']: (type === 'positive' ? a.positiveVotes : a.negativeVotes) + 1 }
        : a
    ));
  };

  const filtered = selectedCategory === 'ALL'
    ? articles
    : articles.filter(a => a.category === selectedCategory);

  return (
    <div className="max-w-4xl mx-auto">
      {/* 헤더 */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">경제 포럼</h1>
        <p className="text-text-secondary">실시간 경제 뉴스와 투자자들의 시장 전망을 확인하세요</p>
      </div>

      {/* 카테고리 필터 */}
      <div className="flex gap-2 mb-8 overflow-x-auto pb-2">
        {CATEGORIES.map(cat => (
          <button
            key={cat.value}
            onClick={() => setSelectedCategory(cat.value)}
            className={`flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-sm font-medium whitespace-nowrap transition-all duration-200 ${
              selectedCategory === cat.value
                ? 'bg-accent text-white shadow-lg shadow-accent/25'
                : 'bg-dark-card text-text-secondary border border-dark-border hover:border-accent/30 hover:text-text-primary'
            }`}
          >
            <span>{cat.icon}</span>
            {cat.label}
          </button>
        ))}
      </div>

      {/* 로딩 */}
      {loading && (
        <div className="text-center py-12 text-text-secondary">
          <div className="animate-pulse">뉴스를 불러오는 중...</div>
        </div>
      )}

      {/* 뉴스 목록 */}
      <div className="space-y-4">
        {filtered.map((article, idx) => (
          <ArticleCard
            key={article.id}
            article={article}
            rank={idx + 1}
            expanded={expandedId === article.id}
            onToggle={() => setExpandedId(expandedId === article.id ? null : article.id)}
            onVote={handleVote}
          />
        ))}
      </div>

      {filtered.length === 0 && !loading && (
        <div className="text-center py-16 text-text-secondary">
          해당 카테고리의 뉴스가 없습니다
        </div>
      )}

      <p className="text-center text-xs text-text-secondary/50 mt-12">
        ※ 본 정보는 투자 조언이 아니며, 투자 판단의 책임은 사용자에게 있습니다.
      </p>
    </div>
  );
}

function ArticleCard({ article, rank, expanded, onToggle, onVote }: {
  article: NewsArticle; rank: number; expanded: boolean;
  onToggle: () => void; onVote: (id: string, type: 'positive' | 'negative') => void;
}) {
  const sentimentConfig: Record<string, { icon: string; color: string; label: string }> = {
    POSITIVE: { icon: '📈', color: 'text-positive', label: '긍정' },
    NEGATIVE: { icon: '📉', color: 'text-negative', label: '부정' },
    NEUTRAL: { icon: '➡️', color: 'text-text-secondary', label: '중립' },
  };
  const sent = sentimentConfig[article.sentiment] || sentimentConfig.NEUTRAL;
  const totalVotes = article.positiveVotes + article.negativeVotes;
  const positiveRatio = totalVotes > 0 ? Math.round((article.positiveVotes / totalVotes) * 100) : 50;

  const timeAgo = getTimeAgo(article.publishedAt);

  return (
    <div className="bg-dark-card rounded-2xl border border-dark-border overflow-hidden card-hover">
      <div className="p-5 cursor-pointer" onClick={onToggle}>
        {/* 상단 메타 */}
        <div className="flex items-center gap-2 mb-3">
          <span className="flex-shrink-0 w-7 h-7 rounded-lg bg-accent/10 text-accent flex items-center justify-center text-xs font-bold">
            {rank}
          </span>
          <span className="text-xs px-2.5 py-1 rounded-md bg-dark-secondary text-text-secondary font-medium">
            {CATEGORIES.find(c => c.value === article.category)?.icon} {CATEGORIES.find(c => c.value === article.category)?.label || article.category}
          </span>
          <span className={`text-xs ${sent.color}`}>{sent.icon} {sent.label}</span>
          <span className="text-xs text-text-secondary/60 ml-auto">{article.sourceName} · {timeAgo}</span>
        </div>

        {/* 제목 */}
        <h3 className="text-lg font-semibold text-text-primary mb-2 leading-tight">{article.title}</h3>

        {/* 요약 */}
        <p className="text-sm text-text-secondary leading-relaxed mb-4">{article.summary}</p>

        {/* 하단: 투표 + 통계 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={(e) => { e.stopPropagation(); onVote(article.id, 'positive'); }}
              className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-positive/10 text-positive text-xs font-medium hover:bg-positive/20 transition-colors"
            >
              👍 {article.positiveVotes}
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onVote(article.id, 'negative'); }}
              className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-negative/10 text-negative text-xs font-medium hover:bg-negative/20 transition-colors"
            >
              👎 {article.negativeVotes}
            </button>
            {/* 비율 바 */}
            <div className="flex items-center gap-2">
              <div className="w-20 h-1.5 bg-dark-secondary rounded-full overflow-hidden">
                <div className="h-full bg-gradient-to-r from-positive to-positive/70 rounded-full transition-all" style={{ width: `${positiveRatio}%` }} />
              </div>
              <span className="text-xs text-text-secondary">{positiveRatio}%</span>
            </div>
          </div>
          <div className="flex gap-4 text-xs text-text-secondary/60">
            <span>💬 {article.commentCount}</span>
            <span>👁 {article.viewCount.toLocaleString()}</span>
          </div>
        </div>
      </div>

      {/* 댓글 영역 (펼침) */}
      {expanded && (
        <div className="border-t border-dark-border px-5 pb-5">
          <CommentSection articleId={article.id} />
        </div>
      )}
    </div>
  );
}

function getTimeAgo(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = Math.floor((now.getTime() - date.getTime()) / 60000);
  if (diff < 1) return '방금';
  if (diff < 60) return `${diff}분 전`;
  if (diff < 1440) return `${Math.floor(diff / 60)}시간 전`;
  return `${Math.floor(diff / 1440)}일 전`;
}
