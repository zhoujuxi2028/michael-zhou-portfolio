# REQ-ADAS-01 Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `adas-log-toolchain-lab/` 交付 Phase 1：合成数据生成（`gen`）、流式异常检测（`parse`，ACC / APA / HMI 各 2 条规则）、规则驱动的边界值用例生成（`cases boundary`），以及英文交付、版本管理、CI。

**Architecture:** 单个 TypeScript 包，Node 直接运行 `.ts`（type stripping，无需构建）。模块之间只通过 JSON Lines / JSON 文件通信。规则引擎按场景隔离状态，用"待确认 + 截止时间"实现时间窗口判断，不使用定时器。

**Tech Stack:** Node ≥ 22.18、TypeScript 5.9.3（strict）、zod 4、commander 15、Vitest 5（v8 coverage）、ESLint 10 + typescript-eslint 8、yaml 2。

**Spec:** [`docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md`](../specs/2026-11-20-adas-log-toolchain-design.md) ｜ **Requirements:** [`adas-log-toolchain-lab/docs/requirements/REQUIREMENTS.md`](../../../adas-log-toolchain-lab/docs/requirements/REQUIREMENTS.md) ｜ **Issue:** #69

---

## 0. 与 spec 的差异（实施时确定）

| # | spec | 本计划 | 原因 |
|---|------|--------|------|
| P1 | TS 7，ESLint 不兼容时固定 5.x | 固定 `typescript@5.9.3` | `typescript-eslint@8.73.0` 的 peer 依赖为 `typescript <6.1.0`（风险 R2 触发） |
| P2 | GitHub Actions 中用 `yamllint` 校验 `.gitlab-ci.yml` | Vitest 测试中用 `yaml` 包解析并检查结构 | 本机与 runner 均无 yamllint / pipx；测试方式本机和 CI 一致 |
| P3 | CLI 端到端测试用 `execa` | 用 `node:child_process` 的 `spawnSync` | 少一个依赖，功能足够 |
| P4 | `Rule.boundaryCases(cfg)` | `boundaryCases(cfg, eps)` | ε 来自配置的 `epsilon` 段，显式传入 |
| P5 | `Rule.onEvent(e, ctx)` | `ctx` 中携带 `state`、`cfg`、`now` | 规则状态由引擎按场景保存，规则本身无状态 |

## 1. 约定

- 所有命令在 `adas-log-toolchain-lab/` 目录下执行（除非写明在仓库根目录）
- 开发分支：`feature/adas-log-toolchain`（不在 main 上开发）
- 提交信息：`feat(adas): …` / `test(adas): …` / `docs(adas): …` / `ci(adas): …`，结尾加 `Refs #69` 与 Co-Authored-By 行
- 代码、注释、CLI 输出、异常 `message` 一律英文
- 相对导入写 `.ts` 扩展名；仅类型的导入必须用 `import type`（`verbatimModuleSyntax`，Node type stripping 要求）
- 不使用 enum、namespace、参数属性（`erasableSyntaxOnly`）

## 2. 文件结构

```
adas-log-toolchain-lab/
├── package.json / package-lock.json / tsconfig.json / eslint.config.js
├── vitest.config.ts / vitest.perf.config.ts / .gitignore
├── .gitlab-ci.yml                 # T13 示例
├── CHANGELOG.md / README.md / README.zh.md   # T13
├── fixtures/
│   ├── bad-lines.jsonl            # T5 数据质量样本
│   └── golden/mixed.jsonl         # T8 黄金样本
├── src/
│   ├── cli.ts                     # 入口：process.exitCode = await main(argv)
│   ├── cli/main.ts                # commander：gen / parse / cases boundary，退出码
│   ├── config.ts                  # loadConfig + UsageError
│   ├── version.ts                 # TOOL_VERSION / RULESET_VERSION
│   ├── io/jsonl.ts                # readLines / toJsonl
│   ├── schema/events.ts           # zod 事件 schema、AdasEvent 类型、ev() 构造器
│   ├── schema/validate.ts         # parseLine：单行 → 事件 / 错误分类
│   ├── generator/prng.ts          # mulberry32 + Rng
│   ├── generator/generate.ts      # 场景脚本、故障注入、labels
│   ├── engine/types.ts            # Rule / Anomaly / BoundaryCase / Epsilon / anomaly()
│   ├── engine/engine.ts           # createEngine：场景隔离、SCENARIO_END、EOF 兜底
│   ├── rules/boundary.ts          # triplet()：生成 −ε / 正好 / +ε 三个用例
│   ├── rules/acc-01.ts … hmi-02.ts  # 6 条规则（各含 boundaryCases）
│   ├── rules/index.ts             # RULES 注册表
│   ├── parser/scan.ts             # 逐行读取 + 数据质量
│   ├── parser/parse.ts            # scan + engine + 报告
│   ├── eval/match.ts              # 异常 ↔ 真值标签匹配，精确率 / 召回率
│   └── casegen/boundary.ts        # 收集、写出、校验边界用例
└── tests/                         # 与 src 对应；perf/ 单独配置
```

## 3. 任务总览

| 任务 | 内容 | 依赖 |
|------|------|------|
| T1 | 项目骨架 + `version.ts` | — |
| T2 | 事件 schema + `parseLine` | T1 |
| T3 | mulberry32 PRNG | T1 |
| T4 | 规则引擎核心（类型、引擎、`triplet`） | T2 |
| T5 | 逐行读取 + 数据质量 | T2 |
| T6a–T6f | 6 条规则 | T4 |
| T7 | 报告组装 + `parseFile` + 黄金样本 | T5、T6 |
| T8 | `generator` | T2、T3 |
| T9 | 真值匹配 + 真值测试 | T7、T8 |
| T10 | `casegen boundary` | T6 |
| T11 | 配置加载 + CLI + 退出码 | T7、T8、T10 |
| T12 | CLI 端到端 + 性能冒烟 | T11 |
| T13 | README（EN / ZH）、CHANGELOG、`.gitlab-ci.yml` + 校验测试 | T11 |
| T14 | GitHub Actions `adas` job + husky 钩子 + 覆盖率门槛 | T13 |

> 与 issue #69 草案的对应：草案 T4 generator → 本计划 T8；草案 T5/T6 → 本计划 T5/T4（引擎先于规则）；草案 T7a–f → T6a–f；草案 T8 → T7。内容不变，只调整顺序以满足依赖。

---

### Task 1: 项目骨架

**Files:**
- Create: `adas-log-toolchain-lab/package.json`, `tsconfig.json`, `eslint.config.js`, `vitest.config.ts`, `vitest.perf.config.ts`, `.gitignore`, `src/version.ts`
- Test: `adas-log-toolchain-lab/tests/version.test.ts`

- [ ] **Step 1: 创建 `package.json`**

```json
{
  "name": "adas-log-toolchain-lab",
  "version": "0.1.0",
  "description": "ADAS log toolchain lab: synthetic logs, anomaly detection, test case generation (simulation only)",
  "private": true,
  "type": "module",
  "engines": { "node": ">=22.18" },
  "scripts": {
    "adas": "node src/cli.ts",
    "demo": "node src/cli.ts gen --out out && node src/cli.ts parse out/events.jsonl --out out/anomalies.json && node src/cli.ts cases boundary --out out/cases",
    "lint": "eslint .",
    "typecheck": "tsc --noEmit",
    "test": "vitest run",
    "test:coverage": "vitest run --coverage",
    "test:perf": "vitest run --config vitest.perf.config.ts"
  }
}
```

- [ ] **Step 2: 安装依赖（生成 `package-lock.json`）**

```bash
cd adas-log-toolchain-lab
npm install --save-exact zod@4.6.7 commander@15.1.0
npm install --save-exact -D typescript@5.9.3 vitest@5.1.1 @vitest/coverage-v8@5.1.1 eslint@10.15.0 typescript-eslint@8.73.0 @types/node@22.20.5 yaml@2.9.1
```

Expected: 无 `ERESOLVE` 报错，生成 `package-lock.json`。

- [ ] **Step 3: 创建 `tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2023",
    "lib": ["ES2024"],
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitOverride": true,
    "allowImportingTsExtensions": true,
    "verbatimModuleSyntax": true,
    "erasableSyntaxOnly": true,
    "noEmit": true,
    "skipLibCheck": true,
    "types": ["node"]
  },
  "include": ["src", "tests", "vitest.config.ts", "vitest.perf.config.ts"]
}
```

- [ ] **Step 4: 创建 `eslint.config.js`、两个 Vitest 配置、`.gitignore`**

`eslint.config.js`：

```js
import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';

export default defineConfig([
  { ignores: ['node_modules/', 'out/', 'coverage/'] },
  tseslint.configs.recommended,
]);
```

`vitest.config.ts`（覆盖率门槛在 T14 开启）：

```ts
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
    },
  },
});
```

`vitest.perf.config.ts`：

```ts
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: { include: ['tests/perf/**/*.test.ts'], testTimeout: 120_000 },
});
```

`.gitignore`：

```
node_modules/
out/
coverage/
```

- [ ] **Step 5: 写失败测试 `tests/version.test.ts`**

```ts
import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { RULESET_VERSION, TOOL_VERSION } from '../src/version.ts';

describe('version', () => {
  it('TOOL_VERSION matches package.json', () => {
    const pkg = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8')) as { version: string };
    expect(TOOL_VERSION).toBe(pkg.version);
  });

  it('RULESET_VERSION is semver', () => {
    expect(RULESET_VERSION).toMatch(/^\d+\.\d+\.\d+$/);
  });
});
```

- [ ] **Step 6: 运行，确认失败**

Run: `npx vitest run tests/version.test.ts`
Expected: FAIL — `Failed to resolve import "../src/version.ts"`

- [ ] **Step 7: 实现 `src/version.ts`**

```ts
/** Tool version; must match package.json. */
export const TOOL_VERSION = '0.1.0';

/**
 * Ruleset version. Bump rules:
 * - major: default threshold or judgment logic changed
 * - minor: rule added
 * - patch: message / description fixes
 */
export const RULESET_VERSION = '1.0.0';
```

- [ ] **Step 8: 运行测试、类型检查、lint**

Run: `npx vitest run tests/version.test.ts && npm run typecheck && npm run lint`
Expected: 2 passed；`tsc` 与 `eslint` 无输出

- [ ] **Step 9: 提交**

```bash
git add .
git commit -m "feat(adas): scaffold TypeScript project

Refs #69"
```

---

### Task 2: 事件 schema 与单行解析

**Files:**
- Create: `src/schema/events.ts`, `src/schema/validate.ts`
- Test: `tests/schema/validate.test.ts`

- [ ] **Step 1: 写失败测试 `tests/schema/validate.test.ts`**

```ts
import { describe, expect, it } from 'vitest';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { parseLine } from '../../src/schema/validate.ts';

const line = (o: unknown) => JSON.stringify(o);

describe('parseLine', () => {
  it('accepts a valid ACC TRACK event', () => {
    const e = ev('ACC/TRACK', 100, 'S001', { egoSpeed: 20, setSpeed: 22, leadDistance: null, leadRelSpeed: 0 });
    const r = parseLine(line(e));
    expect(r).toEqual({ ok: true, event: e });
  });

  it('round-trips every event built with ev()', () => {
    const events = [
      ev('ACC/STATE', 0, 'S', { from: 'OFF', to: 'ACTIVE', reason: 'x' }),
      ev('ACC/BRAKE_PEDAL', 1, 'S', { pressed: true }),
      ev('ACC/TAKEOVER_REQUEST', 2, 'S', { reason: 'x' }),
      ev('APA/STEP', 3, 'S', { step: 'MANEUVERING', reason: 'x' }),
      ev('APA/OBSTACLE', 4, 'S', { minDistance: 0.5 }),
      ev('HMI/DISPLAY', 5, 'S', { feature: 'APA', item: 'STATE', value: 'MANEUVERING' }),
      scenarioEnd('S', 6),
    ];
    for (const e of events) expect(parseLine(line(e))).toEqual({ ok: true, event: e });
  });

  it('classifies invalid JSON', () => {
    expect(parseLine('{not json')).toEqual({ ok: false, kind: 'invalidJson', detail: 'invalid JSON' });
  });

  it('classifies envelope errors with field path', () => {
    const r = parseLine(line({ ts: -1, scenarioId: 'S', module: 'ACC', type: 'TRACK', data: {} }));
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.kind).toBe('schemaError');
      expect(r.detail).toContain('ts');
    }
  });

  it('classifies data errors with data.* path', () => {
    const r = parseLine(line({ ts: 1, scenarioId: 'S', module: 'ACC', type: 'TRACK', data: { egoSpeed: 'fast', setSpeed: 1, leadDistance: null, leadRelSpeed: 0 } }));
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.kind).toBe('schemaError');
      expect(r.detail).toContain('data.egoSpeed');
    }
  });

  it('classifies unknown module/type', () => {
    expect(parseLine(line({ ts: 1, scenarioId: 'S', module: 'RADAR', type: 'POINTS', data: {} }))).toEqual({
      ok: false, kind: 'unknownEvent', detail: 'RADAR/POINTS',
    });
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/schema`
Expected: FAIL — cannot resolve `../../src/schema/events.ts`

- [ ] **Step 3: 实现 `src/schema/events.ts`**

```ts
import { z } from 'zod';

export const AccState = z.enum(['OFF', 'STANDBY', 'ACTIVE', 'OVERRIDE']);
export type AccState = z.infer<typeof AccState>;

export const ApaStep = z.enum(['SEARCHING', 'SLOT_SELECTED', 'MANEUVERING', 'COMPLETED', 'ABORTED']);
export type ApaStep = z.infer<typeof ApaStep>;

export const HmiFeature = z.enum(['ACC', 'APA', 'LKA']);
export type HmiFeature = z.infer<typeof HmiFeature>;

/** Common envelope shared by every event line. */
export const Envelope = z.object({
  ts: z.number().int().nonnegative(),
  scenarioId: z.string().min(1),
  module: z.string().min(1),
  type: z.string().min(1),
  data: z.unknown(),
});

/** Payload schema per `${module}/${type}` (Phase 1). */
export const dataSchemas = {
  'ACC/STATE': z.object({ from: AccState, to: AccState, reason: z.string() }),
  'ACC/TRACK': z.object({
    egoSpeed: z.number().nonnegative(),
    setSpeed: z.number().nonnegative(),
    leadDistance: z.number().nonnegative().nullable(),
    leadRelSpeed: z.number(),
  }),
  'ACC/BRAKE_PEDAL': z.object({ pressed: z.boolean() }),
  'ACC/TAKEOVER_REQUEST': z.object({ reason: z.string() }),
  'APA/STEP': z.object({ step: ApaStep, reason: z.string() }),
  'APA/OBSTACLE': z.object({ minDistance: z.number().nonnegative() }),
  'HMI/DISPLAY': z.object({
    feature: HmiFeature,
    item: z.enum(['STATE', 'TAKEOVER', 'UNAVAILABLE']),
    value: z.string(),
  }),
  'SYS/SCENARIO_END': z.object({}),
} as const;

export type EventKey = keyof typeof dataSchemas;
export type DataOf<K extends EventKey> = z.infer<(typeof dataSchemas)[K]>;
type ModuleOf<K> = K extends `${infer M}/${string}` ? M : never;
type TypeOf<K> = K extends `${string}/${infer T}` ? T : never;

/** Discriminated union: narrow with `e.module === 'ACC' && e.type === 'TRACK'`. */
export type AdasEvent = {
  [K in EventKey]: { ts: number; scenarioId: string; module: ModuleOf<K>; type: TypeOf<K>; data: DataOf<K> };
}[EventKey];

export type Module = AdasEvent['module'];

/** Build a typed event; key order matches the JSON Lines output. */
export function ev<K extends EventKey>(key: K, ts: number, scenarioId: string, data: DataOf<K>): AdasEvent {
  const [module, type] = key.split('/');
  return { ts, scenarioId, module, type, data } as unknown as AdasEvent;
}

export function scenarioEnd(scenarioId: string, ts: number): AdasEvent {
  return ev('SYS/SCENARIO_END', ts, scenarioId, {});
}
```

- [ ] **Step 4: 实现 `src/schema/validate.ts`**

```ts
import type { z } from 'zod';
import { dataSchemas, Envelope, type AdasEvent } from './events.ts';

export type LineResult =
  | { ok: true; event: AdasEvent }
  | { ok: false; kind: 'invalidJson' | 'schemaError' | 'unknownEvent'; detail: string };

function formatIssues(error: z.ZodError, prefix?: string): string {
  return error.issues
    .map((i) => `${[prefix, ...i.path.map(String)].filter(Boolean).join('.')}: ${i.message}`)
    .join('; ');
}

/** Parse one JSON Lines record into a typed event or a classified error. */
export function parseLine(line: string): LineResult {
  let raw: unknown;
  try {
    raw = JSON.parse(line);
  } catch {
    return { ok: false, kind: 'invalidJson', detail: 'invalid JSON' };
  }
  const env = Envelope.safeParse(raw);
  if (!env.success) return { ok: false, kind: 'schemaError', detail: formatIssues(env.error) };
  const key = `${env.data.module}/${env.data.type}`;
  const schema = (dataSchemas as Record<string, z.ZodType | undefined>)[key];
  if (!schema) return { ok: false, kind: 'unknownEvent', detail: key };
  const data = schema.safeParse(env.data.data);
  if (!data.success) return { ok: false, kind: 'schemaError', detail: formatIssues(data.error, 'data') };
  return { ok: true, event: { ...env.data, data: data.data } as unknown as AdasEvent };
}
```

