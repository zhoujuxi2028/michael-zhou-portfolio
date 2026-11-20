import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { parse } from 'yaml';

interface Step {
  uses?: string;
  run?: string;
  name?: string;
  'working-directory'?: string;
  with?: Record<string, unknown>;
}
interface Workflow {
  on: { push: { branches: string[]; paths: string[] }; workflow_dispatch: unknown };
  permissions: Record<string, string>;
  concurrency: { group: string; 'cancel-in-progress': boolean };
  jobs: Record<string, { steps: Step[]; needs?: string | string[]; environment?: unknown; if?: string }>;
}

const root = new URL('../../../.github/workflows/', import.meta.url);
const load = (f: string) => parse(readFileSync(new URL(f, root), 'utf8')) as Workflow;

describe('adas-pages.yml', () => {
  const wf = load('adas-pages.yml');
  const build = wf.jobs.build;
  const runs = (build?.steps ?? []).map((s) => s.run ?? '').join('\n');
  const uses = (build?.steps ?? []).map((s) => s.uses ?? '');

  it('triggers on push to main (path filtered) and manual dispatch', () => {
    expect(wf.on.push.branches).toEqual(['main']);
    expect(wf.on.push.paths).toContain('adas-log-toolchain-lab/**');
    expect(wf.on).toHaveProperty('workflow_dispatch');
  });

  it('has Pages deploy permissions', () => {
    expect(wf.permissions.pages).toBe('write');
    expect(wf.permissions['id-token']).toBe('write');
  });

  it('sets up node 22 and python 3.13', () => {
    const node = build?.steps.find((s) => s.uses?.startsWith('actions/setup-node'));
    const py = build?.steps.find((s) => s.uses?.startsWith('actions/setup-python'));
    expect(String(node?.with?.['node-version'])).toBe('22');
    expect(String(py?.with?.['python-version'])).toBe('3.13');
  });

  it('runs the full pipeline in order', () => {
    const order = [
      'npm ci',
      'cli.ts gen',
      'cli.ts parse',
      'cases regression',
      'cli.ts replay',
      'pip install -r requirements.txt',
      'python -m adas_analysis',
      'npm run build',
    ].map((needle) => {
      const i = runs.indexOf(needle);
      expect(i, needle).toBeGreaterThanOrEqual(0);
      return i;
    });
    expect([...order].sort((a, b) => a - b)).toEqual(order);
  });

  it('writes the analysis output into the dashboard public dir', () => {
    expect(runs).toContain('dashboard/public/summary.json');
  });

  it('runs each pipeline step in the right directory with the right paths', () => {
    const step = (needle: string) => {
      const st = build?.steps.find((x) => x.run?.includes(needle));
      expect(st, needle).toBeDefined();
      return st as Step;
    };
    const lab = 'adas-log-toolchain-lab';
    expect(step('cli.ts gen')['working-directory']).toBe(lab);
    expect(step('cli.ts gen').run).toContain('cli.ts gen --out out');
    expect(step('cli.ts parse')['working-directory']).toBe(lab);
    expect(step('cases regression')['working-directory']).toBe(lab);
    const analysis = step('python -m adas_analysis');
    expect(analysis['working-directory']).toBe(`${lab}/analysis`);
    expect(analysis.run).toContain('--anomalies ../out/anomalies.json');
    expect(analysis.run).toContain('--replay ../out/replay-report.json');
    expect(analysis.run).toContain('--out ../dashboard/public/summary.json');
    const dash = step('npm run build');
    expect(dash['working-directory']).toBe(`${lab}/dashboard`);
    expect(dash.run).toContain('npm ci');
  });

  it('guards deploy behind ADAS_PAGES_ENABLED and never cancels an in-flight deploy', () => {
    expect(wf.jobs.deploy?.if).toContain("vars.ADAS_PAGES_ENABLED == 'true'");
    expect(wf.concurrency['cancel-in-progress']).toBe(false);
  });

  it('uploads the dashboard dist and deploys to Pages', () => {
    const upload = build?.steps.find((s) => s.uses?.startsWith('actions/upload-pages-artifact'));
    expect(upload?.with?.path).toBe('adas-log-toolchain-lab/dashboard/dist');
    expect(uses.some((u) => u.startsWith('actions/checkout@'))).toBe(true);
    const deploy = wf.jobs.deploy;
    expect(deploy?.needs).toBe('build');
    expect(deploy?.environment).toBeDefined();
    expect(deploy?.steps.some((s) => s.uses?.startsWith('actions/deploy-pages@'))).toBe(true);
  });
});

describe('ci.yml adas-dashboard job', () => {
  const job = load('ci.yml').jobs['adas-dashboard'];
  const runs = (job?.steps ?? []).flatMap((s) => (s.run ? [s.run] : []));

  it('exists and runs install, lint, typecheck, test, build', () => {
    expect(job).toBeDefined();
    expect(runs).toEqual(['npm ci', 'npm run lint', 'npm run typecheck', 'npm test', 'npm run build']);
  });

  it('caches npm on the dashboard lockfile', () => {
    const node = job?.steps.find((s) => s.uses?.startsWith('actions/setup-node'));
    expect(node?.with?.['cache-dependency-path']).toBe('adas-log-toolchain-lab/dashboard/package-lock.json');
  });
});
