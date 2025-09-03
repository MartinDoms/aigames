/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './src/**/*.{js,html}',
    '../src/main/resources/templates/**/*.html',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#333652', // Dark blue
          light: '#4a4d6a',
          dark: '#252840',
        },
        secondary: {
          DEFAULT: '#90adc6', // Blue-gray
          light: '#a5bfd4',
          dark: '#7a96ad',
        },
        accent: {
          DEFAULT: '#fad02c', // Yellow
          light: '#fbda58',
          dark: '#e9bf1a',
        },
        neutral: {
          DEFAULT: '#e9eaec', // Light gray
          light: '#f5f6f7',
          dark: '#d0d2d6',
        },
      },
      animation: {
        highlight: 'highlight 2s ease-in-out',
        fadeIn: 'fadeIn 0.3s forwards',
        fadeOut: 'fadeOut 0.3s 1.7s forwards',
      },
      keyframes: {
        highlight: {
          '0%': { backgroundColor: 'var(--color-neutral)' },
          '30%': { backgroundColor: 'var(--color-secondary-light)' },
          '100%': { backgroundColor: 'var(--color-neutral)' },
        },
        fadeIn: {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        fadeOut: {
          from: { opacity: '1' },
          to: { opacity: '0' },
        },
        pulse: {
          '0%, 100%': { transform: 'scale(1)' },
          '50%': { transform: 'scale(1.05)' },
        },
      },
    },
  },
  plugins: [],
};