- [ ] **Step 5: 运行测试与类型检查**

Run: `npx vitest run tests/schema && npm run typecheck`
Expected: 6 passed；`tsc` 无输出

- [ ] **Step 6: 提交**

```bash
git add src/schema tests/schema
git commit -m "feat(adas): add zod event schema and line parser

Refs #69"
```

---

### Task 3: mulberry32 PRNG

**Files:**
- Create: `src/generator/prng.ts`
- Test: `tests/generator/prng.test.ts`

- [ ] **Step 1: 写失败测试**

```ts
import { describe, expect, it } from 'vitest';
import { createRng, mulberry32 } from '../../src/generator/prng.ts';

describe('mulberry32', () => {
  it('is deterministic for the same seed', () => {
    const a = mulberry32(42), b = mulberry32(42);
    expect(Array.from({ length: 5 }, a)).toEqual(Array.from({ length: 5 }, b));
  });

  it('differs across seeds and stays in [0, 1)', () => {
    const a = Array.from({ length: 1000 }, mulberry32(1));
    const b = Array.from({ length: 1000 }, mulberry32(2));
    expect(a).not.toEqual(b);
    for (const x of a) expect(x >= 0 && x < 1).toBe(true);
  });
});

describe('createRng', () => {
  it('int() is inclusive and within bounds', () => {
    const rng = createRng(7);
    const seen = new Set<number>();
    for (let i = 0; i < 2000; i++) {
      const v = rng.int(50, 52);
      expect(v >= 50 && v <= 52 && Number.isInteger(v)).toBe(true);
      seen.add(v);
    }
    expect([...seen].sort()).toEqual([50, 51, 52]);
  });

  it('range() stays within [min, max)', () => {
    const rng = createRng(9);
    for (let i = 0; i < 1000; i++) {
      const v = rng.range(-0.5, 0.5);
      expect(v >= -0.5 && v < 0.5).toBe(true);
    }
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/generator/prng.test.ts`
Expected: FAIL — cannot resolve `prng.ts`

- [ ] **Step 3: 实现 `src/generator/prng.ts`**

```ts
/** mulberry32: small seedable PRNG, identical output across Node versions. Never use Math.random. */
export function mulberry32(seed: number): () => number {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export interface Rng {
  next(): number;
  /** Float in [min, max). */
  range(min: number, max: number): number;
  /** Integer in [min, max], inclusive. */
  int(min: number, max: number): number;
}

export function createRng(seed: number): Rng {
  const next = mulberry32(seed);
  return {
    next,
    range: (min, max) => min + (max - min) * next(),
    int: (min, max) => min + Math.floor(next() * (max - min + 1)),
  };
}
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/generator/prng.test.ts`
Expected: 4 passed

- [ ] **Step 5: 提交**

```bash
git add src/generator/prng.ts tests/generator/prng.test.ts
git commit -m "feat(adas): add seedable mulberry32 PRNG

Refs #69"
```

---

### Task 4: 规则引擎核心

**Files:**
- Create: `src/engine/types.ts`, `src/engine/engine.ts`, `src/rules/boundary.ts`
- Test: `tests/engine/engine.test.ts`, `tests/helpers.ts`

- [ ] **Step 1: 写测试辅助 `tests/helpers.ts`**

```ts
import { createEngine, type EngineConfig } from '../src/engine/engine.ts';
import type { Anomaly, AnyRule } from '../src/engine/types.ts';
import type { AdasEvent } from '../src/schema/events.ts';

/** Run events through an engine with the given rules and flush at EOF. */
export function run(rules: readonly AnyRule[], events: readonly AdasEvent[], config?: EngineConfig): Anomaly[] {
  const engine = createEngine(rules, config);
  return [...events.flatMap((e) => engine.push(e)), ...engine.end()];
}

/** Compact view for assertions. */
export const brief = (as: readonly Anomaly[]) =>
  as.map((a) => ({ ruleId: a.ruleId, scenarioId: a.scenarioId, startTs: a.startTs, endTs: a.endTs, detectTs: a.detectTs }));
```

- [ ] **Step 2: 写失败测试 `tests/engine/engine.test.ts`**（用一个探针规则验证引擎行为）

```ts
import { describe, expect, it } from 'vitest';
import { createEngine } from '../../src/engine/engine.ts';
import { anomaly, type Rule } from '../../src/engine/types.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { triplet } from '../../src/rules/boundary.ts';

type ProbeState = { seen: number; nows: number[] };
type ProbeCfg = { limit: number };

/** Emits one anomaly per scenario at end: evidence = events seen and observed `now` values. */
const probe: Rule<ProbeState, ProbeCfg> = {
  id: 'PROBE', module: 'SYS', severity: 'P2', title: 'probe',
  defaults: { limit: 1 },
  createState: () => ({ seen: 0, nows: [] }),
  onEvent(_e, ctx) {
    ctx.state.seen++;
    ctx.state.nows.push(ctx.now);
    return [];
  },
  onScenarioEnd(ctx) {
    return [anomaly(this, ctx, { startTs: 0, endTs: ctx.now, message: 'end', evidence: { seen: ctx.state.seen, nows: ctx.state.nows, limit: ctx.cfg.limit } })];
  },
  boundaryCases: () => [],
};

const brake = (ts: number, id: string) => ev('ACC/BRAKE_PEDAL', ts, id, { pressed: false });

describe('createEngine', () => {
  it('isolates state per scenario and releases it on SCENARIO_END', () => {
    const engine = createEngine([probe]);
    engine.push(brake(0, 'A'));
    engine.push(brake(5, 'B'));
    engine.push(brake(10, 'A'));
    expect(engine.activeScenarios()).toBe(2);
    const out = engine.push(scenarioEnd('A', 20));
    expect(out).toHaveLength(1);
    expect(out[0]).toMatchObject({ scenarioId: 'A', detectTs: 20, evidence: { seen: 3 } });
    expect(engine.activeScenarios()).toBe(1);
  });

  it('flushes unfinished scenarios at EOF', () => {
    const engine = createEngine([probe]);
    engine.push(brake(0, 'A'));
    const out = engine.end();
    expect(out.map((a) => a.scenarioId)).toEqual(['A']);
    expect(engine.activeScenarios()).toBe(0);
  });

  it('never moves scenario time backwards on out-of-order events', () => {
    const engine = createEngine([probe]);
    engine.push(brake(100, 'A'));
    engine.push(brake(50, 'A'));
    const [a] = engine.end();
    expect(a?.evidence['nows']).toEqual([100, 100]);
  });

  it('merges rule config overrides over defaults', () => {
    const engine = createEngine([probe], { rules: { PROBE: { limit: 9 } } });
    engine.push(brake(0, 'A'));
    expect(engine.end()[0]?.evidence['limit']).toBe(9);
  });
});

describe('triplet', () => {
  it('builds minus / eq / plus cases and marks the trigger side', () => {
    const cases = triplet('X-01-dist', 'minus', (sign, caseId) => ({ description: `${sign}`, events: [scenarioEnd(caseId, 0)] }));
    expect(cases.map((c) => [c.caseId, c.expectTriggered, c.description])).toEqual([
      ['X-01-dist-minus', true, '-1'],
      ['X-01-dist-eq', false, '0'],
      ['X-01-dist-plus', false, '1'],
    ]);
  });
});
```

- [ ] **Step 3: 运行，确认失败**

Run: `npx vitest run tests/engine`
Expected: FAIL — cannot resolve `engine.ts`

- [ ] **Step 4: 实现 `src/engine/types.ts`**

```ts
import type { AdasEvent, Module } from '../schema/events.ts';

export type Severity = 'P1' | 'P2';

export interface Anomaly {
  ruleId: string;
  module: Module;
  severity: Severity;
  scenarioId: string;
  /** Event that started the judgment (threshold rules: first hit). */
  startTs: number;
  /** Threshold rules: last hit; window rules: deadline; unfinished flows: SCENARIO_END ts. */
  endTs: number;
  /** Scenario time when the rule decided. */
  detectTs: number;
  message: string;
  evidence: Record<string, unknown>;
}

export interface Epsilon { distance: number; time: number; torque: number }
export const DEFAULT_EPSILON: Epsilon = { distance: 0.01, time: 1, torque: 0.01 };

export interface RuleCtx<S, C> {
  scenarioId: string;
  /** Max ts seen in this scenario (never moves backwards). */
  now: number;
  state: S;
  cfg: C;
}

export interface BoundaryCase {
  caseId: string;
  description: string;
  /** Complete scenario incl. preconditions and SCENARIO_END. */
  events: AdasEvent[];
  expectTriggered: boolean;
}

export interface RuleMeta { readonly id: string; readonly module: Module; readonly severity: Severity }

export interface Rule<S, C extends object> extends RuleMeta {
  readonly title: string;
  readonly defaults: C;
  createState(): S;
  onEvent(e: AdasEvent, ctx: RuleCtx<S, C>): Anomaly[];
  onScenarioEnd(ctx: RuleCtx<S, C>): Anomaly[];
  boundaryCases(cfg: C, eps: Epsilon): BoundaryCase[];
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type AnyRule = Rule<any, any>;

export function anomaly(
  meta: RuleMeta,
  ctx: { scenarioId: string; now: number },
  f: { startTs: number; endTs: number; message: string; evidence?: Record<string, unknown> },
): Anomaly {
  return {
    ruleId: meta.id, module: meta.module, severity: meta.severity, scenarioId: ctx.scenarioId,
    startTs: f.startTs, endTs: f.endTs, detectTs: ctx.now, message: f.message, evidence: f.evidence ?? {},
  };
}
```

- [ ] **Step 5: 实现 `src/engine/engine.ts`**

```ts
import type { AdasEvent } from '../schema/events.ts';
import { DEFAULT_EPSILON, type Anomaly, type AnyRule, type Epsilon } from './types.ts';

export interface EngineConfig {
  rules?: Record<string, Record<string, number>>;
  epsilon?: Partial<Epsilon>;
}

export function resolveRuleConfig(rule: AnyRule, config: EngineConfig): Record<string, number> {
  return { ...rule.defaults, ...(config.rules?.[rule.id] ?? {}) };
}

export function resolveEpsilon(config: EngineConfig): Epsilon {
  return { ...DEFAULT_EPSILON, ...config.epsilon };
}

export interface Engine {
  push(e: AdasEvent): Anomaly[];
  /** EOF fallback: finish all scenarios still open. */
  end(): Anomaly[];
  activeScenarios(): number;
}

interface ScenarioState { now: number; states: Map<string, unknown> }

export function createEngine(rules: readonly AnyRule[], config: EngineConfig = {}): Engine {
  const cfgs = new Map(rules.map((r) => [r.id, resolveRuleConfig(r, config)] as const));
  const scenarios = new Map<string, ScenarioState>();

  const ctx = (scenarioId: string, sc: ScenarioState, r: AnyRule) => ({
    scenarioId, now: sc.now, state: sc.states.get(r.id), cfg: cfgs.get(r.id),
  });

  const open = (scenarioId: string, ts: number): ScenarioState => {
    const existing = scenarios.get(scenarioId);
    if (existing) return existing;
    const created = { now: ts, states: new Map(rules.map((r) => [r.id, r.createState()] as const)) };
    scenarios.set(scenarioId, created);
    return created;
  };

  const finish = (scenarioId: string, sc: ScenarioState): Anomaly[] => {
    const out = rules.flatMap((r) => r.onScenarioEnd(ctx(scenarioId, sc, r)));
    scenarios.delete(scenarioId);
    return out;
  };

  return {
    push(e) {
      const sc = open(e.scenarioId, e.ts);
      sc.now = Math.max(sc.now, e.ts);
      const out = rules.flatMap((r) => r.onEvent(e, ctx(e.scenarioId, sc, r)));
      if (e.module === 'SYS' && e.type === 'SCENARIO_END') out.push(...finish(e.scenarioId, sc));
      return out;
    },
    end: () => [...scenarios.entries()].flatMap(([id, sc]) => finish(id, sc)),
    activeScenarios: () => scenarios.size,
  };
}
```

- [ ] **Step 6: 实现 `src/rules/boundary.ts`**

```ts
import type { AdasEvent } from '../schema/events.ts';
import type { BoundaryCase } from '../engine/types.ts';

const SIDES = [['minus', -1], ['eq', 0], ['plus', 1]] as const;

/** Three boundary cases (threshold −ε / exact / +ε); `triggerSide` is the only side expected to fire. */
export function triplet(
  prefix: string,
  triggerSide: 'minus' | 'plus',
  build: (sign: -1 | 0 | 1, caseId: string) => { description: string; events: AdasEvent[] },
): BoundaryCase[] {
  return SIDES.map(([tag, sign]) => {
    const caseId = `${prefix}-${tag}`;
    return { caseId, expectTriggered: tag === triggerSide, ...build(sign, caseId) };
  });
}
```

- [ ] **Step 7: 运行测试与类型检查**

Run: `npx vitest run tests/engine && npm run typecheck && npm run lint`
Expected: 5 passed；无类型或 lint 错误

- [ ] **Step 8: 提交**

```bash
git add src/engine src/rules/boundary.ts tests/engine tests/helpers.ts
git commit -m "feat(adas): add rule engine core with per-scenario state

Refs #69"
```

---

### Task 5: 逐行读取与数据质量

**Files:**
- Create: `src/io/jsonl.ts`, `src/parser/scan.ts`, `fixtures/bad-lines.jsonl`
- Test: `tests/parser/scan.test.ts`

- [ ] **Step 1: 创建 `fixtures/bad-lines.jsonl`**（第 5 行是空行，保持原样）

```
{"ts":0,"scenarioId":"B1","module":"ACC","type":"STATE","data":{"from":"OFF","to":"ACTIVE","reason":"x"}}
{not json
{"ts":10,"scenarioId":"B1","module":"ACC","type":"TRACK","data":{"egoSpeed":"fast","setSpeed":1,"leadDistance":null,"leadRelSpeed":0}}
{"ts":20,"scenarioId":"B1","module":"RADAR","type":"POINTS","data":{}}

{"ts":50,"scenarioId":"B1","module":"ACC","type":"BRAKE_PEDAL","data":{"pressed":false}}
{"ts":40,"scenarioId":"B1","module":"ACC","type":"BRAKE_PEDAL","data":{"pressed":false}}
{"ts":-1,"scenarioId":"B1","module":"ACC","type":"BRAKE_PEDAL","data":{"pressed":false}}
{"ts":60,"scenarioId":"B1","module":"SYS","type":"SCENARIO_END","data":{}}
```

- [ ] **Step 2: 写失败测试 `tests/parser/scan.test.ts`**

```ts
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { readLines, toJsonl } from '../../src/io/jsonl.ts';
import { hasQualityIssues, scanLines } from '../../src/parser/scan.ts';
import type { AdasEvent } from '../../src/schema/events.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';

const fixture = fileURLToPath(new URL('../../fixtures/bad-lines.jsonl', import.meta.url));

describe('scanLines', () => {
  it('skips and records bad lines without stopping', async () => {
    const seen: AdasEvent[] = [];
    const r = await scanLines(readLines(fixture), (e) => seen.push(e));
    expect(seen.map((e) => e.ts)).toEqual([0, 50, 40, 60]);
    expect(r.events).toBe(4);
    expect(r.scenarios).toBe(1);
    expect(r.quality.invalidJson).toEqual([{ line: 2, detail: 'invalid JSON' }]);
    expect(r.quality.schemaErrors.map((i) => i.line)).toEqual([3, 8]);
    expect(r.quality.schemaErrors[0]?.detail).toContain('data.egoSpeed');
    expect(r.quality.unknownEvents).toEqual({ 'RADAR/POINTS': 1 });
    expect(r.quality.outOfOrder).toEqual([{ line: 7, scenarioId: 'B1', ts: 40, prevTs: 50 }]);
    expect(r.quality.counts).toEqual({ invalidJson: 1, schemaErrors: 2, unknownEvents: 1, outOfOrder: 1 });
    expect(hasQualityIssues(r.quality)).toBe(true);
  });

  it('reports a clean input as having no issues', async () => {
    const r = await scanLines([JSON.stringify(scenarioEnd('S', 0))], () => {});
    expect(hasQualityIssues(r.quality)).toBe(false);
    expect(r.events).toBe(1);
  });

  it('handles empty input', async () => {
    const r = await scanLines([], () => {});
    expect(r).toMatchObject({ events: 0, scenarios: 0 });
  });

  it('treats over-long lines as invalid', async () => {
    const r = await scanLines(['x'.repeat(200)], () => {}, { maxLineBytes: 100 });
    expect(r.quality.invalidJson).toEqual([{ line: 1, detail: 'line exceeds 100 bytes' }]);
  });

  it('caps issue details but keeps counting', async () => {
    const r = await scanLines(['{', '{', '{'], () => {}, { maxIssues: 2 });
    expect(r.quality.invalidJson).toHaveLength(2);
    expect(r.quality.counts.invalidJson).toBe(3);
  });

  it('counts interleaved scenarios as separate instances', async () => {
    const lines = toJsonl([
      ev('ACC/BRAKE_PEDAL', 0, 'A', { pressed: false }),
      ev('ACC/BRAKE_PEDAL', 0, 'B', { pressed: false }),
      scenarioEnd('A', 10),
      scenarioEnd('B', 10),
    ]).trimEnd().split('\n');
    const r = await scanLines(lines, () => {});
    expect(r.scenarios).toBe(2);
    expect(r.quality.counts.outOfOrder).toBe(0);
  });
});
```

