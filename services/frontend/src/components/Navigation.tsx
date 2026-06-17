'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

const NAV_ITEMS = [
  { href: '/', label: '홈', icon: '🏠' },
  { href: '/forum', label: '포럼', icon: '📰' },
  { href: '/market', label: '시장', icon: '📊' },
  { href: '/dividend', label: '배당', icon: '💰' },
];

export default function Navigation() {
  const pathname = usePathname();

  return (
    <header className="sticky top-0 z-50 border-b border-dark-border bg-dark-bg/80 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* 로고 */}
          <Link href="/" className="flex items-center gap-2">
            <span className="text-2xl">💹</span>
            <span className="text-lg font-bold gradient-text">MoneyForum</span>
          </Link>

          {/* 네비게이션 */}
          <nav className="hidden sm:flex items-center gap-1">
            {NAV_ITEMS.map(item => {
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-accent/15 text-accent'
                      : 'text-text-secondary hover:text-text-primary hover:bg-dark-card'
                  }`}
                >
                  <span className="text-base">{item.icon}</span>
                  {item.label}
                </Link>
              );
            })}
          </nav>

          {/* 우측 */}
          <div className="flex items-center gap-3">
            <button className="px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary transition-colors">
              로그인
            </button>
            <button className="px-4 py-2 bg-accent text-white text-sm font-medium rounded-lg hover:bg-accent/90 transition-colors">
              시작하기
            </button>
          </div>
        </div>
      </div>

      {/* 모바일 네비 */}
      <nav className="sm:hidden flex border-t border-dark-border">
        {NAV_ITEMS.map(item => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex-1 flex flex-col items-center py-2 text-xs ${
                isActive ? 'text-accent' : 'text-text-secondary'
              }`}
            >
              <span className="text-lg">{item.icon}</span>
              {item.label}
            </Link>
          );
        })}
      </nav>
    </header>
  );
}
