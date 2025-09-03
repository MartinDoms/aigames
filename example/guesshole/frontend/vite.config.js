// Your vite configuration code

import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  // Base public path when served in production
  base: '/',

  // Define your build output directory
  build: {
    outDir: 'dist',
    // Generate manifest.json for asset hashing
    manifest: true,
    sourcemap: true,
    // Output directory structure
    rollupOptions: {
      input: {
        // One entry point for each feature (jsRoot in my page templates)
        landing: resolve(__dirname, 'src/js/landing.js'),
        'lobby-form': resolve(__dirname, 'src/js/lobby-form-main.js'),
        lobby: resolve(__dirname, 'src/js/features/lobby/lobby-main.js'),
        'admin-videos': resolve(__dirname, 'src/js/admin-videos-main.js'),
        'admin-map': resolve(__dirname, 'src/js/admin-map-main.js'),
        'admin-add-template': resolve(
          __dirname,
          'src/js/admin-add-template-main.js',
        ),

        // test harnesses
        'round-scoreboard-harness': resolve(
          __dirname,
          'src/js/features/component-harnesses/round-scoreboard-harness.js',
        ),
      },
      output: {
        // Configure chunk names and directory structure
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

  // Configure dev server
  server: {
    // Proxy API requests to your Spring Boot backend during development
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8088',
        ws: true,
      },
    },
  },

  // Add support for resolving paths with @
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
});
