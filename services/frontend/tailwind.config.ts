import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        primary: '#03c75a',       // 네이버 그린
        'primary-dark': '#009f47',
        positive: '#d63031',      // 상승 빨강 (한국 주식 관례)
        negative: '#0984e3',      // 하락 파랑
        'card-bg': '#ffffff',
        'page-bg': '#f5f6f8',
        'border': '#e8e8e8',
        'text-main': '#1e1e1e',
        'text-sub': '#666666',
        'text-light': '#999999',
      },
    },
  },
  plugins: [],
};

export default config;
