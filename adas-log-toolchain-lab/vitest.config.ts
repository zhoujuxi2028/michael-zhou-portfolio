import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['tests/**/*.test.ts'],
    exclude: ['tests/perf/**', 'node_modules/**'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.ts'],
      exclude: ['src/cli.ts'],
      reporter: ['text', 'text-summary', 'json-summary'],
      thresholds: {
        lines: 90,
        'src/rules/**.ts': { lines: 100, functions: 100, statements: 100 },
      },
    },
  },
});
