import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
    './src/context/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        wine: '#711A38',
        wineDark: '#4C1126',
        mauve: '#B7748C',
        blush: '#F5E5E8',
        cream: '#FCF8F2',
        ink: '#2B2025',
        muted: '#786A6F',
        border: '#EADDE0',
        success: '#22795B',
      },
      fontFamily: {
        serif: ['var(--font-display)', 'Georgia', 'serif'],
        sans: ['var(--font-body)', 'system-ui', '-apple-system', '"Segoe UI"', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 18px 52px rgba(92, 26, 49, 0.09)',
        glow: '0 8px 23px rgba(113, 26, 56, 0.18)',
      },
    },
  },
  plugins: [],
};

export default config;
