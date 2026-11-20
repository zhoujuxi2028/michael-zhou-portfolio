import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';

export default defineConfig([
  { ignores: ['node_modules/', 'dist/'] },
  { languageOptions: { parserOptions: { tsconfigRootDir: import.meta.dirname } } },
  tseslint.configs.recommended,
]);
