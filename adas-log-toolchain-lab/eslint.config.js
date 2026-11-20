import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';

export default defineConfig([
  // dashboard/ and analysis/ are separate packages with their own lint (see their CI jobs).
  { ignores: ['node_modules/', 'out/', 'coverage/', 'dashboard/', 'analysis/'] },
  tseslint.configs.recommended,
  { languageOptions: { parserOptions: { tsconfigRootDir: import.meta.dirname } } },
]);
