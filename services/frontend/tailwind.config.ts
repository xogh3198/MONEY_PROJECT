import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        primary: '#1a1a2e',
        accent: '#16213e',
        positive: '#22c55e',
        negative: '#ef4444',
        neutral: '#6b7280',
      },
    },
  },
  plugins: [],
};

export default config;
