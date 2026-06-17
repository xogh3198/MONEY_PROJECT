'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

const NAV_ITEMS = [
  { href: '/', label: '홈' },
  { href: '/forum', label: '경제뉴스' },
  { href: '/market', label: '시장지표' },
  { href: '/dividend', label: '배당관리' },
];

export default function Navigation() {
  const pathname = usePathname();

  return (
    <>
      {/* 상단 헤더 */}
      <header className="bg-white border-b border-[#e4e4e4] sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 flex items-center justify-between h-12">
          <Link href="/" className="text-lg font-bold text-[#03c75a]">
            MoneyForum
          </Link>
          <div className="flex items-center gap-4">
            <input
              type="text"
              placeholder="뉴스, 종목 검색"
              className="w-56 px-3 py-1.5 text-sm border border-[#e4e4e4] rounded bg-[#f5f6f8] focus:outline-none focus:border-[#03c75a]"
            />
            <button className="text-sm text-[#03c75a] font-medium hover:underline">로그인</button>
          </div>
        </div>
      </header>

      {/* 서브 네비게이션 (탭) */}
      <nav className="bg-white border-b border-[#e4e4e4]">
        <div className="max-w-6xl mx-auto px-4 flex">
          {NAV_ITEMS.map(item => (
            <Link
              key={item.href}
              href={item.href}
              className={`relative px-5 py-3 text-sm font-medium transition-colors ${
                pathname === item.href
                  ? 'text-[#03c75a]'
                  : 'text-[#666] hover:text-[#333]'
              }`}
            >
              {item.label}
              {pathname === item.href && (
                <span className="absolute bottom-0 left-0 right-0 h-[2px] bg-[#03c75a]" />
              )}
            </Link>
          ))}
        </div>
      </nav>
    </>
  );
}
