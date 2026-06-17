import Link from 'next/link';

export default function HomePage() {
  return (
    <div className="space-y-10">
      {/* 히어로 섹션 */}
      <section className="text-center py-12">
        <h1 className="text-4xl sm:text-5xl font-bold mb-4">
          투자자를 위한<br />
          <span className="gradient-text">올인원 경제 플랫폼</span>
        </h1>
        <p className="text-text-secondary text-lg max-w-2xl mx-auto mb-8">
          실시간 경제 뉴스, AI 시장 예측, 배당금 최적화까지.<br />
          모든 투자 정보를 한 곳에서 확인하세요.
        </p>
        <div className="flex gap-3 justify-center">
          <Link href="/forum" className="px-6 py-3 bg-accent text-white font-medium rounded-xl hover:bg-accent/90 transition-colors">
            포럼 둘러보기
          </Link>
          <Link href="/market" className="px-6 py-3 bg-dark-card text-text-primary font-medium rounded-xl border border-dark-border hover:border-accent/50 transition-colors">
            시장 현황 보기
          </Link>
        </div>
      </section>

      {/* 실시간 티커 바 */}
      <section className="flex gap-3 overflow-x-auto pb-2 scrollbar-hide">
        <TickerCard name="코스피" value="2,847.52" change="+1.23%" positive />
        <TickerCard name="코스닥" value="892.15" change="-0.45%" positive={false} />
        <TickerCard name="USD/KRW" value="1,342.50" change="+0.12%" positive />
        <TickerCard name="S&P 500" value="5,892.30" change="+0.67%" positive />
        <TickerCard name="BTC" value="$98,452" change="-2.10%" positive={false} />
        <TickerCard name="금(Gold)" value="$2,380" change="+0.34%" positive />
      </section>

      {/* 핫 이슈 */}
      <section>
        <div className="flex justify-between items-center mb-5">
          <h2 className="text-xl font-bold">🔥 핫 이슈</h2>
          <Link href="/forum" className="text-accent text-sm hover:underline">전체 보기 →</Link>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <HotNewsCard
            rank={1}
            title="한은, 기준금리 동결... 하반기 인하 시사"
            category="금리"
            sentiment="neutral"
            comments={42}
            positiveRatio={43}
          />
          <HotNewsCard
            rank={2}
            title="삼성전자, 2분기 어닝서프라이즈 전망"
            category="국내증시"
            sentiment="positive"
            comments={87}
            positiveRatio={88}
          />
          <HotNewsCard
            rank={3}
            title="美 연준, 9월 금리인하 기대감 후퇴"
            category="해외증시"
            sentiment="negative"
            comments={31}
            positiveRatio={25}
          />
          <HotNewsCard
            rank={4}
            title="비트코인 10만 달러 재도전, 기관 매수세"
            category="암호화폐"
            sentiment="positive"
            comments={92}
            positiveRatio={72}
          />
        </div>
      </section>

      {/* 기능 소개 */}
      <section className="grid md:grid-cols-3 gap-6">
        <FeatureCard
          icon="📰"
          title="경제 포럼"
          description="실시간 뉴스 큐레이션과 투자자 커뮤니티. 시장 심리를 한눈에."
          href="/forum"
          gradient="from-blue-500/20 to-cyan-500/20"
        />
        <FeatureCard
          icon="📊"
          title="시장 예측"
          description="AI 기반 환율·주가·금리 흐름 분석. 데이터로 보는 시장 방향."
          href="/market"
          gradient="from-purple-500/20 to-pink-500/20"
        />
        <FeatureCard
          icon="💰"
          title="배당 관리"
          description="포트폴리오 배당 캘린더와 ISA 절세 최적화. 놓치는 배당 없이."
          href="/dividend"
          gradient="from-green-500/20 to-emerald-500/20"
        />
      </section>
    </div>
  );
}

function TickerCard({ name, value, change, positive }: {
  name: string; value: string; change: string; positive: boolean;
}) {
  return (
    <div className="flex-shrink-0 bg-dark-card rounded-xl px-5 py-3 border border-dark-border min-w-[140px]">
      <div className="text-xs text-text-secondary mb-0.5">{name}</div>
      <div className="text-base font-bold text-text-primary">{value}</div>
      <div className={`text-sm font-semibold ${positive ? 'text-positive' : 'text-negative'}`}>
        {change}
      </div>
    </div>
  );
}

function HotNewsCard({ rank, title, category, sentiment, comments, positiveRatio }: {
  rank: number; title: string; category: string;
  sentiment: 'positive' | 'negative' | 'neutral';
  comments: number; positiveRatio: number;
}) {
  const sentimentColor = { positive: 'text-positive', negative: 'text-negative', neutral: 'text-text-secondary' }[sentiment];
  const sentimentIcon = { positive: '📈', negative: '📉', neutral: '➡️' }[sentiment];

  return (
    <div className="bg-dark-card rounded-2xl p-5 border border-dark-border card-hover cursor-pointer">
      <div className="flex items-start gap-3">
        <span className="flex-shrink-0 w-8 h-8 rounded-full bg-accent/15 text-accent flex items-center justify-center text-sm font-bold">
          {rank}
        </span>
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-text-primary mb-2 line-clamp-2">{title}</h3>
          <div className="flex items-center gap-3 text-xs">
            <span className="px-2 py-0.5 rounded bg-dark-secondary text-text-secondary">{category}</span>
            <span className={sentimentColor}>{sentimentIcon}</span>
            <span className="text-text-secondary">💬 {comments}</span>
            <div className="flex items-center gap-1">
              <div className="w-16 h-1.5 bg-dark-secondary rounded-full overflow-hidden">
                <div className="h-full bg-positive rounded-full" style={{ width: `${positiveRatio}%` }} />
              </div>
              <span className="text-text-secondary">{positiveRatio}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function FeatureCard({ icon, title, description, href, gradient }: {
  icon: string; title: string; description: string; href: string; gradient: string;
}) {
  return (
    <Link href={href} className={`bg-gradient-to-br ${gradient} rounded-2xl p-6 border border-dark-border card-hover block`}>
      <div className="text-3xl mb-4">{icon}</div>
      <h3 className="text-lg font-bold text-text-primary mb-2">{title}</h3>
      <p className="text-sm text-text-secondary leading-relaxed">{description}</p>
    </Link>
  );
}
