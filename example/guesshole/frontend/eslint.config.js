import js from '@eslint/js';
import globals from 'globals';
import { defineConfig } from 'eslint/config';
import prettierConfig from 'eslint-config-prettier';
import prettierPlugin from 'eslint-plugin-prettier';
import editorconfig from 'eslint-plugin-editorconfig';
import { readFileSync } from 'fs';

// Import the prettier config from the shared .prettierrc.json file
const prettierRules = JSON.parse(
  readFileSync(new URL('./.prettierrc.json', import.meta.url), 'utf-8'),
);

export default defineConfig([
  js.configs.recommended,
  prettierConfig,
  {
    files: ['**/*.{js,mjs,cjs}'],
    languageOptions: {
      globals: {
        ...globals.browser,
      },
      ecmaVersion: 2022,
      sourceType: 'module',
    },
    plugins: {
      prettier: prettierPlugin,
      editorconfig: editorconfig,
    },
    rules: {
      'editorconfig/charset': 'warn',
      'editorconfig/eol-last': 'off', // Let Prettier handle this
      'editorconfig/indent': 'off', // Let Prettier handle this
      'editorconfig/linebreak-style': 'warn',
      'editorconfig/no-trailing-spaces': 'off', // Let Prettier handle this

      // Use prettier with the shared config
      'prettier/prettier': ['error', prettierRules],
    },
  },
  {
    files: ['vite.config.js', 'vite.config.ts'],
    languageOptions: {
      globals: {
        __dirname: 'readonly',
        process: 'readonly',
      },
    },
  },
]);
