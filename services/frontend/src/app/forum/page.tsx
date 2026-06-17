'use client';
import { useState, useEffect } from 'react';
import { fetchNewsByCategory, voteArticle } from '@/lib/api';

type Category = 'ALL' | 'DOMESTIC' | 'OVERSEAS' | 'FOREX' | 'RATE' | 'CRYPTO';
type Sentiment = 'positive' | 'negative' | 'neutral';

interface NewsArticle {
  id: string;
  title: string;
  summary: string;
  source: string;
  category: Category;
  sentiment: Sentiment;
  relatedStocks: string[];
  viewCount: number;
  commentCount: number;
  positiveVotes: number;
  negativeVotes: number;
  publishedAt: string;
}

const MOCK_ARTICLES: NewsArticle[] = [
  {
    id: '1', title: '한은, 기준금리 3.0% 동결... "하반기 인하 검토"',
    summary: '한국은행 금융통화위원회가 기준금리를 3.0%로 동결했다. 이창용 총재는 하반기 경기 둔화 시 인하를 검토할 수 있다고 밝혔다.',
    source: '한국경제', category: 'RATE', sentiment: 'neutral',
    relatedStocks: ['금융주', 'KB금융'], viewCount: 2341, commentCount: 56,
    positiveVotes: 23, negativeVotes: 31, publishedAt: '2026-06-17 09:30',
  },
  {
    id: '2', title: '삼성전자, AI 반도체 수주 급증... 목표가 상향',
    summary: '삼성전자가 엔비디아향 HBM3E 공급 확대로 2분기 영업이익이 시장 예상치를 40% 상회할 것으로 전망된다.',
    source: '매일경제', category: 'DOMESTIC', sentiment: 'positive',
    relatedStocks: ['005930', '삼성전자'], viewCount: 4521, commentCount: 124,
    positiveVotes: 89, negativeVotes: 12, publishedAt: '2026-06-17 08:15',
  },
  {
    id: '3', title: '원/달러 환율 1,350원 돌파... 수출기업 수혜',
    summary: '미국 고용지표 호조와 연준 금리인하 기대 후퇴로 원/달러 환율이 1,350원을 돌파했다.',
    source: '연합뉴스', category: 'FOREX', sentiment: 'negative',
    relatedStocks: ['수출주', '현대차'], viewCount: 1876, commentCount: 38,
    positiveVotes: 15, negativeVotes: 45, publishedAt: '2026-06-17 10:00',
  },
  {
    id: '4', title: '비트코인 10만 달러 재도전... 기관 매수세 유입',
    summary: '비트코인 ETF로의 자금 유입이 지속되며 10만 달러 저항선을 재차 시험하고 있다.',
    source: '코인데스크', category: 'CRYPTO', sentiment: 'positive',
    relatedStocks: ['BTC', 'ETH'], viewCount: 3210, commentCount: 92,
    positiveVotes: 67, negativeVotes: 25, publishedAt: '2026-06-17 07:45',
  },
];

const CATEGORIES: { value: Category; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'DOMESTIC', label: '🇰🇷 국내증시' },
  { value: 'OVERSEAS', label: '🇺🇸 해외증시' },
  { value: 'FOREX', label: '💱 환율' },
  { value: 'RATE', label: '📈 금리' },
  { value: 'CRYPTO', label: '₿ 암호화폐' },
];

