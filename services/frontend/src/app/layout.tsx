import './globals.css';
import type { Metadata } from 'next';
import Navigation from '@/components/Navigation';

export const metadata: Metadata = {
  title: 'MoneyForum - 경제 뉴스 & 투자 포럼',
  description: '실시간 경제 뉴스, 시장 분석, 배당금 관리. 투자자를 위한 종합 플랫폼.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        <link rel="preconnect" href="https://cdn.jsdelivr.net" />
        <link href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css" rel="stylesheet" />
      </head>
      <body>
        <Navigation />
        <main className="max-w-6xl mx-auto px-4 py-5">
          {children}
        </main>
        {/* 푸터 */}
        <footer className="border-t border-[#e4e4e4] mt-10 py-6 text-center text-[12px] text-[#999]">
          <p>MoneyForum · 경제 뉴스 & 투자 포럼</p>
          <p className="mt-1">본 서비스는 투자 조언을 제공하지 않습니다. 투자 판단의 책임은 사용자에게 있습니다.</p>
        </footer>
      </body>
    </html>
  );
}
