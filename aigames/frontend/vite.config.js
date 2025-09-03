import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  base: '/',

  build: {
    outDir: 'dist',
    manifest: true,
    sourcemap: true,
    rollupOptions: {
      input: {
        landing: resolve(__dirname, 'src/js/landing.js'),
      },
      output: {
        entryFileNames: 'js/[name]-[hash].js',
        chunkFileNames: 'js/[name]-[hash].js',
        assetFileNames: ({ name }) => {
          if (/\.(gif|jpe?g|png|svg|webp)$/.test(name ?? '')) {
            return 'images/[name]-[hash][extname]';
          }
          if (/\.css$/.test(name ?? '')) {
            return 'css/[name]-[hash][extname]';
          }
          if (/\.(mp3|wav)$/.test(name ?? '')) {
            return 'audio/[name]-[hash][extname]';
          }
          return 'assets/[name]-[hash][extname]';
        },
      },
    },
  },

  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8089',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8089',
        ws: true,
      },
    },
  },

  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
});