- [ ] **Step 3: 运行，确认失败**

Run: `npx vitest run tests/parser/scan.test.ts`
Expected: FAIL — cannot resolve `jsonl.ts`

- [ ] **Step 4: 实现 `src/io/jsonl.ts`**

```ts
import { createReadStream } from 'node:fs';
import { createInterface } from 'node:readline';

/** Stream a file line by line without loading it into memory. */
export function readLines(path: string): AsyncIterable<string> {
  return createInterface({ input: createReadStream(path, { encoding: 'utf8' }), crlfDelay: Infinity });
}

export function toJsonl(records: readonly unknown[]): string {
  return records.length === 0 ? '' : `${records.map((r) => JSON.stringify(r)).join('\n')}\n`;
}
```

- [ ] **Step 5: 实现 `src/parser/scan.ts`**

```ts
import type { AdasEvent } from '../schema/events.ts';
import { parseLine } from '../schema/validate.ts';

export interface LineIssue { line: number; detail: string }
export interface OutOfOrder { line: number; scenarioId: string; ts: number; prevTs: number }

export interface DataQuality {
  counts: { invalidJson: number; schemaErrors: number; unknownEvents: number; outOfOrder: number };
  invalidJson: LineIssue[];
  schemaErrors: LineIssue[];
  unknownEvents: Record<string, number>;
  outOfOrder: OutOfOrder[];
}

export interface ScanOptions { maxLineBytes?: number; maxIssues?: number }
export interface ScanResult { quality: DataQuality; events: number; scenarios: number }

export const DEFAULT_MAX_LINE_BYTES = 1024 * 1024;
export const DEFAULT_MAX_ISSUES = 100;

export function hasQualityIssues(q: DataQuality): boolean {
  return Object.values(q.counts).some((n) => n > 0);
}

/**
 * Read lines, validate, record data quality issues, and hand valid events to `onEvent`.
 * Blank lines are ignored. `scenarios` counts scenario instances (an id reused after
 * SCENARIO_END counts again), so per-scenario bookkeeping is released at scenario end.
 */
export async function scanLines(
  lines: AsyncIterable<string> | Iterable<string>,
  onEvent: (e: AdasEvent) => void,
  opts: ScanOptions = {},
): Promise<ScanResult> {
  const maxLineBytes = opts.maxLineBytes ?? DEFAULT_MAX_LINE_BYTES;
  const maxIssues = opts.maxIssues ?? DEFAULT_MAX_ISSUES;
  const quality: DataQuality = {
    counts: { invalidJson: 0, schemaErrors: 0, unknownEvents: 0, outOfOrder: 0 },
    invalidJson: [], schemaErrors: [], unknownEvents: {}, outOfOrder: [],
  };
  const keep = <T>(list: T[], item: T) => { if (list.length < maxIssues) list.push(item); };
  const lastTs = new Map<string, number>();
  let lineNo = 0, events = 0, scenarios = 0;

  for await (const raw of lines) {
    lineNo++;
    if (raw.trim() === '') continue;
    if (Buffer.byteLength(raw, 'utf8') > maxLineBytes) {
      quality.counts.invalidJson++;
      keep(quality.invalidJson, { line: lineNo, detail: `line exceeds ${maxLineBytes} bytes` });
      continue;
    }
    const r = parseLine(raw);
    if (!r.ok) {
      if (r.kind === 'unknownEvent') {
        quality.counts.unknownEvents++;
        quality.unknownEvents[r.detail] = (quality.unknownEvents[r.detail] ?? 0) + 1;
      } else {
        const key = r.kind === 'invalidJson' ? 'invalidJson' : 'schemaErrors';
        quality.counts[key]++;
        keep(quality[key], { line: lineNo, detail: r.detail });
      }
      continue;
    }
    const e = r.event;
    const prev = lastTs.get(e.scenarioId);
    if (prev === undefined) scenarios++;
    else if (e.ts < prev) {
      quality.counts.outOfOrder++;
      keep(quality.outOfOrder, { line: lineNo, scenarioId: e.scenarioId, ts: e.ts, prevTs: prev });
    }
    lastTs.set(e.scenarioId, Math.max(prev ?? e.ts, e.ts));
    if (e.module === 'SYS' && e.type === 'SCENARIO_END') lastTs.delete(e.scenarioId);
    events++;
    onEvent(e);
  }
  return { quality, events, scenarios };
}
```

- [ ] **Step 6: 运行测试**

Run: `npx vitest run tests/parser/scan.test.ts && npm run typecheck`
Expected: 6 passed

- [ ] **Step 7: 提交**

```bash
git add src/io src/parser/scan.ts fixtures/bad-lines.jsonl tests/parser/scan.test.ts
git commit -m "feat(adas): add streaming line scanner with data quality report

Refs #69"
```

---

### Task 6a: 规则 ACC-01 跟车距离过近

**Files:**
- Create: `src/rules/acc-01.ts`
- Test: `tests/rules/acc-01.test.ts`

判定：ACC 为 ACTIVE 时 `leadDistance < max(minGapM, egoSpeed × timeGapS)`；`leadDistance = null` 跳过；相邻命中间隔 ≤ `mergeGapMs` 合并；离开 ACTIVE、命中中断超过 `mergeGapMs`、场景结束时输出。

- [ ] **Step 1: 写失败测试**

```ts
import { describe, expect, it } from 'vitest';
import { acc01 } from '../../src/rules/acc-01.ts';
import { ev, scenarioEnd, type AdasEvent } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const active = (ts = 0) => ev('ACC/STATE', ts, S, { from: 'OFF', to: 'ACTIVE', reason: 't' });
const track = (ts: number, egoSpeed: number, leadDistance: number | null) =>
  ev('ACC/TRACK', ts, S, { egoSpeed, setSpeed: egoSpeed, leadDistance, leadRelSpeed: 0 });

describe('ACC-01 following distance too short', () => {
  it('does not fire when distance is safe', () => {
    expect(run([acc01], [active(), track(100, 20, 25), scenarioEnd(S, 200)])).toEqual([]);
  });

  it('merges consecutive hits into one anomaly and closes on recovery gap', () => {
    const events: AdasEvent[] = [active(), track(1000, 10, 7), track(1050, 10, 6.5), track(1500, 10, 20), scenarioEnd(S, 3000)];
    const out = run([acc01], events);
    expect(brief(out)).toEqual([{ ruleId: 'ACC-01', scenarioId: S, startTs: 1000, endTs: 1050, detectTs: 1500 }]);
    expect(out[0]?.evidence).toEqual({ minDistance: 6.5, threshold: 10 });
    expect(out[0]?.message).toBe('Following distance 6.50 m below threshold 10.00 m');
  });

  it('uses the 5 m floor at low speed', () => {
    expect(run([acc01], [active(), track(100, 3, 4.9), scenarioEnd(S, 200)])).toHaveLength(1);
    expect(run([acc01], [active(), track(100, 3, 5), scenarioEnd(S, 200)])).toHaveLength(0);
  });

  it('ignores frames when not ACTIVE or without a lead vehicle', () => {
    const standby = ev('ACC/STATE', 0, S, { from: 'OFF', to: 'STANDBY', reason: 't' });
    expect(run([acc01], [standby, track(100, 20, 1), scenarioEnd(S, 200)])).toEqual([]);
    expect(run([acc01], [active(), track(100, 20, null), scenarioEnd(S, 200)])).toEqual([]);
  });

  it('closes the episode when ACC leaves ACTIVE', () => {
    const off = ev('ACC/STATE', 1100, S, { from: 'ACTIVE', to: 'STANDBY', reason: 't' });
    const out = run([acc01], [active(), track(1000, 20, 10), off, scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'ACC-01', scenarioId: S, startTs: 1000, endTs: 1000, detectTs: 1100 }]);
  });

  it('splits hits separated by more than mergeGapMs', () => {
    const out = run([acc01], [active(), track(1000, 20, 10), track(1300, 20, 10), scenarioEnd(S, 2000)]);
    expect(out.map((a) => [a.startTs, a.endTs])).toEqual([[1000, 1000], [1300, 1300]]);
  });

  it('reports an open episode at scenario end', () => {
    expect(brief(run([acc01], [active(), track(1000, 20, 10), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'ACC-01', scenarioId: S, startTs: 1000, endTs: 1000, detectTs: 1100 },
    ]);
  });

  it('honours config overrides', () => {
    const events = [active(), track(100, 20, 25), scenarioEnd(S, 200)];
    expect(run([acc01], events, { rules: { 'ACC-01': { timeGapS: 1.5 } } })).toHaveLength(1);
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/rules/acc-01.test.ts`
Expected: FAIL — cannot resolve `acc-01.ts`

- [ ] **Step 3: 实现 `src/rules/acc-01.ts`**

```ts
import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type AccState } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { minGapM: number; timeGapS: number; mergeGapMs: number };
type Episode = { startTs: number; lastHitTs: number; minDistance: number; threshold: number };
type State = { acc: AccState; open: Episode | null };

const meta = { id: 'ACC-01', module: 'ACC', severity: 'P1' } as const;

function close(ctx: RuleCtx<State, Cfg>): Anomaly[] {
  const o = ctx.state.open;
  if (!o) return [];
  ctx.state.open = null;
  return [anomaly(meta, ctx, {
    startTs: o.startTs,
    endTs: o.lastHitTs,
    message: `Following distance ${o.minDistance.toFixed(2)} m below threshold ${o.threshold.toFixed(2)} m`,
    evidence: { minDistance: o.minDistance, threshold: o.threshold },
  })];
}

export const acc01: Rule<State, Cfg> = {
  ...meta,
  title: 'Following distance too short',
  defaults: { minGapM: 5, timeGapS: 1.0, mergeGapMs: 200 },
  createState: () => ({ acc: 'OFF', open: null }),

  onEvent(e, ctx) {
    if (e.module === 'ACC' && e.type === 'STATE') {
      ctx.state.acc = e.data.to;
      return e.data.to === 'ACTIVE' ? [] : close(ctx);
    }
    if (e.module !== 'ACC' || e.type !== 'TRACK') return [];
    const out: Anomaly[] = [];
    const open = ctx.state.open;
    if (open && e.ts - open.lastHitTs > ctx.cfg.mergeGapMs) out.push(...close(ctx));
    const d = e.data.leadDistance;
    if (ctx.state.acc !== 'ACTIVE' || d === null) return out;
    const threshold = Math.max(ctx.cfg.minGapM, e.data.egoSpeed * ctx.cfg.timeGapS);
    if (d >= threshold) return out;
    const cur = ctx.state.open;
    if (cur) {
      cur.lastHitTs = e.ts;
      cur.minDistance = Math.min(cur.minDistance, d);
    } else {
      ctx.state.open = { startTs: e.ts, lastHitTs: e.ts, minDistance: d, threshold };
    }
    return out;
  },

  onScenarioEnd: (ctx) => close(ctx),

  boundaryCases(cfg, eps) {
    return [{ name: 'lowspeed', speed: 3 }, { name: 'highspeed', speed: 20 }].flatMap(({ name, speed }) => {
      const threshold = Math.max(cfg.minGapM, speed * cfg.timeGapS);
      return triplet(`ACC-01-${name}`, 'minus', (sign, id) => {
        const lead = threshold + sign * eps.distance;
        return {
          description: `lead distance ${lead.toFixed(2)} m vs threshold ${threshold.toFixed(2)} m at ${speed} m/s`,
          events: [
            ev('ACC/STATE', 0, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
            ev('ACC/TRACK', 1000, id, { egoSpeed: speed, setSpeed: speed, leadDistance: lead, leadRelSpeed: 0 }),
            scenarioEnd(id, 2000),
          ],
        };
      });
    });
  },
};
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/rules/acc-01.test.ts && npm run typecheck`
Expected: 8 passed

- [ ] **Step 5: 提交**

```bash
git add src/rules/acc-01.ts tests/rules/acc-01.test.ts
git commit -m "feat(adas): add ACC-01 following distance rule

Refs #69"
```

---

### Task 6b: 规则 ACC-02 踩刹车后未退出

**Files:**
- Create: `src/rules/acc-02.ts`
- Test: `tests/rules/acc-02.test.ts`

判定：ACTIVE 时 `BRAKE_PEDAL pressed=true` 后 `exitWindowMs`（200 ms）内未切到 STANDBY / OFF。`startTs` = 踩刹车时刻，`endTs` = 截止时间。

- [ ] **Step 1: 写失败测试**

```ts
import { describe, expect, it } from 'vitest';
import { acc02 } from '../../src/rules/acc-02.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const state = (ts: number, from: 'OFF' | 'STANDBY' | 'ACTIVE', to: 'OFF' | 'STANDBY' | 'ACTIVE') =>
  ev('ACC/STATE', ts, S, { from, to, reason: 't' });
const brake = (ts: number, pressed = true) => ev('ACC/BRAKE_PEDAL', ts, S, { pressed });
const tick = (ts: number) => ev('ACC/TRACK', ts, S, { egoSpeed: 10, setSpeed: 10, leadDistance: null, leadRelSpeed: 0 });

describe('ACC-02 no exit after brake pedal', () => {
  it('does not fire when ACC exits in time', () => {
    expect(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), state(1100, 'ACTIVE', 'STANDBY'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires once the deadline passes without exit', () => {
    const out = run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), tick(1200), tick(1250), state(1500, 'ACTIVE', 'STANDBY'), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'ACC-02', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1250 }]);
    expect(out[0]?.message).toBe('ACC did not exit within 200 ms after brake pedal');
  });

  it('treats an exit exactly at the deadline as on time', () => {
    expect(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), state(1200, 'ACTIVE', 'OFF'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('ignores brake release and brakes while not ACTIVE', () => {
    expect(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000, false), scenarioEnd(S, 2000)])).toEqual([]);
    expect(run([acc02], [state(0, 'OFF', 'STANDBY'), brake(1000), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires at scenario end if still pending', () => {
    expect(brief(run([acc02], [state(0, 'OFF', 'ACTIVE'), brake(1000), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'ACC-02', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1100 },
    ]);
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/rules/acc-02.test.ts`
Expected: FAIL — cannot resolve `acc-02.ts`

- [ ] **Step 3: 实现 `src/rules/acc-02.ts`**

```ts
import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type AccState } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { exitWindowMs: number };
type Pending = { startTs: number; deadline: number };
type State = { acc: AccState; pending: Pending | null };

const meta = { id: 'ACC-02', module: 'ACC', severity: 'P1' } as const;

const report = (ctx: RuleCtx<State, Cfg>, p: Pending): Anomaly =>
  anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: `ACC did not exit within ${ctx.cfg.exitWindowMs} ms after brake pedal`,
  });

export const acc02: Rule<State, Cfg> = {
  ...meta,
  title: 'No exit after brake pedal',
  defaults: { exitWindowMs: 200 },
  createState: () => ({ acc: 'OFF', pending: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const s = ctx.state;
    if (s.pending && ctx.now > s.pending.deadline) {
      out.push(report(ctx, s.pending));
      s.pending = null;
    }
    if (e.module !== 'ACC') return out;
    if (e.type === 'STATE') {
      s.acc = e.data.to;
      if (e.data.to === 'STANDBY' || e.data.to === 'OFF') s.pending = null;
    } else if (e.type === 'BRAKE_PEDAL' && e.data.pressed && s.acc === 'ACTIVE' && !s.pending) {
      s.pending = { startTs: e.ts, deadline: e.ts + ctx.cfg.exitWindowMs };
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const p = ctx.state.pending;
    ctx.state.pending = null;
    return p ? [report(ctx, p)] : [];
  },

  boundaryCases(cfg, eps) {
    return triplet('ACC-02-window', 'plus', (sign, id) => {
      const exitTs = 1000 + cfg.exitWindowMs + sign * eps.time;
      return {
        description: `ACC exits ${exitTs - 1000} ms after brake pedal (window ${cfg.exitWindowMs} ms)`,
        events: [
          ev('ACC/STATE', 0, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
          ev('ACC/BRAKE_PEDAL', 1000, id, { pressed: true }),
          ev('ACC/STATE', exitTs, id, { from: 'ACTIVE', to: 'STANDBY', reason: 'brake' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
  },
};
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/rules/acc-02.test.ts && npm run typecheck`
Expected: 5 passed

- [ ] **Step 5: 提交**

```bash
git add src/rules/acc-02.ts tests/rules/acc-02.test.ts
git commit -m "feat(adas): add ACC-02 brake exit rule

Refs #69"
```

---

### Task 6c: 规则 APA-01 障碍物过近未中止

