import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { parse } from 'yaml';

const text = readFileSync(new URL('../.gitlab-ci.yml', import.meta.url), 'utf8');

describe('.gitlab-ci.yml example', () => {
  it('is marked as an unexecuted example', () => {
    expect(text.split('\n')[0]).toMatch(/example, not executed on GitLab/i);
  });

  it('is valid YAML with lint and test jobs mirroring GitHub Actions', () => {
    const ci = parse(text) as Record<string, { stage?: string; script?: string[] }> & { stages: string[] };
    expect(ci.stages).toEqual(['lint', 'test']);
    expect(ci['lint']?.script).toEqual(['npm run lint', 'npm run typecheck']);
    expect(ci['test']?.script).toContain('npm run test:coverage');
  });
});
