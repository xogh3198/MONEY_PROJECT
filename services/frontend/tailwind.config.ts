import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        dark: {
          bg: '#0f1117',
          card: '#1e2235',
          secondary: '#1a1d29',
          border: '#2a2d3a',
        },
        accent: '#4f8cff',
        positive: '#00d26a',
        negative: '#ff4757',
        neutral: '#6b7280',
        'text-primary': '#e8eaed',
        'text-secondary': '#9aa0a6',
      },
      borderRadius: {
        'xl': '12px',
        '2xl': '16px',
      },
    },
  },
  plugins: [],
};

export default config;