**Files:**
- Create: `src/rules/apa-01.ts`
- Test: `tests/rules/apa-01.test.ts`

判定：MANEUVERING 时 `minDistance < minDistanceM`（0.3 m）开始一个"过近片段"；片段开始后 `abortWindowMs`（200 ms）内未 ABORTED 即异常。距离恢复只结束片段，不取消待确认。

- [ ] **Step 1: 写失败测试**

```ts
import { describe, expect, it } from 'vitest';
import { apa01 } from '../../src/rules/apa-01.ts';
import { ev, scenarioEnd, type ApaStep } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const step = (ts: number, s: ApaStep) => ev('APA/STEP', ts, S, { step: s, reason: 't' });
const obs = (ts: number, minDistance: number) => ev('APA/OBSTACLE', ts, S, { minDistance });

describe('APA-01 obstacle too close without abort', () => {
  it('does not fire when aborted in time', () => {
    expect(run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.2), step(1150, 'ABORTED'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires after the window and tracks the minimum distance', () => {
    const out = run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.25), obs(1100, 0.2), obs(1300, 0.9), step(1400, 'ABORTED'), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-01', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1300 }]);
    expect(out[0]?.evidence).toEqual({ minDistance: 0.2 });
  });

  it('ignores obstacles outside MANEUVERING and safe distances', () => {
    expect(run([apa01], [step(0, 'SEARCHING'), obs(1000, 0.1), scenarioEnd(S, 2000)])).toEqual([]);
    expect(run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.3), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('keeps the first pending when a new episode starts inside the window', () => {
    const out = run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.2), obs(1050, 0.5), obs(1100, 0.1), scenarioEnd(S, 3000)]);
    expect(out.map((a) => [a.startTs, a.evidence['minDistance']])).toEqual([[1000, 0.1]]);
  });

  it('reports once for a close episode that continues past the deadline', () => {
    const frames = [1000, 1100, 1200, 1300, 1400].map((t) => obs(t, 0.2));
    const out = run([apa01], [step(0, 'MANEUVERING'), ...frames, step(1450, 'ABORTED'), scenarioEnd(S, 3000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-01', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1300 }]);
  });

  it('fires at scenario end if still pending', () => {
    expect(brief(run([apa01], [step(0, 'MANEUVERING'), obs(1000, 0.2), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'APA-01', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1100 },
    ]);
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/rules/apa-01.test.ts`
Expected: FAIL — cannot resolve `apa-01.ts`

- [ ] **Step 3: 实现 `src/rules/apa-01.ts`**

```ts
import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type ApaStep } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { minDistanceM: number; abortWindowMs: number };
type Pending = { startTs: number; deadline: number; minDistance: number };
type State = { step: ApaStep | null; inEpisode: boolean; pending: Pending | null };

const meta = { id: 'APA-01', module: 'APA', severity: 'P1' } as const;

const report = (ctx: RuleCtx<State, Cfg>, p: Pending): Anomaly =>
  anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: `Obstacle at ${p.minDistance.toFixed(2)} m below ${ctx.cfg.minDistanceM} m without abort within ${ctx.cfg.abortWindowMs} ms`,
    evidence: { minDistance: p.minDistance },
  });

export const apa01: Rule<State, Cfg> = {
  ...meta,
  title: 'Obstacle too close without abort',
  defaults: { minDistanceM: 0.3, abortWindowMs: 200 },
  createState: () => ({ step: null, inEpisode: false, pending: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const s = ctx.state;
    if (s.pending && ctx.now > s.pending.deadline) {
      out.push(report(ctx, s.pending));
      s.pending = null;
    }
    if (e.module !== 'APA') return out;
    if (e.type === 'STEP') {
      s.step = e.data.step;
      if (e.data.step === 'ABORTED') s.pending = null;
      if (e.data.step !== 'MANEUVERING') s.inEpisode = false;
      return out;
    }
    if (s.step !== 'MANEUVERING') return out;
    const d = e.data.minDistance;
    if (d >= ctx.cfg.minDistanceM) {
      s.inEpisode = false;
      return out;
    }
    if (!s.inEpisode) {
      s.inEpisode = true;
      s.pending ??= { startTs: e.ts, deadline: e.ts + ctx.cfg.abortWindowMs, minDistance: d };
    }
    // pending is null when this episode already expired and was reported.
    if (s.pending) s.pending.minDistance = Math.min(s.pending.minDistance, d);
    return out;
  },

  onScenarioEnd(ctx) {
    const p = ctx.state.pending;
    ctx.state.pending = null;
    return p ? [report(ctx, p)] : [];
  },

  boundaryCases(cfg, eps) {
    const distance = triplet('APA-01-distance', 'minus', (sign, id) => {
      const d = cfg.minDistanceM + sign * eps.distance;
      return {
        description: `obstacle at ${d.toFixed(2)} m vs ${cfg.minDistanceM} m, never aborted`,
        events: [
          ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }),
          ev('APA/OBSTACLE', 1000, id, { minDistance: d }),
          ev('APA/OBSTACLE', 1100, id, { minDistance: 0.8 }),
          ev('APA/STEP', 5000, id, { step: 'COMPLETED', reason: 'parked' }),
          scenarioEnd(id, 6000),
        ],
      };
    });
    const window = triplet('APA-01-window', 'plus', (sign, id) => {
      const abortTs = 1000 + cfg.abortWindowMs + sign * eps.time;
      return {
        description: `abort ${abortTs - 1000} ms after obstacle (window ${cfg.abortWindowMs} ms)`,
        events: [
          ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }),
          ev('APA/OBSTACLE', 1000, id, { minDistance: 0.2 }),
          ev('APA/STEP', abortTs, id, { step: 'ABORTED', reason: 'obstacle' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
    return [...distance, ...window];
  },
};
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/rules/apa-01.test.ts && npm run typecheck`
Expected: 6 passed

- [ ] **Step 5: 提交**

```bash
git add src/rules/apa-01.ts tests/rules/apa-01.test.ts
git commit -m "feat(adas): add APA-01 obstacle abort rule

Refs #69"
```

---

### Task 6d: 规则 APA-02 泊车超时

**Files:**
- Create: `src/rules/apa-02.ts`
- Test: `tests/rules/apa-02.test.ts`

判定：MANEUVERING → COMPLETED / ABORTED 超过 `timeoutMs`（60 s）报超时（`endTs` = 截止）；场景结束仍未到终态且未报过超时，报"未完成"（`endTs` = 场景结束时刻）。每次泊车最多一条。

- [ ] **Step 1: 写失败测试**

```ts
import { describe, expect, it } from 'vitest';
import { apa02 } from '../../src/rules/apa-02.ts';
import { ev, scenarioEnd, type ApaStep } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const step = (ts: number, s: ApaStep) => ev('APA/STEP', ts, S, { step: s, reason: 't' });
const obs = (ts: number) => ev('APA/OBSTACLE', ts, S, { minDistance: 1 });

describe('APA-02 parking timeout', () => {
  it('does not fire when completed in time (exactly at the deadline is on time)', () => {
    expect(run([apa02], [step(0, 'MANEUVERING'), step(60000, 'COMPLETED'), scenarioEnd(S, 61000)])).toEqual([]);
  });

  it('fires once on timeout and not again at completion', () => {
    const out = run([apa02], [step(4000, 'MANEUVERING'), obs(64100), step(69000, 'COMPLETED'), scenarioEnd(S, 71000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-02', scenarioId: S, startTs: 4000, endTs: 64000, detectTs: 64100 }]);
    expect(out[0]?.message).toBe('Parking maneuver exceeded 60000 ms');
  });

  it('reports an unfinished maneuver at scenario end', () => {
    const out = run([apa02], [step(4000, 'MANEUVERING'), scenarioEnd(S, 30000)]);
    expect(brief(out)).toEqual([{ ruleId: 'APA-02', scenarioId: S, startTs: 4000, endTs: 30000, detectTs: 30000 }]);
    expect(out[0]?.message).toBe('Parking maneuver unfinished at scenario end');
  });

  it('reports timeout (not unfinished) when the scenario ends after the deadline', () => {
    const out = run([apa02], [step(0, 'MANEUVERING')], undefined);
    expect(out[0]?.message).toBe('Parking maneuver unfinished at scenario end');
    const late = run([apa02], [step(0, 'MANEUVERING'), scenarioEnd(S, 70000)]);
    expect(late.map((a) => [a.endTs, a.message])).toEqual([[60000, 'Parking maneuver exceeded 60000 ms']]);
  });

  it('does not report twice when timed out and never completed', () => {
    expect(run([apa02], [step(0, 'MANEUVERING'), obs(60100), scenarioEnd(S, 70000)])).toHaveLength(1);
  });

  it('keeps the first start on repeated MANEUVERING and resets after ABORTED', () => {
    const out = run([apa02], [step(0, 'MANEUVERING'), step(100, 'MANEUVERING'), step(200, 'ABORTED'), scenarioEnd(S, 70000)]);
    expect(out).toEqual([]);
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/rules/apa-02.test.ts`
Expected: FAIL — cannot resolve `apa-02.ts`

- [ ] **Step 3: 实现 `src/rules/apa-02.ts`**

```ts
import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { timeoutMs: number };
type Maneuver = { startTs: number; deadline: number; reported: boolean };
type State = { maneuver: Maneuver | null };

const meta = { id: 'APA-02', module: 'APA', severity: 'P2' } as const;

const timeout = (ctx: RuleCtx<State, Cfg>, m: Maneuver): Anomaly =>
  anomaly(meta, ctx, { startTs: m.startTs, endTs: m.deadline, message: `Parking maneuver exceeded ${ctx.cfg.timeoutMs} ms` });

export const apa02: Rule<State, Cfg> = {
  ...meta,
  title: 'Parking timeout',
  defaults: { timeoutMs: 60000 },
  createState: () => ({ maneuver: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const m = ctx.state.maneuver;
    if (m && !m.reported && ctx.now > m.deadline) {
      out.push(timeout(ctx, m));
      m.reported = true;
    }
    if (e.module === 'APA' && e.type === 'STEP') {
      if (e.data.step === 'MANEUVERING') {
        ctx.state.maneuver ??= { startTs: e.ts, deadline: e.ts + ctx.cfg.timeoutMs, reported: false };
      } else if (e.data.step === 'COMPLETED' || e.data.step === 'ABORTED') {
        ctx.state.maneuver = null;
      }
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const m = ctx.state.maneuver;
    ctx.state.maneuver = null;
    if (!m || m.reported) return [];
    if (ctx.now > m.deadline) return [timeout(ctx, m)];
    return [anomaly(meta, ctx, { startTs: m.startTs, endTs: ctx.now, message: 'Parking maneuver unfinished at scenario end' })];
  },

  boundaryCases(cfg, eps) {
    const window = triplet('APA-02-timeout', 'plus', (sign, id) => {
      const doneTs = cfg.timeoutMs + sign * eps.time;
      return {
        description: `maneuver completes after ${doneTs} ms (timeout ${cfg.timeoutMs} ms)`,
        events: [
          ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }),
          ev('APA/STEP', doneTs, id, { step: 'COMPLETED', reason: 'parked' }),
          scenarioEnd(id, doneTs + 1000),
        ],
      };
    });
    const id = 'APA-02-unfinished';
    return [...window, {
      caseId: id,
      description: 'scenario ends while still maneuvering',
      expectTriggered: true,
      events: [ev('APA/STEP', 0, id, { step: 'MANEUVERING', reason: 'boundary' }), scenarioEnd(id, 10000)],
    }];
  },
};
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/rules/apa-02.test.ts && npm run typecheck`
Expected: 6 passed

- [ ] **Step 5: 提交**

```bash
git add src/rules/apa-02.ts tests/rules/apa-02.test.ts
git commit -m "feat(adas): add APA-02 parking timeout rule

Refs #69"
```

---

### Task 6e: 规则 HMI-01 显示与功能状态不一致

**Files:**
- Create: `src/rules/hmi-01.ts`
- Test: `tests/rules/hmi-01.test.ts`

判定：ACC `STATE` 或 APA `STEP` 变化后 `displayWindowMs`（300 ms）内，取窗口内最后一次 `DISPLAY(feature, STATE)`：没有，或 `value` ≠ 新状态 → 异常。到截止才判定；窗口内同一功能再次变化，前一个作废。场景结束时未满足的也判异常。

- [ ] **Step 1: 写失败测试**

```ts
import { describe, expect, it } from 'vitest';
import { hmi01 } from '../../src/rules/hmi-01.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const acc = (ts: number, to: 'STANDBY' | 'ACTIVE' | 'OVERRIDE') => ev('ACC/STATE', ts, S, { from: 'OFF', to, reason: 't' });
const show = (ts: number, feature: 'ACC' | 'APA', value: string) => ev('HMI/DISPLAY', ts, S, { feature, item: 'STATE', value });
const tick = (ts: number) => ev('ACC/BRAKE_PEDAL', ts, S, { pressed: false });

describe('HMI-01 HMI state mismatch', () => {
  it('passes when HMI shows the new state in time', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), show(1100, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires when nothing is shown within the window', () => {
    const out = run([hmi01], [acc(1000, 'ACTIVE'), tick(1350), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'HMI-01', scenarioId: S, startTs: 1000, endTs: 1300, detectTs: 1350 }]);
    expect(out[0]?.message).toBe('HMI did not show ACC state ACTIVE within 300 ms');
  });

  it('fires when the last shown value is wrong', () => {
    const out = run([hmi01], [acc(1000, 'OVERRIDE'), show(1100, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)]);
    expect(out[0]?.message).toBe('HMI showed ACC state ACTIVE, expected OVERRIDE');
    expect(out[0]?.evidence).toEqual({ feature: 'ACC', expected: 'OVERRIDE', shown: 'ACTIVE' });
  });

  it('uses the last display in the window', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), show(1050, 'ACC', 'STANDBY'), show(1200, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('ignores displays after the deadline and of other features', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), show(1100, 'APA', 'ACTIVE'), show(1301, 'ACC', 'ACTIVE'), scenarioEnd(S, 2000)])).toHaveLength(1);
  });

  it('supersedes a pending check when the feature changes again', () => {
    expect(run([hmi01], [acc(1000, 'ACTIVE'), acc(1100, 'STANDBY'), show(1200, 'ACC', 'STANDBY'), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('covers APA STEP changes', () => {
    const step = ev('APA/STEP', 0, S, { step: 'MANEUVERING', reason: 't' });
    expect(run([hmi01], [step, show(100, 'APA', 'MANEUVERING'), scenarioEnd(S, 1000)])).toEqual([]);
    expect(run([hmi01], [step, scenarioEnd(S, 1000)])).toHaveLength(1);
  });

  it('fires at scenario end when unmet', () => {
    expect(brief(run([hmi01], [acc(1000, 'ACTIVE'), scenarioEnd(S, 1100)]))).toEqual([
      { ruleId: 'HMI-01', scenarioId: S, startTs: 1000, endTs: 1300, detectTs: 1100 },
    ]);
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/rules/hmi-01.test.ts`
Expected: FAIL — cannot resolve `hmi-01.ts`

- [ ] **Step 3: 实现 `src/rules/hmi-01.ts`**

```ts
import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd, type AdasEvent, type HmiFeature } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { displayWindowMs: number };
type Pending = { expected: string; startTs: number; deadline: number; lastShown: string | null };
type State = { pending: Map<HmiFeature, Pending> };

const meta = { id: 'HMI-01', module: 'HMI', severity: 'P2' } as const;

function judge(ctx: RuleCtx<State, Cfg>, feature: HmiFeature, p: Pending): Anomaly[] {
  if (p.lastShown === p.expected) return [];
  return [anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: p.lastShown === null
      ? `HMI did not show ${feature} state ${p.expected} within ${ctx.cfg.displayWindowMs} ms`
      : `HMI showed ${feature} state ${p.lastShown}, expected ${p.expected}`,
    evidence: { feature, expected: p.expected, shown: p.lastShown },
  })];
}

function stateChange(e: AdasEvent): { feature: HmiFeature; value: string } | null {
  if (e.module === 'ACC' && e.type === 'STATE') return { feature: 'ACC', value: e.data.to };
  if (e.module === 'APA' && e.type === 'STEP') return { feature: 'APA', value: e.data.step };
  return null;
}

export const hmi01: Rule<State, Cfg> = {
  ...meta,
  title: 'HMI state mismatch',
  defaults: { displayWindowMs: 300 },
  createState: () => ({ pending: new Map() }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const pending = ctx.state.pending;
    for (const [feature, p] of pending) {
      if (ctx.now > p.deadline) {
        out.push(...judge(ctx, feature, p));
        pending.delete(feature);
      }
    }
    const change = stateChange(e);
    if (change) {
      pending.set(change.feature, { expected: change.value, startTs: e.ts, deadline: e.ts + ctx.cfg.displayWindowMs, lastShown: null });
    } else if (e.module === 'HMI' && e.type === 'DISPLAY' && e.data.item === 'STATE') {
      const p = pending.get(e.data.feature);
      if (p && e.ts <= p.deadline) p.lastShown = e.data.value;
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const out = [...ctx.state.pending].flatMap(([feature, p]) => judge(ctx, feature, p));
    ctx.state.pending.clear();
    return out;
  },

  boundaryCases(cfg, eps) {
    const window = triplet('HMI-01-window', 'plus', (sign, id) => {
      const showTs = 1000 + cfg.displayWindowMs + sign * eps.time;
      return {
        description: `HMI shows ACC ACTIVE ${showTs - 1000} ms after change (window ${cfg.displayWindowMs} ms)`,
        events: [
          ev('ACC/STATE', 1000, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
          ev('HMI/DISPLAY', showTs, id, { feature: 'ACC', item: 'STATE', value: 'ACTIVE' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
    const id = 'HMI-01-wrong-value';
    return [...window, {
      caseId: id,
      description: 'HMI shows STANDBY while ACC is ACTIVE',
      expectTriggered: true,
      events: [
        ev('ACC/STATE', 1000, id, { from: 'OFF', to: 'ACTIVE', reason: 'boundary' }),
        ev('HMI/DISPLAY', 1100, id, { feature: 'ACC', item: 'STATE', value: 'STANDBY' }),
        scenarioEnd(id, 3000),
      ],
    }];
  },
};
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/rules/hmi-01.test.ts && npm run typecheck`
Expected: 8 passed

