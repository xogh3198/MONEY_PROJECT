import Link from 'next/link';

export default function HomePage() {
  return (
    <div className="space-y-6">
      {/* 시장 지표 요약 */}
      <section className="bg-white rounded-lg border border-border p-4">
        <div className="flex items-center gap-6 overflow-x-auto text-sm">
          <Ticker name="코스피" value="2,847.52" change="+34.67" percent="+1.23%" up />
          <span className="text-border">|</span>
          <Ticker name="코스닥" value="892.15" change="-4.02" percent="-0.45%" up={false} />
          <span className="text-border">|</span>
          <Ticker name="원/달러" value="1,342.50" change="+1.60" percent="+0.12%" up />
          <span className="text-border">|</span>
          <Ticker name="S&P500" value="5,892.30" change="+39.27" percent="+0.67%" up />
          <span className="text-border">|</span>
          <Ticker name="비트코인" value="$98,452" change="-2,103" percent="-2.10%" up={false} />
        </div>
      </section>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 왼쪽: 핫 뉴스 */}
        <section className="lg:col-span-2">
          <div className="bg-white rounded-lg border border-border">
            <div className="flex items-center justify-between px-5 py-3 border-b border-border">
              <h2 className="font-bold text-text-main">오늘의 경제뉴스</h2>
              <Link href="/forum" className="text-xs text-text-light hover:text-primary">더보기 &gt;</Link>
            </div>
            <ul className="divide-y divide-border">
              <NewsItem title="한은, 기준금리 3.0% 동결...하반기 인하 시사" source="한국경제" time="2시간 전" category="금리" />
              <NewsItem title="삼성전자, AI 반도체 수주 급증...목표가 상향" source="매일경제" time="3시간 전" category="국내증시" />
              <NewsItem title="원/달러 환율 1,350원 돌파...수출기업 수혜" source="연합뉴스" time="4시간 전" category="환율" />
              <NewsItem title="비트코인 10만 달러 재도전, ETF 자금 유입 지속" source="코인데스크" time="5시간 전" category="암호화폐" />
              <NewsItem title="나스닥 사상 최고치 경신...AI 빅테크 기대감" source="서울경제" time="6시간 전" category="해외증시" />
              <NewsItem title="한국 수출 5개월 연속 증가...반도체 호조" source="한국경제" time="7시간 전" category="국내증시" />
            </ul>
          </div>
        </section>

        {/* 오른쪽: 사이드바 */}
        <aside className="space-y-6">
          {/* 인기 토론 */}
          <div className="bg-white rounded-lg border border-border">
            <div className="px-5 py-3 border-b border-border">
              <h3 className="font-bold text-sm text-text-main">🔥 인기 토론</h3>
            </div>
            <ul className="p-4 space-y-3">
              <RankItem rank={1} title="하반기 금리 인하 확실한가?" comments={42} ratio={43} />
              <RankItem rank={2} title="삼성전자 10만원 가나?" comments={87} ratio={72} />
              <RankItem rank={3} title="비트코인 연말 전망" comments={56} ratio={65} />
              <RankItem rank={4} title="환율 1,400원 간다 vs 안간다" comments={31} ratio={38} />
              <RankItem rank={5} title="2분기 실적 시즌 유망주" comments={24} ratio={81} />
            </ul>
          </div>

          {/* 기능 바로가기 */}
          <div className="bg-white rounded-lg border border-border p-5 space-y-3">
            <h3 className="font-bold text-sm text-text-main mb-2">투자 도구</h3>
            <Link href="/market" className="flex items-center gap-3 p-3 rounded-md hover:bg-page-bg transition-colors">
              <span className="text-xl">📊</span>
              <div>
                <div className="text-sm font-medium">시장 예측</div>
                <div className="text-xs text-text-light">AI 기반 흐름 분석</div>
              </div>
            </Link>
            <Link href="/dividend" className="flex items-center gap-3 p-3 rounded-md hover:bg-page-bg transition-colors">
              <span className="text-xl">💰</span>
              <div>
                <div className="text-sm font-medium">배당 캘린더</div>
                <div className="text-xs text-text-light">포트폴리오 배당 관리</div>
              </div>
            </Link>
          </div>
        </aside>
      </div>
    </div>
  );
}

function Ticker({ name, value, change, percent, up }: {
  name: string; value: string; change: string; percent: string; up: boolean;
}) {
  return (
    <div className="flex items-center gap-2 whitespace-nowrap">
      <span className="text-text-sub">{name}</span>
      <span className="font-semibold">{value}</span>
      <span className={`font-medium ${up ? 'text-positive' : 'text-negative'}`}>
        {change} ({percent})
      </span>
    </div>
  );
}

function NewsItem({ title, source, time, category }: {
  title: string; source: string; time: string; category: string;
}) {
  return (
    <li className="px-5 py-3 hover:bg-page-bg transition-colors cursor-pointer">
      <div className="flex items-start justify-between gap-4">
        <p className="text-sm text-text-main font-medium leading-snug">{title}</p>
        <span className="flex-shrink-0 text-xs px-2 py-0.5 rounded bg-page-bg text-text-light">{category}</span>
      </div>
      <div className="flex gap-2 mt-1 text-xs text-text-light">
        <span>{source}</span>
        <span>·</span>
        <span>{time}</span>
      </div>
    </li>
  );
}

function RankItem({ rank, title, comments, ratio }: {
  rank: number; title: string; comments: number; ratio: number;
}) {
  return (
    <div className="flex items-center gap-3">
      <span className={`flex-shrink-0 w-5 text-center text-xs font-bold ${rank <= 3 ? 'text-primary' : 'text-text-light'}`}>
        {rank}
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-sm text-text-main truncate">{title}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="text-xs text-text-light">💬 {comments}</span>
          <div className="w-12 h-1 bg-gray-100 rounded-full overflow-hidden">
            <div className="h-full bg-primary/60 rounded-full" style={{ width: `${ratio}%` }} />
          </div>
        </div>
      </div>
    </div>
  );
}
