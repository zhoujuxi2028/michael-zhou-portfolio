import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { parse } from 'yaml';

const text = readFileSync(new URL('../.gitlab-ci.yml', import.meta.url), 'utf8');

describe('.gitlab-ci.yml example', () => {
  it('is marked as an unexecuted example', () => {
    expect(text.split('\n')[0]).toMatch(/example, not executed on GitLab/i);
  });

  type Job = { stage?: string; image?: string; script?: string[]; before_script?: string[]; variables?: Record<string, string> };
  const ci = parse(text) as Record<string, Job> & { stages: string[] };

  it('is valid YAML with lint and test jobs mirroring GitHub Actions', () => {
    expect(ci.stages).toEqual(['lint', 'test', 'analysis', 'dashboard']);
    expect(ci['lint']?.script).toEqual(['npm run lint', 'npm run typecheck']);
    expect(ci['test']?.script).toContain('npm run test:coverage');
  });

  it('has an analysis job on Python 3.13 running flake8 and pytest', () => {
    const job = ci['analysis'];
    expect(job?.stage).toBe('analysis');
    expect(job?.image).toBe('python:3.13');
    expect(job?.script).toEqual(
      expect.arrayContaining(['cd analysis', 'flake8 --max-line-length 100 .', 'pytest -v']),
    );
  });

  it('has a dashboard job on Node 22 running lint, typecheck, test and build', () => {
    const job = ci['dashboard'];
    expect(job?.stage).toBe('dashboard');
    expect(job?.image).toBe('node:22');
    for (const cmd of ['npm run lint', 'npm run typecheck', 'npm test', 'npm run build']) {
      expect(job?.script).toContain(cmd);
    }
  });
});