- [ ] **Step 5: 提交**

```bash
git add src/rules/hmi-01.ts tests/rules/hmi-01.test.ts
git commit -m "feat(adas): add HMI-01 state mismatch rule

Refs #69"
```

---

### Task 6f: 规则 HMI-02 接管请求未显示 + 规则注册表

**Files:**
- Create: `src/rules/hmi-02.ts`, `src/rules/index.ts`
- Test: `tests/rules/hmi-02.test.ts`, `tests/rules/index.test.ts`

判定：`ACC / TAKEOVER_REQUEST` 后 `takeoverWindowMs`（200 ms）内无 `DISPLAY item=TAKEOVER` → 异常。

- [ ] **Step 1: 写失败测试 `tests/rules/hmi-02.test.ts` 与 `tests/rules/index.test.ts`**

```ts
// tests/rules/hmi-02.test.ts
import { describe, expect, it } from 'vitest';
import { hmi02 } from '../../src/rules/hmi-02.ts';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { brief, run } from '../helpers.ts';

const S = 'S1';
const req = (ts: number) => ev('ACC/TAKEOVER_REQUEST', ts, S, { reason: 't' });
const shown = (ts: number) => ev('HMI/DISPLAY', ts, S, { feature: 'ACC', item: 'TAKEOVER', value: 'SHOWN' });
const tick = (ts: number) => ev('ACC/BRAKE_PEDAL', ts, S, { pressed: false });

describe('HMI-02 takeover request not shown', () => {
  it('passes when shown in time (exactly at the deadline is on time)', () => {
    expect(run([hmi02], [req(1000), shown(1200), scenarioEnd(S, 2000)])).toEqual([]);
  });

  it('fires when shown too late', () => {
    const out = run([hmi02], [req(1000), tick(1250), shown(1450), scenarioEnd(S, 2000)]);
    expect(brief(out)).toEqual([{ ruleId: 'HMI-02', scenarioId: S, startTs: 1000, endTs: 1200, detectTs: 1250 }]);
    expect(out[0]?.message).toBe('Takeover request not shown on HMI within 200 ms');
  });

  it('ignores state displays and keeps the first pending request', () => {
    const state = ev('HMI/DISPLAY', 1100, S, { feature: 'ACC', item: 'STATE', value: 'ACTIVE' });
    expect(run([hmi02], [req(1000), req(1050), state, scenarioEnd(S, 2000)]).map((a) => a.startTs)).toEqual([1000]);
  });

  it('fires at scenario end if still pending', () => {
    expect(run([hmi02], [req(1000), scenarioEnd(S, 1100)])).toHaveLength(1);
  });
});
```

```ts
// tests/rules/index.test.ts
import { describe, expect, it } from 'vitest';
import { RULES } from '../../src/rules/index.ts';

describe('RULES registry', () => {
  it('registers the six Phase 1 rules with unique ids', () => {
    expect(RULES.map((r) => r.id)).toEqual(['ACC-01', 'ACC-02', 'APA-01', 'APA-02', 'HMI-01', 'HMI-02']);
  });
});
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/rules/hmi-02.test.ts tests/rules/index.test.ts`
Expected: FAIL — cannot resolve `hmi-02.ts` / `index.ts`

- [ ] **Step 3: 实现 `src/rules/hmi-02.ts`**

```ts
import { anomaly, type Anomaly, type Rule, type RuleCtx } from '../engine/types.ts';
import { ev, scenarioEnd } from '../schema/events.ts';
import { triplet } from './boundary.ts';

type Cfg = { takeoverWindowMs: number };
type Pending = { startTs: number; deadline: number };
type State = { pending: Pending | null };

const meta = { id: 'HMI-02', module: 'HMI', severity: 'P1' } as const;

const report = (ctx: RuleCtx<State, Cfg>, p: Pending): Anomaly =>
  anomaly(meta, ctx, {
    startTs: p.startTs,
    endTs: p.deadline,
    message: `Takeover request not shown on HMI within ${ctx.cfg.takeoverWindowMs} ms`,
  });

export const hmi02: Rule<State, Cfg> = {
  ...meta,
  title: 'Takeover request not shown',
  defaults: { takeoverWindowMs: 200 },
  createState: () => ({ pending: null }),

  onEvent(e, ctx) {
    const out: Anomaly[] = [];
    const s = ctx.state;
    if (s.pending && ctx.now > s.pending.deadline) {
      out.push(report(ctx, s.pending));
      s.pending = null;
    }
    if (e.module === 'ACC' && e.type === 'TAKEOVER_REQUEST') {
      s.pending ??= { startTs: e.ts, deadline: e.ts + ctx.cfg.takeoverWindowMs };
    } else if (e.module === 'HMI' && e.type === 'DISPLAY' && e.data.item === 'TAKEOVER' && s.pending && e.ts <= s.pending.deadline) {
      s.pending = null;
    }
    return out;
  },

  onScenarioEnd(ctx) {
    const p = ctx.state.pending;
    ctx.state.pending = null;
    return p ? [report(ctx, p)] : [];
  },

  boundaryCases(cfg, eps) {
    return triplet('HMI-02-window', 'plus', (sign, id) => {
      const showTs = 1000 + cfg.takeoverWindowMs + sign * eps.time;
      return {
        description: `takeover shown ${showTs - 1000} ms after request (window ${cfg.takeoverWindowMs} ms)`,
        events: [
          ev('ACC/TAKEOVER_REQUEST', 1000, id, { reason: 'boundary' }),
          ev('HMI/DISPLAY', showTs, id, { feature: 'ACC', item: 'TAKEOVER', value: 'SHOWN' }),
          scenarioEnd(id, 3000),
        ],
      };
    });
  },
};
```

- [ ] **Step 4: 实现 `src/rules/index.ts`**

```ts
import type { AnyRule } from '../engine/types.ts';
import { acc01 } from './acc-01.ts';
import { acc02 } from './acc-02.ts';
import { apa01 } from './apa-01.ts';
import { apa02 } from './apa-02.ts';
import { hmi01 } from './hmi-01.ts';
import { hmi02 } from './hmi-02.ts';

/** Phase 1 ruleset (RULESET_VERSION 1.0.0). Add a rule: new file + one line here. */
export const RULES: readonly AnyRule[] = [acc01, acc02, apa01, apa02, hmi01, hmi02];
```

- [ ] **Step 5: 运行全部规则测试**

Run: `npx vitest run tests/rules && npm run typecheck && npm run lint`
Expected: 全部 passed（ACC-01 8 + ACC-02 5 + APA-01 6 + APA-02 6 + HMI-01 8 + HMI-02 4 + index 1 = 38）

- [ ] **Step 6: 提交**

```bash
git add src/rules/hmi-02.ts src/rules/index.ts tests/rules/hmi-02.test.ts tests/rules/index.test.ts
git commit -m "feat(adas): add HMI-02 takeover rule and rule registry

Refs #69"
```

---

### Task 7: 报告组装、`parseFile` 与黄金样本

**Files:**
- Create: `src/parser/parse.ts`, `fixtures/golden/mixed.jsonl`
- Test: `tests/parser/parse.test.ts`（快照写入 `tests/parser/__snapshots__/`）

- [ ] **Step 1: 创建 `fixtures/golden/mixed.jsonl`**

```
{"ts":0,"scenarioId":"G1","module":"ACC","type":"STATE","data":{"from":"OFF","to":"ACTIVE","reason":"golden"}}
{"ts":100,"scenarioId":"G1","module":"HMI","type":"DISPLAY","data":{"feature":"ACC","item":"STATE","value":"ACTIVE"}}
{"ts":1000,"scenarioId":"G1","module":"ACC","type":"TRACK","data":{"egoSpeed":10,"setSpeed":10,"leadDistance":7,"leadRelSpeed":0}}
{"ts":1050,"scenarioId":"G1","module":"ACC","type":"TRACK","data":{"egoSpeed":10,"setSpeed":10,"leadDistance":6.5,"leadRelSpeed":0}}
{"ts":1500,"scenarioId":"G1","module":"ACC","type":"TRACK","data":{"egoSpeed":10,"setSpeed":10,"leadDistance":20,"leadRelSpeed":0}}
{"ts":3000,"scenarioId":"G1","module":"SYS","type":"SCENARIO_END","data":{}}
{"ts":0,"scenarioId":"G2","module":"APA","type":"STEP","data":{"step":"MANEUVERING","reason":"golden"}}
{"ts":500,"scenarioId":"G2","module":"APA","type":"OBSTACLE","data":{"minDistance":0.25}}
{"ts":600,"scenarioId":"G2","module":"APA","type":"OBSTACLE","data":{"minDistance":0.9}}
{"ts":2000,"scenarioId":"G2","module":"SYS","type":"SCENARIO_END","data":{}}
```

预期异常（人工推导）：G1 ACC-01 `[1000, 1050]` detect 1500；G2 HMI-01 `[0, 300]` detect 500（MANEUVERING 未显示）；G2 APA-01 `[500, 700]` detect 2000；G2 APA-02 未完成 `[0, 2000]` detect 2000。

- [ ] **Step 2: 写失败测试 `tests/parser/parse.test.ts`**

```ts
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { parseFile, parseLines } from '../../src/parser/parse.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../../src/version.ts';
import { brief } from '../helpers.ts';

const golden = fileURLToPath(new URL('../../fixtures/golden/mixed.jsonl', import.meta.url));

describe('parseFile', () => {
  it('detects the hand-derived anomalies in the golden sample', async () => {
    const { report } = await parseFile(golden);
    expect(brief(report.anomalies)).toEqual([
      { ruleId: 'ACC-01', scenarioId: 'G1', startTs: 1000, endTs: 1050, detectTs: 1500 },
      { ruleId: 'APA-02', scenarioId: 'G2', startTs: 0, endTs: 2000, detectTs: 2000 },
      { ruleId: 'HMI-01', scenarioId: 'G2', startTs: 0, endTs: 300, detectTs: 500 },
      { ruleId: 'APA-01', scenarioId: 'G2', startTs: 500, endTs: 700, detectTs: 2000 },
    ]);
    expect(report.meta).toEqual({ input: golden, events: 10, scenarios: 2, toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION });
    expect(report.summary).toEqual({ byRule: { 'ACC-01': 1, 'APA-02': 1, 'HMI-01': 1, 'APA-01': 1 }, bySeverity: { P1: 2, P2: 2 } });
  });

  it('matches the report snapshot', async () => {
    const { report } = await parseFile(golden);
    expect({ ...report, meta: { ...report.meta, input: 'fixtures/golden/mixed.jsonl' } }).toMatchSnapshot();
  });

  it('produces an empty report for empty input', async () => {
    const { report } = await parseLines([], 'empty.jsonl');
    expect(report.meta.events).toBe(0);
    expect(report.anomalies).toEqual([]);
    expect(report.summary).toEqual({ byRule: {}, bySeverity: {} });
  });
});
```

> 排序规则：`scenarioId` → `startTs` → `ruleId`。G2 中 APA-02 与 HMI-01 的 `startTs` 都是 0，按 `ruleId` 字典序 `APA-02` < `HMI-01`。

- [ ] **Step 3: 运行，确认失败**

Run: `npx vitest run tests/parser/parse.test.ts`
Expected: FAIL — cannot resolve `parse.ts`

- [ ] **Step 4: 实现 `src/parser/parse.ts`**

```ts
import { createEngine, type EngineConfig } from '../engine/engine.ts';
import type { Anomaly, AnyRule } from '../engine/types.ts';
import { readLines } from '../io/jsonl.ts';
import { RULES } from '../rules/index.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../version.ts';
import { scanLines, type DataQuality, type ScanOptions, type ScanResult } from './scan.ts';

export interface Report {
  meta: { input: string; events: number; scenarios: number; toolVersion: string; rulesetVersion: string };
  anomalies: Anomaly[];
  summary: { byRule: Record<string, number>; bySeverity: Record<string, number> };
  dataQuality: DataQuality;
}

export interface ParseOptions extends ScanOptions {
  config?: EngineConfig;
  rules?: readonly AnyRule[];
}

export interface ParseOutcome { report: Report; peakActiveScenarios: number }

const byPosition = (a: Anomaly, b: Anomaly) =>
  a.scenarioId < b.scenarioId ? -1 : a.scenarioId > b.scenarioId ? 1
    : a.startTs - b.startTs || (a.ruleId < b.ruleId ? -1 : a.ruleId > b.ruleId ? 1 : 0);

export function buildReport(input: string, scan: ScanResult, anomalies: readonly Anomaly[]): Report {
  const sorted = [...anomalies].sort(byPosition);
  const byRule: Record<string, number> = {};
  const bySeverity: Record<string, number> = {};
  for (const a of sorted) {
    byRule[a.ruleId] = (byRule[a.ruleId] ?? 0) + 1;
    bySeverity[a.severity] = (bySeverity[a.severity] ?? 0) + 1;
  }
  return {
    meta: { input, events: scan.events, scenarios: scan.scenarios, toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION },
    anomalies: sorted,
    summary: { byRule, bySeverity },
    dataQuality: scan.quality,
  };
}

export async function parseLines(
  lines: AsyncIterable<string> | Iterable<string>,
  input: string,
  opts: ParseOptions = {},
): Promise<ParseOutcome> {
  const engine = createEngine(opts.rules ?? RULES, opts.config);
  const anomalies: Anomaly[] = [];
  let peakActiveScenarios = 0;
  const scan = await scanLines(lines, (e) => {
    anomalies.push(...engine.push(e));
    peakActiveScenarios = Math.max(peakActiveScenarios, engine.activeScenarios());
  }, opts);
  anomalies.push(...engine.end());
  return { report: buildReport(input, scan, anomalies), peakActiveScenarios };
}

export function parseFile(path: string, opts: ParseOptions = {}): Promise<ParseOutcome> {
  return parseLines(readLines(path), path, opts);
}
```

- [ ] **Step 5: 运行测试（首次运行写入快照）**

Run: `npx vitest run tests/parser/parse.test.ts`
Expected: 3 passed，生成 `tests/parser/__snapshots__/parse.test.ts.snap`。打开快照人工核对：4 条异常、`dataQuality.counts` 全为 0、`message` 为英文。

- [ ] **Step 6: 提交（含快照）**

```bash
git add src/parser/parse.ts fixtures/golden tests/parser/parse.test.ts tests/parser/__snapshots__
git commit -m "feat(adas): assemble anomaly report and add golden sample

Refs #69"
```

---

### Task 8: 合成数据生成器

**Files:**
- Create: `src/generator/generate.ts`
- Test: `tests/generator/generate.test.ts`

规格（spec §6.5）：默认 20 个场景，偶数下标为 `ACC_FOLLOW`（60 s），奇数下标为 `APA_PARK`（≤ 90 s）；TRACK 50 ms、OBSTACLE 100 ms；每条规则注入 2 次故障（共 12 条标签），只注入在前 4 个 ACC 场景和前 4 个 APA 场景；无故障信号与阈值保持安全余量；每个注入只触发目标规则。

ACC 场景时间线（无故障部分）：0 ms OFF→STANDBY；1000 ms STANDBY→ACTIVE；28000 ms 踩刹车，28100 ms ACTIVE→STANDBY，28600 ms 松开，30000 ms STANDBY→ACTIVE；50000 ms 接管请求 + 显示；58000 ms ACTIVE→STANDBY；60000 ms 场景结束。故障槽位：15000 ms、40000 ms。

| ACC 场景 | 槽位 15000 | 槽位 40000 |
|----------|-----------|-----------|
| 第 1 个（S001） | ACC-01 | ACC-02 |
| 第 2 个（S003） | HMI-01（显示错误值） | HMI-02（显示过晚） |
| 第 3 个（S005） | ACC-01 | HMI-01（未显示） |
| 第 4 个（S007） | ACC-02 | HMI-02（未显示） |

| APA 场景 | 故障 |
|----------|------|
| S002、S004 | APA-01：14000–14400 ms 距离 0.2 m，14450 ms ABORTED |
| S006 | APA-02 超时：MANEUVERING 4000 ms，COMPLETED 69000 ms |
| S008 | APA-02 未完成：MANEUVERING 4000 ms，30000 ms 场景结束 |

