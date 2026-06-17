'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

const NAV_ITEMS = [
  { href: '/', label: '🏠 홈', description: '핫 이슈' },
  { href: '/forum', label: '📰 포럼', description: '경제 뉴스 & 토론' },
  { href: '/market', label: '📊 시장', description: '지표 & 예측' },
  { href: '/dividend', label: '💰 배당', description: '배당금 관리' },
];

export default function Navigation() {
  const pathname = usePathname();

  return (
    <header className="bg-primary text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="text-xl font-bold tracking-tight">
            💹 MoneyForum
          </Link>
          <nav className="flex gap-1">
            {NAV_ITEMS.map(item => (
              <Link
                key={item.href}
                href={item.href}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  pathname === item.href
                    ? 'bg-white/20 text-white'
                    : 'text-gray-300 hover:bg-white/10 hover:text-white'
                }`}
              >
                {item.label}
              </Link>
            ))}
          </nav>
          <button className="px-4 py-2 bg-blue-600 rounded-lg text-sm font-medium hover:bg-blue-700">
            로그인
          </button>
        </div>
      </div>
    </header>
  );
}
