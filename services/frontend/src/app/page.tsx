import Link from 'next/link';

// 메인 페이지: 핫 이슈 요약 + 시장 지표 스냅샷
export default function HomePage() {
  return (
    <div className="space-y-8">
      {/* 시장 지표 요약 바 */}
      <section className="grid grid-cols-5 gap-4">
        <IndicatorCard name="코스피" value="2,847.52" change="+1.23%" positive />
        <IndicatorCard name="코스닥" value="892.15" change="-0.45%" positive={false} />
        <IndicatorCard name="USD/KRW" value="1,342.50" change="+0.12%" positive />
        <IndicatorCard name="S&P500" value="5,892.30" change="+0.67%" positive />
        <IndicatorCard name="BTC" value="$98,452" change="-2.10%" positive={false} />
      </section>

      {/* 핫 이슈 */}
      <section>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold">🔥 오늘의 핫 이슈</h2>
          <Link href="/forum" className="text-blue-600 text-sm hover:underline">전체 보기 →</Link>
        </div>
        <div className="space-y-3">
          <NewsCard
            title="한은, 기준금리 동결... 하반기 인하 시사"
            summary="한국은행이 기준금리를 3.0%로 동결했으나, 하반기 경기 둔화 시 인하 가능성을 시사했다."
            category="금리"
            sentiment="neutral"
            commentCount={42}
            viewCount={1523}
          />
          <NewsCard
            title="삼성전자, 2분기 실적 어닝서프라이즈 전망"
            summary="AI 반도체 수요 급증으로 삼성전자의 2분기 영업이익이 시장 예상치를 크게 상회할 것으로 전망된다."
            category="국내증시"
            sentiment="positive"
            commentCount={87}
            viewCount={3412}
          />
          <NewsCard
            title="美 연준, 9월 금리인하 기대감 후퇴"
            summary="미국 고용지표 호조로 연준의 9월 금리 인하 기대감이 약해지며 달러 강세가 지속되고 있다."
            category="해외증시"
            sentiment="negative"
            commentCount={31}
            viewCount={2198}
          />
        </div>
      </section>

      {/* 기능 소개 카드 */}
      <section className="grid grid-cols-3 gap-6">
        <FeatureCard
          icon="📰"
          title="경제 포럼"
          description="실시간 경제 뉴스와 투자자 커뮤니티 토론"
          href="/forum"
        />
        <FeatureCard
          icon="📊"
          title="시장 예측"
          description="AI 기반 환율, 주가, 금리 흐름 예측"
          href="/market"
        />
        <FeatureCard
          icon="💰"
          title="배당금 관리"
          description="포트폴리오 배당 캘린더 & ISA 절세 최적화"
          href="/dividend"
        />
      </section>
    </div>
  );
}

function IndicatorCard({ name, value, change, positive }: {
  name: string; value: string; change: string; positive: boolean;
}) {
  return (
    <div className="bg-white rounded-xl p-4 shadow-sm border">
      <div className="text-xs text-gray-500 mb-1">{name}</div>
      <div className="text-lg font-bold">{value}</div>
      <div className={`text-sm font-medium ${positive ? 'text-positive' : 'text-negative'}`}>
        {change}
      </div>
    </div>
  );
}

function NewsCard({ title, summary, category, sentiment, commentCount, viewCount }: {
  title: string; summary: string; category: string;
  sentiment: 'positive' | 'negative' | 'neutral';
  commentCount: number; viewCount: number;
}) {
  const sentimentEmoji = { positive: '🟢', negative: '🔴', neutral: '⚪' }[sentiment];
  return (
    <div className="bg-white rounded-xl p-5 shadow-sm border hover:shadow-md transition-shadow cursor-pointer">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xs px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full">{category}</span>
            <span>{sentimentEmoji}</span>
          </div>
          <h3 className="font-bold text-lg mb-1">{title}</h3>
          <p className="text-gray-600 text-sm">{summary}</p>
        </div>
      </div>
      <div className="flex gap-4 mt-3 text-xs text-gray-400">
        <span>💬 {commentCount}</span>
        <span>👁 {viewCount.toLocaleString()}</span>
      </div>
    </div>
  );
}

function FeatureCard({ icon, title, description, href }: {
  icon: string; title: string; description: string; href: string;
}) {
  return (
    <Link href={href} className="bg-white rounded-xl p-6 shadow-sm border hover:shadow-md transition-shadow">
      <div className="text-3xl mb-3">{icon}</div>
      <h3 className="font-bold text-lg mb-1">{title}</h3>
      <p className="text-gray-600 text-sm">{description}</p>
    </Link>
  );
}