- [ ] **Step 1: 写失败测试 `tests/generator/generate.test.ts`**

```ts
import { describe, expect, it } from 'vitest';
import { generate, MIN_SCENARIOS } from '../../src/generator/generate.ts';
import { parseLine } from '../../src/schema/validate.ts';

describe('generate', () => {
  it('is deterministic for the same seed', () => {
    expect(generate({ seed: 42, scenarios: 20 })).toEqual(generate({ seed: 42, scenarios: 20 }));
  });

  it('changes values (not structure) across seeds', () => {
    const a = generate({ seed: 1, scenarios: 20 }), b = generate({ seed: 2, scenarios: 20 });
    expect(a.events).not.toEqual(b.events);
    expect(a.labels).toEqual(b.labels);
  });

  it('injects 12 labelled faults, 2 per Phase 1 rule', () => {
    const { labels } = generate({ seed: 42, scenarios: 20 });
    expect(labels).toHaveLength(12);
    const perRule = Object.groupBy(labels, (l) => l.ruleId);
    for (const id of ['ACC-01', 'ACC-02', 'APA-01', 'APA-02', 'HMI-01', 'HMI-02']) expect(perRule[id]).toHaveLength(2);
    expect(new Set(labels.map((l) => l.scenarioId)).size).toBe(8);
  });

  it('emits valid, per-scenario ordered events ending with SCENARIO_END', () => {
    const { events } = generate({ seed: 42, scenarios: 20 });
    const byScenario = Object.groupBy(events, (e) => e.scenarioId);
    expect(Object.keys(byScenario)).toHaveLength(20);
    for (const list of Object.values(byScenario)) {
      const ts = list!.map((e) => e.ts);
      expect(ts).toEqual([...ts].sort((x, y) => x - y));
      const last = list!.at(-1)!;
      expect([last.module, last.type]).toEqual(['SYS', 'SCENARIO_END']);
      expect(last.ts).toBeLessThanOrEqual(90000);
    }
    for (const e of events.slice(0, 500)) expect(parseLine(JSON.stringify(e)).ok).toBe(true);
  });

  it('uses the documented sampling periods', () => {
    const { events } = generate({ seed: 42, scenarios: 20 });
    const tracks = events.filter((e) => e.scenarioId === 'S009' && e.type === 'TRACK').map((e) => e.ts);
    expect(tracks).toHaveLength(1200);
    expect(tracks[1]! - tracks[0]!).toBe(50);
    const obstacles = events.filter((e) => e.scenarioId === 'S010' && e.type === 'OBSTACLE').map((e) => e.ts);
    expect(obstacles[1]! - obstacles[0]!).toBe(100);
  });

  it('interleaves scenarios while keeping per-scenario order', () => {
    const seq = generate({ seed: 42, scenarios: 20 });
    const mixed = generate({ seed: 42, scenarios: 20, interleave: true });
    expect(mixed.events).toHaveLength(seq.events.length);
    expect(mixed.events.slice(0, 20).map((e) => e.scenarioId)).not.toEqual(seq.events.slice(0, 20).map((e) => e.scenarioId));
    const s1 = (es: typeof seq.events) => es.filter((e) => e.scenarioId === 'S001');
    expect(s1(mixed.events)).toEqual(s1(seq.events));
  });

  it(`rejects fewer than ${MIN_SCENARIOS} scenarios`, () => {
    expect(() => generate({ seed: 1, scenarios: 7 })).toThrow(RangeError);
    expect(() => generate({ seed: 1, scenarios: 8.5 })).toThrow(RangeError);
  });
});
```

> `Object.groupBy` 需要 Node ≥ 21 与 `lib: ES2024`（T1 已配置）。

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/generator/generate.test.ts`
Expected: FAIL — cannot resolve `generate.ts`

- [ ] **Step 3: 实现 `src/generator/generate.ts`**

```ts
import { ev, scenarioEnd, type AccState, type AdasEvent, type ApaStep } from '../schema/events.ts';
import { createRng, type Rng } from './prng.ts';

export interface Label { scenarioId: string; ruleId: string; startTs: number; endTs: number }
export interface GenOptions { seed: number; scenarios: number; interleave?: boolean }
export interface GenResult { events: AdasEvent[]; labels: Label[] }

export const MIN_SCENARIOS = 8;

// Mirrors ACC-01 defaults; only used to keep clean data at a safe margin.
const ACC_MIN_GAP_M = 5;
const ACC_TIME_GAP_S = 1.0;
const ACC_END_MS = 60000;
const TRACK_MS = 50;
const OBSTACLE_MS = 100;
const SLOT_A = 15000;
const SLOT_B = 40000;

type AccFault = 'ACC-01' | 'ACC-02' | 'HMI-01-wrong' | 'HMI-01-missing' | 'HMI-02-late' | 'HMI-02-missing';
type ApaFault = 'APA-01' | 'APA-02-timeout' | 'APA-02-unfinished';

const ACC_PLAN: readonly (readonly [AccFault, AccFault])[] = [
  ['ACC-01', 'ACC-02'],
  ['HMI-01-wrong', 'HMI-02-late'],
  ['ACC-01', 'HMI-01-missing'],
  ['ACC-02', 'HMI-02-missing'],
];
const APA_PLAN: readonly ApaFault[] = ['APA-01', 'APA-01', 'APA-02-timeout', 'APA-02-unfinished'];

const round2 = (x: number) => Math.round(x * 100) / 100;
const byTs = (es: readonly AdasEvent[]) => [...es].sort((a, b) => a.ts - b.ts);

export function generate(o: GenOptions): GenResult {
  if (!Number.isInteger(o.scenarios) || o.scenarios < MIN_SCENARIOS) {
    throw new RangeError(`scenarios must be an integer >= ${MIN_SCENARIOS}`);
  }
  const events: AdasEvent[] = [];
  const labels: Label[] = [];
  for (let i = 0; i < o.scenarios; i++) {
    const id = `S${String(i + 1).padStart(3, '0')}`;
    const rng = createRng((o.seed + Math.imul(i + 1, 0x9e3779b9)) >>> 0);
    const sc = i % 2 === 0
      ? accScenario(id, rng, ACC_PLAN[i / 2] ?? null)
      : apaScenario(id, rng, APA_PLAN[(i - 1) / 2] ?? null);
    for (const e of byTs(sc.events)) events.push(e);
    labels.push(...sc.labels);
  }
  return { events: o.interleave ? byTs(events) : events, labels };
}

function accScenario(id: string, rng: Rng, faults: readonly [AccFault, AccFault] | null): GenResult {
  const events: AdasEvent[] = [];
  const labels: Label[] = [];
  const label = (ruleId: string, startTs: number, endTs: number) => labels.push({ scenarioId: id, ruleId, startTs, endTs });
  const display = (ts: number, value: string) =>
    events.push(ev('HMI/DISPLAY', ts, id, { feature: 'ACC', item: 'STATE', value }));
  const state = (ts: number, from: AccState, to: AccState, reason: string, shown: 'ok' | 'none' = 'ok') => {
    events.push(ev('ACC/STATE', ts, id, { from, to, reason }));
    if (shown === 'ok') display(ts + rng.int(50, 150), to);
  };
  const brake = (ts: number, pressed: boolean) => events.push(ev('ACC/BRAKE_PEDAL', ts, id, { pressed }));
  const takeover = (ts: number, showAt: number | null) => {
    events.push(ev('ACC/TAKEOVER_REQUEST', ts, id, { reason: 'sensor_degraded' }));
    if (showAt !== null) events.push(ev('HMI/DISPLAY', showAt, id, { feature: 'ACC', item: 'TAKEOVER', value: 'SHOWN' }));
  };

  // Clean script.
  state(0, 'OFF', 'STANDBY', 'driver_on');
  state(1000, 'STANDBY', 'ACTIVE', 'driver_set');
  brake(28000, true);
  state(28100, 'ACTIVE', 'STANDBY', 'brake');
  brake(28600, false);
  state(30000, 'STANDBY', 'ACTIVE', 'resume');
  takeover(50000, 50000 + rng.int(50, 150));
  state(58000, 'ACTIVE', 'STANDBY', 'driver_off');

  // Fault injections: each triggers only its target rule.
  const tooClose = new Set<number>();
  const inject = (f: AccFault, s: number) => {
    switch (f) {
      case 'ACC-01':
        for (let t = s; t <= s + 1000; t += TRACK_MS) tooClose.add(t);
        label('ACC-01', s, s + 1000);
        break;
      case 'ACC-02':
        brake(s, true);
        state(s + 500, 'ACTIVE', 'STANDBY', 'late_exit');
        brake(s + 700, false);
        state(s + 2000, 'STANDBY', 'ACTIVE', 'resume');
        label('ACC-02', s, s + 200);
        break;
      case 'HMI-01-wrong':
        state(s, 'ACTIVE', 'OVERRIDE', 'driver_accel', 'none');
        display(s + 100, 'ACTIVE');
        state(s + 1500, 'OVERRIDE', 'ACTIVE', 'accel_release');
        label('HMI-01', s, s + 300);
        break;
      case 'HMI-01-missing':
        state(s, 'ACTIVE', 'OVERRIDE', 'driver_accel', 'none');
        state(s + 1500, 'OVERRIDE', 'ACTIVE', 'accel_release');
        label('HMI-01', s, s + 300);
        break;
      case 'HMI-02-late':
        takeover(s, s + 450);
        label('HMI-02', s, s + 200);
        break;
      case 'HMI-02-missing':
        takeover(s, null);
        label('HMI-02', s, s + 200);
        break;
    }
  };
  if (faults) {
    inject(faults[0], SLOT_A);
    inject(faults[1], SLOT_B);
  }

  const v0 = rng.range(15, 25);
  for (let t = 0; t < ACC_END_MS; t += TRACK_MS) {
    const egoSpeed = round2(v0 + rng.range(-0.2, 0.2));
    const threshold = Math.max(ACC_MIN_GAP_M, egoSpeed * ACC_TIME_GAP_S);
    const leadDistance = tooClose.has(t) ? round2(threshold - 1.5) : round2(threshold + 8 + rng.range(0, 2));
    events.push(ev('ACC/TRACK', t, id, { egoSpeed, setSpeed: round2(v0 + 2), leadDistance, leadRelSpeed: round2(rng.range(-0.5, 0.5)) }));
  }
  events.push(scenarioEnd(id, ACC_END_MS));
  return { events, labels };
}

function apaScenario(id: string, rng: Rng, fault: ApaFault | null): GenResult {
  const events: AdasEvent[] = [];
  const labels: Label[] = [];
  const step = (ts: number, s: ApaStep, reason: string) => {
    events.push(ev('APA/STEP', ts, id, { step: s, reason }));
    events.push(ev('HMI/DISPLAY', ts + rng.int(50, 150), id, { feature: 'APA', item: 'STATE', value: s }));
  };

  step(0, 'SEARCHING', 'driver_request');
  step(3000, 'SLOT_SELECTED', 'slot_found');
  const start = 4000;
  step(start, 'MANEUVERING', 'start');

  let stop: number;
  let terminal: ApaStep | null = 'COMPLETED';
  const tooClose = new Set<number>();
  switch (fault) {
    case 'APA-01':
      for (let t = 14000; t <= 14400; t += OBSTACLE_MS) tooClose.add(t);
      stop = 14450;
      terminal = 'ABORTED';
      labels.push({ scenarioId: id, ruleId: 'APA-01', startTs: 14000, endTs: 14200 });
      break;
    case 'APA-02-timeout':
      stop = 69000;
      labels.push({ scenarioId: id, ruleId: 'APA-02', startTs: start, endTs: start + 60000 });
      break;
    case 'APA-02-unfinished':
      stop = 30000;
      terminal = null;
      labels.push({ scenarioId: id, ruleId: 'APA-02', startTs: start, endTs: stop });
      break;
    default:
      stop = start + rng.int(200, 400) * OBSTACLE_MS;
  }

  for (let t = start; t < stop; t += OBSTACLE_MS) {
    events.push(ev('APA/OBSTACLE', t, id, { minDistance: tooClose.has(t) ? 0.2 : round2(0.8 + rng.range(0, 1)) }));
  }
  if (terminal) step(stop, terminal, terminal === 'ABORTED' ? 'obstacle' : 'parked');
  events.push(scenarioEnd(id, terminal ? stop + 2000 : stop));
  return { events, labels };
}
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/generator && npm run typecheck && npm run lint`
Expected: generate 7 passed + prng 4 passed

- [ ] **Step 5: 提交**

```bash
git add src/generator/generate.ts tests/generator/generate.test.ts
git commit -m "feat(adas): add synthetic scenario generator with fault injection

Refs #69"
```

---

### Task 9: 真值匹配与真值测试

**Files:**
- Create: `src/eval/match.ts`
- Test: `tests/eval/match.test.ts`, `tests/truth.test.ts`

匹配（spec §6.4）：`scenarioId`、`ruleId` 相同且 `[startTs, endTs]` 闭区间有重叠；一一匹配。

- [ ] **Step 1: 写失败测试 `tests/eval/match.test.ts`**

```ts
import { describe, expect, it } from 'vitest';
import { matchLabels } from '../../src/eval/match.ts';
import type { Anomaly } from '../../src/engine/types.ts';

const a = (scenarioId: string, ruleId: string, startTs: number, endTs: number): Anomaly => ({
  ruleId, module: 'ACC', severity: 'P1', scenarioId, startTs, endTs, detectTs: endTs, message: '', evidence: {},
});
const l = (scenarioId: string, ruleId: string, startTs: number, endTs: number) => ({ scenarioId, ruleId, startTs, endTs });

describe('matchLabels', () => {
  it('matches on scenario + rule + closed-interval overlap', () => {
    const r = matchLabels([a('S1', 'ACC-01', 100, 200)], [l('S1', 'ACC-01', 200, 300)]);
    expect(r).toMatchObject({ matched: 1, precision: 1, recall: 1 });
  });

  it('does not match a different rule, scenario, or a disjoint interval', () => {
    const r = matchLabels(
      [a('S1', 'ACC-01', 0, 10), a('S2', 'ACC-01', 0, 10), a('S1', 'ACC-02', 0, 10)],
      [l('S1', 'ACC-01', 11, 20)],
    );
    expect(r).toMatchObject({ matched: 0, precision: 0, recall: 0 });
    expect(r.unmatchedAnomalies).toHaveLength(3);
    expect(r.unmatchedLabels).toHaveLength(1);
  });

  it('is one-to-one', () => {
    const r = matchLabels([a('S1', 'ACC-01', 0, 100)], [l('S1', 'ACC-01', 0, 10), l('S1', 'ACC-01', 50, 60)]);
    expect(r).toMatchObject({ matched: 1, precision: 1, recall: 0.5 });
  });

  it('defines precision and recall as 1 for empty sets', () => {
    expect(matchLabels([], [])).toMatchObject({ precision: 1, recall: 1 });
  });
});
```

- [ ] **Step 2: 写失败测试 `tests/truth.test.ts`**

```ts
import { describe, expect, it } from 'vitest';
import { matchLabels } from '../src/eval/match.ts';
import { generate } from '../src/generator/generate.ts';
import { toJsonl } from '../src/io/jsonl.ts';
import { parseLines } from '../src/parser/parse.ts';

async function evaluate(seed: number, interleave: boolean) {
  const { events, labels } = generate({ seed, scenarios: 20, interleave });
  const lines = toJsonl(events).trimEnd().split('\n');
  const { report } = await parseLines(lines, `seed-${seed}`);
  return { report, result: matchLabels(report.anomalies, labels) };
}

describe('truth labels (spec §6.4)', () => {
  it('seed 42 reaches 100% precision and recall', async () => {
    const { report, result } = await evaluate(42, false);
    expect(result.unmatchedAnomalies).toEqual([]);
    expect(result.unmatchedLabels).toEqual([]);
    expect(result).toMatchObject({ precision: 1, recall: 1, matched: 12 });
    expect(report.dataQuality.counts).toEqual({ invalidJson: 0, schemaErrors: 0, unknownEvents: 0, outOfOrder: 0 });
  });

  it('interleaved scenarios give the same result', async () => {
    const { result } = await evaluate(42, true);
    expect(result).toMatchObject({ precision: 1, recall: 1, matched: 12 });
  });

  it.each([1, 2, 3, 7, 2026])('seed %i stays at 100%', async (seed) => {
    const { result } = await evaluate(seed, false);
    expect(result).toMatchObject({ precision: 1, recall: 1 });
  });
});
```

- [ ] **Step 3: 运行，确认失败**

Run: `npx vitest run tests/eval tests/truth.test.ts`
Expected: FAIL — cannot resolve `match.ts`

- [ ] **Step 4: 实现 `src/eval/match.ts`**

```ts
import type { Anomaly } from '../engine/types.ts';
import type { Label } from '../generator/generate.ts';

export interface MatchResult {
  matched: number;
  precision: number;
  recall: number;
  unmatchedAnomalies: Anomaly[];
  unmatchedLabels: Label[];
}