export default function ForumPage() {
  const [selectedCategory, setSelectedCategory] = useState<Category>('ALL');
  const [articles, setArticles] = useState<NewsArticle[]>(MOCK_ARTICLES);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadArticles();
  }, [selectedCategory]);

  const loadArticles = async () => {
    setLoading(true);
    try {
      const data = await fetchNewsByCategory(selectedCategory === 'ALL' ? undefined : selectedCategory);
      if (data.content && data.content.length > 0) {
        setArticles(data.content);
      }
      // API 비어있으면 Mock 유지
    } catch {
      // API 연결 실패 시 Mock 데이터 유지
    } finally {
      setLoading(false);
    }
  };

  const handleVote = async (articleId: string, type: 'positive' | 'negative') => {
    try {
      await voteArticle(articleId, type);
      setArticles(prev => prev.map(a => {
        if (a.id === articleId) {
          return type === 'positive'
            ? { ...a, positiveVotes: a.positiveVotes + 1 }
            : { ...a, negativeVotes: a.negativeVotes + 1 };
        }
        return a;
      }));
    } catch {
      // 투표 실패 시 무시 (로그인 필요 등)
    }
  };

  const filtered = selectedCategory === 'ALL'
    ? articles
    : articles.filter(a => a.category === selectedCategory);

  return (
    <div>
      <h1 className="text-2xl font-bold mb-2">📰 경제 포럼</h1>
      <p className="text-gray-500 mb-6">실시간 경제 뉴스와 투자자 의견을 확인하세요</p>

      {/* 카테고리 필터 */}
      <div className="flex gap-2 mb-6 overflow-x-auto">
        {CATEGORIES.map(cat => (
          <button
            key={cat.value}
            onClick={() => setSelectedCategory(cat.value)}
            className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
              selectedCategory === cat.value
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-700 border hover:bg-gray-50'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* 뉴스 목록 */}
      <div className="space-y-4">
        {filtered.map(article => (
          <ArticleCard key={article.id} article={article} />
        ))}
      </div>

      <p className="text-center text-xs text-gray-400 mt-8">
        ※ 본 정보는 투자 조언이 아니며, 투자 판단의 책임은 사용자에게 있습니다.
      </p>
    </div>
  );
}

function ArticleCard({ article }: { article: NewsArticle }) {
  const sentimentConfig = {
    positive: { emoji: '🟢', label: '긍정', color: 'text-positive' },
    negative: { emoji: '🔴', label: '부정', color: 'text-negative' },
    neutral: { emoji: '⚪', label: '중립', color: 'text-neutral' },
  }[article.sentiment];

  const totalVotes = article.positiveVotes + article.negativeVotes;
  const positiveRatio = totalVotes > 0 ? Math.round((article.positiveVotes / totalVotes) * 100) : 50;

  return (
    <div className="bg-white rounded-xl p-5 shadow-sm border hover:shadow-md transition-shadow">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-xs px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full">
          {CATEGORIES.find(c => c.value === article.category)?.label || article.category}
        </span>
        <span className={`text-xs ${sentimentConfig.color}`}>
          {sentimentConfig.emoji} {sentimentConfig.label}
        </span>
        <span className="text-xs text-gray-400 ml-auto">{article.source} · {article.publishedAt}</span>
      </div>

      <h3 className="font-bold text-lg mb-2 hover:text-blue-600 cursor-pointer">{article.title}</h3>
      <p className="text-gray-600 text-sm mb-3">{article.summary}</p>

      {/* 관련 종목 태그 */}
      <div className="flex gap-1 mb-3">
        {article.relatedStocks.map(stock => (
          <span key={stock} className="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded">
            {stock}
          </span>
        ))}
      </div>

      {/* 투표 바 + 통계 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button className="flex items-center gap-1 text-sm text-gray-500 hover:text-positive">
            👍 {article.positiveVotes}
          </button>
          <button className="flex items-center gap-1 text-sm text-gray-500 hover:text-negative">
            👎 {article.negativeVotes}
          </button>
          {/* 투표 비율 바 */}
          <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
            <div className="h-full bg-positive rounded-full" style={{ width: `${positiveRatio}%` }} />
          </div>
          <span className="text-xs text-gray-400">{positiveRatio}% 긍정</span>
        </div>
        <div className="flex gap-3 text-xs text-gray-400">
          <span>💬 {article.commentCount}</span>
          <span>👁 {article.viewCount.toLocaleString()}</span>
        </div>
      </div>
    </div>
  );
}
