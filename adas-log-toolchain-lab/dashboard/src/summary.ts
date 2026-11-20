export type Severity = 'P1' | 'P2';

export interface Summary {
  meta: {
    toolVersion: string;
    rulesetVersion: string;
    analysisVersion: string;
    anomalies: string;
    replay: string | null;
  };
  totals: { events: number; scenarios: number; anomalies: number; p1: number; p2: number };
  byRule: { ruleId: string; module: string; severity: Severity; count: number }[];
  byModule: { module: string; count: number }[];
  byScenario: { scenarioId: string; count: number; p1: number; p2: number }[];
  timeline: { bucketStartMs: number; count: number }[];
  dataQuality: { invalidJson: number; schemaErrors: number; unknownEvents: number; outOfOrder: number };
  replay: { total: number; passed: number; failed: number; passRate: number } | null;
}

export type ParseResult = { ok: true; value: Summary } | { ok: false; error: string };

type Obj = Record<string, unknown>;
type Kind = 'string' | 'number';

class Invalid extends Error {}

function fail(path: string, expected: string): never {
  throw new Invalid(`${path}: expected ${expected}`);
}

function obj(v: unknown, path: string): Obj {
  if (typeof v !== 'object' || v === null || Array.isArray(v)) fail(path, 'object');
  return v as Obj;
}

function prim(o: Obj, key: string, kind: Kind, path: string): void {
  if (typeof o[key] !== kind || (kind === 'number' && !Number.isFinite(o[key]))) {
    fail(`${path}.${key}`, kind);
  }
}

function fields(v: unknown, path: string, spec: Record<string, Kind>): Obj {
  const o = obj(v, path);
  for (const [k, kind] of Object.entries(spec)) prim(o, k, kind, path);
  return o;
}

function list(root: Obj, key: string, spec: Record<string, Kind>): Obj[] {
  const arr = root[key];
  if (!Array.isArray(arr)) return fail(key, 'array');
  return arr.map((item, i) => fields(item, `${key}[${i}]`, spec));
}

function validate(input: unknown): Summary {
  const root = obj(input, 'summary');
  const meta = obj(root.meta, 'meta');
  for (const k of ['toolVersion', 'rulesetVersion', 'analysisVersion', 'anomalies']) {
    prim(meta, k, 'string', 'meta');
  }
  if (meta.replay !== null && typeof meta.replay !== 'string') fail('meta.replay', 'string or null');
  fields(root.totals, 'totals', { events: 'number', scenarios: 'number', anomalies: 'number', p1: 'number', p2: 'number' });
  const rules = list(root, 'byRule', { ruleId: 'string', module: 'string', severity: 'string', count: 'number' });
  rules.forEach((r, i) => {
    if (r.severity !== 'P1' && r.severity !== 'P2') fail(`byRule[${i}].severity`, 'P1 or P2');
  });
  list(root, 'byModule', { module: 'string', count: 'number' });
  list(root, 'byScenario', { scenarioId: 'string', count: 'number', p1: 'number', p2: 'number' });
  list(root, 'timeline', { bucketStartMs: 'number', count: 'number' });
  fields(root.dataQuality, 'dataQuality', { invalidJson: 'number', schemaErrors: 'number', unknownEvents: 'number', outOfOrder: 'number' });
  if (root.replay !== null) {
    fields(root.replay, 'replay', { total: 'number', passed: 'number', failed: 'number', passRate: 'number' });
  }
  return input as Summary;
}

export function parseSummary(input: unknown): ParseResult {
  try {
    return { ok: true, value: validate(input) };
  } catch (e) {
    if (e instanceof Invalid) return { ok: false, error: e.message };
    throw e;
  }
}