const overlaps = (a: Anomaly, l: Label) =>
  a.scenarioId === l.scenarioId && a.ruleId === l.ruleId && a.startTs <= l.endTs && l.startTs <= a.endTs;

/** One-to-one greedy matching (labels in order, first overlapping unmatched anomaly). */
export function matchLabels(anomalies: readonly Anomaly[], labels: readonly Label[]): MatchResult {
  const used = new Set<number>();
  const unmatchedLabels: Label[] = [];
  for (const l of labels) {
    const i = anomalies.findIndex((a, idx) => !used.has(idx) && overlaps(a, l));
    if (i === -1) unmatchedLabels.push(l);
    else used.add(i);
  }
  const matched = used.size;
  return {
    matched,
    precision: anomalies.length === 0 ? 1 : matched / anomalies.length,
    recall: labels.length === 0 ? 1 : matched / labels.length,
    unmatchedAnomalies: anomalies.filter((_, idx) => !used.has(idx)),
    unmatchedLabels,
  };
}
```

- [ ] **Step 5: 运行测试**

Run: `npx vitest run tests/eval tests/truth.test.ts`
Expected: 4 + 7 passed。**若真值测试失败**：看 `unmatchedAnomalies` / `unmatchedLabels`，用 @superpowers:systematic-debugging 定位是 generator 的故障隔离问题还是规则问题，修复对应模块；不得放宽 100% 要求。

- [ ] **Step 6: 提交**

```bash
git add src/eval tests/eval tests/truth.test.ts
git commit -m "test(adas): add label matching and 100% truth test

Refs #69"
```

---

### Task 10: `casegen boundary`

**Files:**
- Create: `src/casegen/boundary.ts`
- Test: `tests/casegen/boundary.test.ts`

- [ ] **Step 1: 写失败测试**

```ts
import { mkdtempSync, readFileSync, readdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildBoundaryCases, verifyBoundaryCases, writeBoundaryCases } from '../../src/casegen/boundary.ts';
import { RULES } from '../../src/rules/index.ts';
import { parseLine } from '../../src/schema/validate.ts';

describe('casegen boundary', () => {
  const cases = buildBoundaryCases();

  it('produces at least 3 cases per rule with unique ids', () => {
    for (const r of RULES) expect(cases.filter((c) => c.ruleId === r.id).length).toBeGreaterThanOrEqual(3);
    expect(new Set(cases.map((c) => c.caseId)).size).toBe(cases.length);
    expect(cases).toHaveLength(26);
  });

  it('every case is a complete scenario ending with SCENARIO_END', () => {
    for (const c of cases) {
      const last = c.events.at(-1)!;
      expect([last.module, last.type, last.scenarioId]).toEqual(['SYS', 'SCENARIO_END', c.caseId]);
    }
  });

  it('all generated cases pass against the rule engine', () => {
    const results = verifyBoundaryCases(cases);
    expect(results.filter((r) => !r.pass)).toEqual([]);
  });

  it('applies config overrides to thresholds', () => {
    const custom = buildBoundaryCases(undefined, { rules: { 'ACC-02': { exitWindowMs: 500 } }, epsilon: { time: 5 } });
    const plus = custom.find((c) => c.caseId === 'ACC-02-window-plus')!;
    expect(plus.events.find((e) => e.type === 'STATE' && e.ts > 0)?.ts).toBe(1505);
    expect(verifyBoundaryCases(custom, undefined, { rules: { 'ACC-02': { exitWindowMs: 500 } } }).every((r) => r.pass)).toBe(true);
  });

  it('writes one JSONL file per case plus expected.json', async () => {
    const out = mkdtempSync(join(tmpdir(), 'adas-cases-'));
    const entries = await writeBoundaryCases(out, cases);
    const dir = join(out, 'boundary');
    expect(readdirSync(dir)).toHaveLength(cases.length + 1);
    const expected = JSON.parse(readFileSync(join(dir, 'expected.json'), 'utf8')) as typeof entries;
    expect(expected).toEqual(entries);
    const first = expected[0]!;
    const lines = readFileSync(join(dir, first.file), 'utf8').trimEnd().split('\n');
    expect(lines.every((x) => parseLine(x).ok)).toBe(true);
  });
});
```

> 用例数：ACC-01 6、ACC-02 3、APA-01 6、APA-02 4、HMI-01 4、HMI-02 3，共 26。

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/casegen`
Expected: FAIL — cannot resolve `boundary.ts`

- [ ] **Step 3: 实现 `src/casegen/boundary.ts`**

```ts
import { mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { createEngine, resolveEpsilon, resolveRuleConfig, type EngineConfig } from '../engine/engine.ts';
import type { AnyRule, BoundaryCase } from '../engine/types.ts';
import { toJsonl } from '../io/jsonl.ts';
import { RULES } from '../rules/index.ts';

export type RuleBoundaryCase = BoundaryCase & { ruleId: string };
export interface BoundaryEntry { caseId: string; ruleId: string; file: string; expectTriggered: boolean; description: string }
export interface CaseResult { caseId: string; ruleId: string; expectTriggered: boolean; triggered: boolean; pass: boolean }

/** Collect boundary cases from every rule; rules own the construction logic. */
export function buildBoundaryCases(rules: readonly AnyRule[] = RULES, config: EngineConfig = {}): RuleBoundaryCase[] {
  const eps = resolveEpsilon(config);
  return rules.flatMap((r) => r.boundaryCases(resolveRuleConfig(r, config), eps).map((c) => ({ ...c, ruleId: r.id })));
}

/** Run each case through a fresh engine and compare with `expectTriggered`. */
export function verifyBoundaryCases(
  cases: readonly RuleBoundaryCase[],
  rules: readonly AnyRule[] = RULES,
  config: EngineConfig = {},
): CaseResult[] {
  return cases.map((c) => {
    const engine = createEngine(rules, config);
    const anomalies = [...c.events.flatMap((e) => engine.push(e)), ...engine.end()];
    const triggered = anomalies.some((a) => a.ruleId === c.ruleId);
    return { caseId: c.caseId, ruleId: c.ruleId, expectTriggered: c.expectTriggered, triggered, pass: triggered === c.expectTriggered };
  });
}

export async function writeBoundaryCases(outDir: string, cases: readonly RuleBoundaryCase[]): Promise<BoundaryEntry[]> {
  const dir = join(outDir, 'boundary');
  await mkdir(dir, { recursive: true });
  const entries: BoundaryEntry[] = [];
  for (const c of cases) {
    const file = `${c.caseId}.jsonl`;
    await writeFile(join(dir, file), toJsonl(c.events));
    entries.push({ caseId: c.caseId, ruleId: c.ruleId, file, expectTriggered: c.expectTriggered, description: c.description });
  }
  await writeFile(join(dir, 'expected.json'), `${JSON.stringify(entries, null, 2)}\n`);
  return entries;
}
```

- [ ] **Step 4: 运行测试**

Run: `npx vitest run tests/casegen && npm run typecheck`
Expected: 5 passed

- [ ] **Step 5: 提交**

```bash
git add src/casegen tests/casegen
git commit -m "feat(adas): generate and verify boundary value cases from rules

Refs #69"
```

---

### Task 11: 配置加载、CLI 与退出码

**Files:**
- Create: `src/config.ts`, `src/cli/main.ts`, `src/cli.ts`
- Test: `tests/config.test.ts`, `tests/cli/main.test.ts`

退出码（spec §11）：0 正常；1 门禁触发（`--fail-on P1|P2` 达到级别及以上，或 `--strict` 下有数据质量问题，或边界用例失败）；2 用法错误（参数错、文件不存在、配置无效）。

- [ ] **Step 1: 写失败测试 `tests/config.test.ts`**

```ts
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { loadConfig, UsageError } from '../src/config.ts';

const write = (content: string) => {
  const p = join(mkdtempSync(join(tmpdir(), 'adas-cfg-')), 'rules.config.json');
  writeFileSync(p, content);
  return p;
};

describe('loadConfig', () => {
  it('loads rule overrides and epsilon', async () => {
    const p = write('{"rules":{"ACC-01":{"timeGapS":1.2}},"epsilon":{"time":2}}');
    expect(await loadConfig(p)).toEqual({ rules: { 'ACC-01': { timeGapS: 1.2 } }, epsilon: { time: 2 } });
  });

  it.each([
    ['missing file', '/nonexistent/rules.json', /config file not found/],
    ['invalid JSON', null, /not valid JSON/],
  ])('rejects %s', async (_name, path, msg) => {
    await expect(loadConfig(path ?? write('{'))).rejects.toThrow(msg);
  });

  it('rejects unknown keys and non-numeric thresholds', async () => {
    await expect(loadConfig(write('{"rule":{}}'))).rejects.toBeInstanceOf(UsageError);
    await expect(loadConfig(write('{"rules":{"ACC-01":{"timeGapS":"x"}}}'))).rejects.toThrow(/invalid config/);
  });
});
```

- [ ] **Step 2: 写失败测试 `tests/cli/main.test.ts`**

```ts
import { existsSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { main } from '../../src/cli/main.ts';
import { scenarioEnd } from '../../src/schema/events.ts';

function capture() {
  const out: string[] = [], err: string[] = [];
  return { io: { out: (s: string) => out.push(s), err: (s: string) => err.push(s) }, out, err };
}
const tmp = () => mkdtempSync(join(tmpdir(), 'adas-cli-'));

describe('adas-lab CLI', () => {
  it('gen writes events and labels', async () => {
    const dir = tmp(), c = capture();
    expect(await main(['gen', '--out', dir, '--seed', '7'], c.io)).toBe(0);
    expect(existsSync(join(dir, 'events.jsonl'))).toBe(true);
    expect(JSON.parse(readFileSync(join(dir, 'labels.json'), 'utf8'))).toHaveLength(12);
    expect(c.out.join('\n')).toMatch(/Generated \d+ events in 20 scenarios \(12 injected faults\)/);
  });

  it('parse writes a report and applies --fail-on', async () => {
    const dir = tmp();
    await main(['gen', '--out', dir], capture().io);
    const input = join(dir, 'events.jsonl'), report = join(dir, 'anomalies.json');
    const c = capture();
    expect(await main(['parse', input, '--out', report], c.io)).toBe(0);
    expect(JSON.parse(readFileSync(report, 'utf8')).anomalies).toHaveLength(12);
    expect(c.out.join('\n')).toContain('Anomalies: 12 (P1: 8, P2: 4)');
    expect(await main(['parse', input, '--out', report, '--fail-on', 'P1'], capture().io)).toBe(1);
    expect(await main(['parse', input, '--out', report, '--fail-on', 'P2'], capture().io)).toBe(1);
  });

  it('--strict fails on data quality issues only', async () => {
    const dir = tmp(), input = join(dir, 'in.jsonl'), report = join(dir, 'r.json');
    writeFileSync(input, `{bad\n${JSON.stringify(scenarioEnd('S', 0))}\n`);
    expect(await main(['parse', input, '--out', report], capture().io)).toBe(0);
    expect(await main(['parse', input, '--out', report, '--strict'], capture().io)).toBe(1);
    writeFileSync(input, `${JSON.stringify(scenarioEnd('S', 0))}\n`);
    expect(await main(['parse', input, '--out', report, '--strict', '--fail-on', 'P1'], capture().io)).toBe(0);
  });

  it('cases boundary writes and verifies cases', async () => {
    const dir = tmp(), c = capture();
    expect(await main(['cases', 'boundary', '--out', dir], c.io)).toBe(0);
    expect(existsSync(join(dir, 'boundary', 'expected.json'))).toBe(true);
    expect(c.out.join('\n')).toContain('26 passed, 0 failed');
  });

  it.each([
    [['parse', '/nonexistent.jsonl'], /input file not found/],
    [['parse', 'x.jsonl', '--fail-on', 'P9'], /P9/],
    [['gen', '--scenarios', '3'], /scenarios must be an integer >= 8/],
    [['gen', '--seed', 'abc'], /must be an integer/],
    [['parse', 'fixtures/golden/mixed.jsonl', '--config', '/nonexistent.json'], /config file not found/],
    [['unknown-command'], /unknown command/],
  ])('exits 2 on usage error: %j', async (args, msg) => {
    const c = capture();
    expect(await main(args, c.io)).toBe(2);
    expect(c.err.join('\n')).toMatch(msg);
  });

  it('prints the version', async () => {
    const c = capture();
    expect(await main(['--version'], c.io)).toBe(0);
    expect(c.out.join('\n')).toContain('0.1.0');
  });
});
```

- [ ] **Step 3: 运行，确认失败**

Run: `npx vitest run tests/config.test.ts tests/cli/main.test.ts`
Expected: FAIL — cannot resolve `config.ts` / `main.ts`

- [ ] **Step 4: 实现 `src/config.ts`**

```ts
import { readFile } from 'node:fs/promises';
import { z } from 'zod';
import type { EngineConfig } from './engine/engine.ts';

/** Errors that map to exit code 2. */
export class UsageError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'UsageError';
  }
}

const ConfigSchema = z.object({
  rules: z.record(z.string(), z.record(z.string(), z.number())).optional(),
  epsilon: z.object({ distance: z.number().positive(), time: z.number().positive(), torque: z.number().positive() }).partial().optional(),
}).strict();

export async function loadConfig(path: string): Promise<EngineConfig> {
  let text: string;
  try {
    text = await readFile(path, 'utf8');
  } catch {
    throw new UsageError(`config file not found: ${path}`);
  }
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new UsageError(`config is not valid JSON: ${path}`);
  }
  const r = ConfigSchema.safeParse(raw);
  if (!r.success) {
    throw new UsageError(`invalid config: ${r.error.issues.map((i) => `${i.path.map(String).join('.')}: ${i.message}`).join('; ')}`);
  }
  return r.data;
}
```

- [ ] **Step 5: 实现 `src/cli/main.ts`**

```ts
import { access, mkdir, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { Command, CommanderError, InvalidArgumentError, Option } from 'commander';
import { buildBoundaryCases, verifyBoundaryCases, writeBoundaryCases } from '../casegen/boundary.ts';
import { loadConfig, UsageError } from '../config.ts';
import type { EngineConfig } from '../engine/engine.ts';
import { generate } from '../generator/generate.ts';
import { toJsonl } from '../io/jsonl.ts';
import { parseFile, type Report } from '../parser/parse.ts';
import { hasQualityIssues } from '../parser/scan.ts';
import { TOOL_VERSION } from '../version.ts';

export interface Io { out(s: string): void; err(s: string): void }
const processIo: Io = { out: (s) => process.stdout.write(`${s}\n`), err: (s) => process.stderr.write(`${s}\n`) };

type FailOn = 'P1' | 'P2';

function integer(v: string): number {
  const n = Number(v);
  if (!Number.isInteger(n)) throw new InvalidArgumentError('must be an integer');
  return n;
}

async function writeJson(path: string, data: unknown): Promise<void> {
  await mkdir(dirname(path), { recursive: true });
  await writeFile(path, `${JSON.stringify(data, null, 2)}\n`);
}

const config = (path: string | undefined): Promise<EngineConfig> => (path ? loadConfig(path) : Promise.resolve({}));

export function exitCodeFor(report: Report, failOn: FailOn | undefined, strict: boolean): 0 | 1 {
  const severityGate = failOn === 'P2'
    ? report.anomalies.length > 0
    : failOn === 'P1' && report.anomalies.some((a) => a.severity === 'P1');
  return severityGate || (strict && hasQualityIssues(report.dataQuality)) ? 1 : 0;
}

function printSummary(io: Io, report: Report, out: string): void {
  const { meta, summary, dataQuality: q } = report;
  io.out(`Parsed ${meta.events} events in ${meta.scenarios} scenarios`);
  io.out(`Anomalies: ${report.anomalies.length} (P1: ${summary.bySeverity['P1'] ?? 0}, P2: ${summary.bySeverity['P2'] ?? 0})`);
  io.out(`Data quality: invalid JSON ${q.counts.invalidJson}, schema errors ${q.counts.schemaErrors}, unknown events ${q.counts.unknownEvents}, out of order ${q.counts.outOfOrder}`);
  io.out(`Report written to ${out}`);
}

export async function main(argv: readonly string[], io: Io = processIo): Promise<number> {
  let code = 0;
  const program = new Command('adas-lab')
    .description('ADAS log toolchain lab (synthetic data only)')
    .version(TOOL_VERSION)
    .exitOverride()
    .configureOutput({ writeOut: (s) => io.out(s.trimEnd()), writeErr: (s) => io.err(s.trimEnd()) });

  program.command('gen')
    .description('Generate synthetic ADAS logs and truth labels')
    .option('--seed <n>', 'random seed', integer, 42)
    .option('--scenarios <n>', 'number of scenarios (>= 8)', integer, 20)
    .option('--interleave', 'interleave events across scenarios', false)
    .option('--out <dir>', 'output directory', 'out')
    .action(async (o: { seed: number; scenarios: number; interleave: boolean; out: string }) => {
      let result;
      try {
        result = generate(o);
      } catch (e) {
        if (e instanceof RangeError) throw new UsageError(e.message);
        throw e;
      }
      const eventsPath = join(o.out, 'events.jsonl');
      await mkdir(o.out, { recursive: true });
      await writeFile(eventsPath, toJsonl(result.events));
      await writeJson(join(o.out, 'labels.json'), result.labels);
      io.out(`Generated ${result.events.length} events in ${o.scenarios} scenarios (${result.labels.length} injected faults) -> ${eventsPath}`);
    });

  program.command('parse')
    .description('Detect anomalies in a JSON Lines log')
    .argument('<input>', 'input .jsonl file')
    .option('--out <file>', 'report path', 'out/anomalies.json')
    .option('--config <file>', 'rule config JSON')
    .addOption(new Option('--fail-on <level>', 'exit 1 if any anomaly at this level or above').choices(['P1', 'P2']))
    .option('--strict', 'exit 1 on any data quality issue', false)
    .action(async (input: string, o: { out: string; config?: string; failOn?: FailOn; strict: boolean }) => {
      try {
        await access(input);
      } catch {
        throw new UsageError(`input file not found: ${input}`);
      }
      const { report } = await parseFile(input, { config: await config(o.config) });
      await writeJson(o.out, report);
      printSummary(io, report, o.out);
      code = exitCodeFor(report, o.failOn, o.strict);
    });

  program.command('cases')
    .description('Generate test cases')
    .command('boundary')
    .description('Generate boundary value cases from rule thresholds and verify them')
    .option('--out <dir>', 'output directory', 'out/cases')
    .option('--config <file>', 'rule config JSON')
    .action(async (o: { out: string; config?: string }) => {
      const cfg = await config(o.config);
      const list = buildBoundaryCases(undefined, cfg);
      await writeBoundaryCases(o.out, list);
      const results = verifyBoundaryCases(list, undefined, cfg);
      const failed = results.filter((r) => !r.pass);
      for (const f of failed) io.err(`FAIL ${f.caseId}: expected triggered=${f.expectTriggered}, got ${f.triggered}`);
      io.out(`${results.length} boundary cases written to ${join(o.out, 'boundary')}; ${results.length - failed.length} passed, ${failed.length} failed`);
      code = failed.length > 0 ? 1 : 0;
    });

  try {
    await program.parseAsync([...argv], { from: 'user' });
    return code;
  } catch (e) {
    if (e instanceof CommanderError) return e.exitCode === 0 ? 0 : 2;
    if (e instanceof UsageError) {
      io.err(`error: ${e.message}`);
      return 2;
    }
    throw e;
  }
}
```

- [ ] **Step 6: 实现 `src/cli.ts`**

```ts
#!/usr/bin/env node
import { main } from './cli/main.ts';

process.exitCode = await main(process.argv.slice(2));
```

- [ ] **Step 7: 运行测试**

Run: `npx vitest run tests/config.test.ts tests/cli/main.test.ts && npm run typecheck && npm run lint`
Expected: 全部 passed。若 `unknown-command` 用例的报错文字与 `/unknown command/` 不符，按 commander 实际输出调整正则（不改退出码要求）。

- [ ] **Step 8: 手动冒烟**

Run: `npm run demo`
Expected：依次输出 `Generated … (12 injected faults)`、`Anomalies: 12 (P1: 8, P2: 4)`、`26 boundary cases … 26 passed, 0 failed`，退出码 0。

- [ ] **Step 9: 提交**

```bash
git add src/config.ts src/cli src/cli.ts tests/config.test.ts tests/cli/main.test.ts
git commit -m "feat(adas): add adas-lab CLI with gen, parse and cases boundary

Refs #69"
```

---

### Task 12: CLI 端到端与性能冒烟

**Files:**
- Test: `tests/cli/e2e.test.ts`, `tests/perf/perf.test.ts`

- [ ] **Step 1: 写测试 `tests/cli/e2e.test.ts`**（以子进程运行真实入口）

```ts
import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const root = fileURLToPath(new URL('../..', import.meta.url));
const cli = (args: string[]) => spawnSync(process.execPath, ['src/cli.ts', ...args], { cwd: root, encoding: 'utf8' });

describe('adas-lab (subprocess)', () => {
  it('runs gen -> parse -> cases boundary end to end', () => {
    const dir = mkdtempSync(join(tmpdir(), 'adas-e2e-'));
    expect(cli(['gen', '--out', dir]).status).toBe(0);
    const parse = cli(['parse', join(dir, 'events.jsonl'), '--out', join(dir, 'anomalies.json'), '--fail-on', 'P1']);
    expect(parse.status).toBe(1);
    expect(parse.stdout).toContain('Anomalies: 12');
    expect(cli(['cases', 'boundary', '--out', join(dir, 'cases')]).status).toBe(0);
    expect(existsSync(join(dir, 'cases', 'boundary', 'expected.json'))).toBe(true);
  });

  it('exits 2 for a missing input file', () => {
    const r = cli(['parse', 'does-not-exist.jsonl']);
    expect(r.status).toBe(2);
    expect(r.stderr).toContain('input file not found');
  });
});
```

- [ ] **Step 2: 写测试 `tests/perf/perf.test.ts`**

```ts
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { generate } from '../../src/generator/generate.ts';
import { toJsonl } from '../../src/io/jsonl.ts';
import { parseFile } from '../../src/parser/parse.ts';

describe('performance smoke', () => {
  it('parses >= 100k events in under 5 s', async () => {
    const { events } = generate({ seed: 7, scenarios: 150 });
    expect(events.length).toBeGreaterThanOrEqual(100_000);
    const file = join(mkdtempSync(join(tmpdir(), 'adas-perf-')), 'big.jsonl');
    writeFileSync(file, toJsonl(events));
    const t0 = performance.now();
    const { report } = await parseFile(file);
    const ms = performance.now() - t0;
    expect(report.meta.events).toBe(events.length);
    expect(ms).toBeLessThan(5000);
  });

  it('keeps rule state bounded regardless of file size', async () => {
    for (const scenarios of [20, 80]) {
      const file = join(mkdtempSync(join(tmpdir(), 'adas-perf-')), 'seq.jsonl');
      writeFileSync(file, toJsonl(generate({ seed: 1, scenarios }).events));
      const { peakActiveScenarios } = await parseFile(file);
      expect(peakActiveScenarios).toBe(1);
    }
  });
});
```

- [ ] **Step 3: 运行**

Run: `npx vitest run tests/cli/e2e.test.ts && npm run test:perf`
Expected: e2e 2 passed；perf 2 passed，并打印耗时。若 150 个场景不足 10 万事件，把 `scenarios` 调到满足为止。

- [ ] **Step 4: 提交**

```bash
git add tests/cli/e2e.test.ts tests/perf
git commit -m "test(adas): add CLI end-to-end and performance smoke tests

Refs #69"
```

---

### Task 13: README、CHANGELOG、GitLab CI 示例

**Files:**
- Create: `README.md`, `README.zh.md`, `CHANGELOG.md`, `.gitlab-ci.yml`
- Test: `tests/gitlab-ci.test.ts`

- [ ] **Step 1: 写失败测试 `tests/gitlab-ci.test.ts`**

```ts
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
```

- [ ] **Step 2: 运行，确认失败**

Run: `npx vitest run tests/gitlab-ci.test.ts`
Expected: FAIL — ENOENT `.gitlab-ci.yml`

- [ ] **Step 3: 创建 `.gitlab-ci.yml`**

```yaml
# Example, not executed on GitLab. Mirrors the GitHub Actions `adas` job; assumes this directory is the repo root.
image: node:22

stages:
  - lint
  - test

variables:
  npm_config_cache: "$CI_PROJECT_DIR/.npm"

cache:
  key:
    files:
      - package-lock.json
  paths:
    - .npm/

default:
  before_script:
    - npm ci

lint:
  stage: lint
  script:
    - npm run lint
    - npm run typecheck

test:
  stage: test
  script:
    - npm run test:coverage
    - npm run demo
  coverage: '/Lines\s*:\s*(\d+\.?\d*)%/'
  artifacts:
    when: always
    paths:
      - coverage/
```

- [ ] **Step 4: 创建 `CHANGELOG.md`**

```markdown
# Changelog

All notable changes to this project are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: [SemVer](https://semver.org/).

## [0.1.0] - Unreleased

Ruleset version: 1.0.0

### Added

- `adas-lab gen`: seeded synthetic ACC / APA / HMI logs (JSON Lines) with truth labels
- `adas-lab parse`: streaming anomaly detection with data quality report and exit-code gates (`--fail-on`, `--strict`)
- Rules ACC-01, ACC-02, APA-01, APA-02, HMI-01, HMI-02
- `adas-lab cases boundary`: boundary value cases generated from rule thresholds
- GitHub Actions job and a GitLab CI example
```

- [ ] **Step 5: 创建 `README.md`（英文）**

````markdown
# ADAS Log Toolchain Lab

> Language: English | [中文](./README.zh.md)

TypeScript toolchain for ADAS-style logs: generate synthetic data, detect anomalies with a streaming rule engine, and generate test cases from rule thresholds.

> **Disclaimer:** simulation lab. All data is synthetic. Thresholds are illustrative, not calibration values of any vehicle. This project does not represent real-vehicle, test-fleet or production experience.

## Status

Phase 1 of 4 ([REQ-ADAS-01](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69)). Design: [spec](../docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md).

## Quick start

Requires Node.js >= 22.18 (runs `.ts` directly, no build step).

```bash
npm ci
npm run demo   # gen -> parse -> cases boundary
npm test
```

## Commands

| Command | What it does |
|---------|--------------|
| `node src/cli.ts gen [--seed 42] [--scenarios 20] [--interleave] [--out out]` | Write `events.jsonl` and `labels.json` |
| `node src/cli.ts parse <input> [--out file] [--config file] [--fail-on P1\|P2] [--strict]` | Write `anomalies.json` |
| `node src/cli.ts cases boundary [--out dir] [--config file]` | Write boundary cases and verify them |

Exit codes: `0` ok, `1` gate triggered, `2` usage error.

## Rules (ruleset 1.0.0)

| ID | Rule | Severity |
|----|------|----------|
| ACC-01 | Following distance below `max(5 m, speed × 1.0 s)` while ACTIVE | P1 |
| ACC-02 | ACC not exited within 200 ms after brake pedal | P1 |
| APA-01 | Obstacle < 0.3 m during maneuver without abort within 200 ms | P1 |
| APA-02 | Parking maneuver longer than 60 s or unfinished | P2 |
| HMI-01 | HMI state not shown (or wrong) within 300 ms of a state change | P2 |
| HMI-02 | Takeover request not shown within 200 ms | P1 |

Override thresholds with `--config rules.config.json`, e.g. `{"rules": {"ACC-01": {"timeGapS": 1.2}}}`.

## Quality gates

- Truth labels: precision and recall must be 100% on generated data
- Coverage: lines >= 90%, `src/rules/` 100%
- Performance smoke: 100k events < 5 s (`npm run test:perf`)

## Gap to production

No vehicle bus capture, storage or upload; no raw sensor data; synthetic scenarios only.
````

- [ ] **Step 6: 创建 `README.zh.md`（中文，与英文版结构一致）**

````markdown
# ADAS 日志工具链实验室

> 语言：[English](./README.md) | 中文

用 TypeScript 处理 ADAS 风格日志：生成合成数据、流式规则引擎检测异常、按规则阈值生成测试用例。

> **声明**：simulation lab，全部为合成数据。阈值为演示用合理值，不是任何车型的标定值。本项目不代表实车、测试车或量产项目经验。

## 状态

4 期中的第 1 期（[REQ-ADAS-01](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69)）。设计见 [spec](../docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md)。

## 快速开始

需要 Node.js >= 22.18（直接运行 `.ts`，无需构建）。

```bash
npm ci
npm run demo   # gen -> parse -> cases boundary
npm test
```

## 命令

| 命令 | 作用 |
|------|------|
| `node src/cli.ts gen [--seed 42] [--scenarios 20] [--interleave] [--out out]` | 生成 `events.jsonl` 和 `labels.json` |
| `node src/cli.ts parse <input> [--out file] [--config file] [--fail-on P1\|P2] [--strict]` | 生成 `anomalies.json` |
| `node src/cli.ts cases boundary [--out dir] [--config file]` | 生成并校验边界值用例 |

退出码：`0` 正常，`1` 门禁触发，`2` 用法错误。

## 规则（规则集 1.0.0）

| ID | 规则 | 级别 |
|----|------|------|
| ACC-01 | ACTIVE 时跟车距离低于 `max(5 m, 车速 × 1.0 s)` | P1 |
| ACC-02 | 踩刹车后 200 ms 内 ACC 未退出 | P1 |
| APA-01 | 泊车中障碍物 < 0.3 m 且 200 ms 内未中止 | P1 |
| APA-02 | 泊车超过 60 s 或未完成 | P2 |
| HMI-01 | 状态变化后 300 ms 内 HMI 未显示或显示错误 | P2 |
| HMI-02 | 接管请求 200 ms 内未显示 | P1 |

用 `--config rules.config.json` 覆盖阈值，例如 `{"rules": {"ACC-01": {"timeGapS": 1.2}}}`。

## 质量门槛

- 真值标签：生成数据上精确率、召回率均为 100%
- 覆盖率：行 ≥ 90%，`src/rules/` 100%
- 性能冒烟：10 万事件 < 5 s（`npm run test:perf`）

## 与生产环境的差距

没有车辆总线采集、存储与上传；没有原始传感器数据；场景全部为合成。
````

- [ ] **Step 7: 运行测试**

Run: `npx vitest run tests/gitlab-ci.test.ts`
Expected: 2 passed

- [ ] **Step 8: 提交**

```bash
git add README.md README.zh.md CHANGELOG.md .gitlab-ci.yml tests/gitlab-ci.test.ts
git commit -m "docs(adas): add bilingual README, changelog and GitLab CI example

Refs #69"
```

---

### Task 14: GitHub Actions、husky 钩子、覆盖率门槛

**Files:**
- Modify: `adas-log-toolchain-lab/vitest.config.ts`（开启门槛）
- Modify: `.github/workflows/ci.yml`（仓库根，追加 `adas` job）
- Modify: `.husky/pre-commit`（仓库根，追加 adas lint）

- [ ] **Step 1: 在 `vitest.config.ts` 的 `coverage` 中加入门槛**

```ts
      thresholds: {
        lines: 90,
        'src/rules/**.ts': { lines: 100, functions: 100, statements: 100 },
      },
```

- [ ] **Step 2: 运行覆盖率**

Run: `npm run test:coverage`
Expected: 全部测试 passed，门槛满足。若 `src/rules/` 某文件未到 100%，查看 `text` 报告中未覆盖的行号，在对应 `tests/rules/*.test.ts` 补用例（不降门槛）。

- [ ] **Step 3: 在 `.github/workflows/ci.yml` 末尾追加 `adas` job**（与 `edi` job 同级）

```yaml
  adas:
    name: Tests (adas-log-toolchain-lab)
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: adas-log-toolchain-lab
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-node@v7
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: adas-log-toolchain-lab/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm run test:coverage
      - run: npm run demo
```

- [ ] **Step 4: 在 `.husky/pre-commit` 末尾追加**

```sh

# adas-log-toolchain-lab: lint + typecheck
if echo "$STAGED" | grep -q "^adas-log-toolchain-lab/"; then
  echo "→ lint: adas-log-toolchain-lab"
  (cd adas-log-toolchain-lab && npm run lint && npm run typecheck)
fi
```

- [ ] **Step 5: 本地全量验证**（在 `adas-log-toolchain-lab/`）

Run: `npm ci && npm run lint && npm run typecheck && npm run test:coverage && npm run demo && npm run test:perf`
Expected: 全部通过，`demo` 退出码 0

- [ ] **Step 6: 提交（在仓库根目录执行）**

```bash
cd ..
git add adas-log-toolchain-lab/vitest.config.ts .github/workflows/ci.yml .husky/pre-commit
git commit -m "ci(adas): add GitHub Actions job, pre-commit lint and coverage gates

Refs #69"
```

---

## 4. 验证（Stage 4 入口条件）

| 检查 | 命令（`adas-log-toolchain-lab/`） | 期望 |
|------|-----------------------------------|------|
| Lint | `npm run lint` | 无错误 |
| 类型 | `npm run typecheck` | 无错误 |
| 测试 + 覆盖率 | `npm run test:coverage` | 全部 passed；行 ≥ 90%，`src/rules/` 100% |
| 真值 | 包含在测试中（`tests/truth.test.ts`） | 精确率 / 召回率 100% |
| 演示 | `npm run demo` | 12 条异常、26 个边界用例全部通过 |
| 性能 | `npm run test:perf` | ≥ 10 万事件 < 5 s；`peakActiveScenarios = 1` |
| CI | 推送后在 PR 上查看 `Tests (adas-log-toolchain-lab)` | 绿灯 |

对照 `REQUIREMENTS.md` §6 Phase 1 验收标准逐项勾选后，进入 Stage 4。

## 5. 不在本计划范围

Phase 2–4（`collector`、`casegen regression`、`replay`、LKA、`analysis`、`dashboard`、`ticket`）各自另写实施计划。Stage 5 的仓库登记更新（REQUIREMENTS / INDEX / register 状态、根 README 速查表、根 CLAUDE.md）在 Phase 1 PR 收尾时完成。
