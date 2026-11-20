import { mkdir, writeFile } from 'node:fs/promises';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { renderCaseTest, type CaseTestEntry } from '../casegen/template.ts';
import { resolveEpsilon, resolveRuleConfig, type EngineConfig } from '../engine/engine.ts';
import type { AnyRule } from '../engine/types.ts';
import { toJsonl } from '../io/jsonl.ts';
import { RULES } from '../rules/index.ts';
import { mapTicket } from './map.ts';
import type { Ticket } from './parse.ts';

const RUNNER = fileURLToPath(new URL('../casegen/case-runner.ts', import.meta.url));

export interface TaskSummary { mapped: number; unmapped: number }

const writeJson = (path: string, data: unknown) => writeFile(path, `${JSON.stringify(data, null, 2)}\n`);

/**
 * Write `<out>/<KEY>.json`, `data/<KEY>-<caseId>.jsonl`, `generated/<KEY>.test.ts` for mapped tickets
 * and `unmapped.json` for the rest. Repro data = the rule's boundary cases that must trigger.
 */
export async function generateTasks(
  tickets: readonly Ticket[],
  outDir: string,
  opts: { rules?: readonly AnyRule[]; config?: EngineConfig } = {},
): Promise<TaskSummary> {
  const rules = opts.rules ?? RULES;
  const config = opts.config ?? {};
  const dataDir = join(outDir, 'data');
  const genDir = join(outDir, 'generated');
  await mkdir(dataDir, { recursive: true });
  await mkdir(genDir, { recursive: true });
  const runnerImport = relative(genDir, RUNNER).replaceAll('\\', '/');
  const unmapped: { key: string; reason: string }[] = [];
  let mapped = 0;
  for (const ticket of tickets) {
    const m = mapTicket(ticket, rules);
    if (!m.ok) {
      unmapped.push({ key: m.key, reason: m.reason });
      continue;
    }
    mapped++;
    const rule = rules.find((r) => r.id === m.task.ruleId) as AnyRule;
    const cases = rule.boundaryCases(resolveRuleConfig(rule, config), resolveEpsilon(config)).filter((c) => c.expectTriggered);
    const entries: CaseTestEntry[] = [];
    for (const c of cases) {
      const name = `${m.task.key}-${c.caseId}.jsonl`;
      await writeFile(join(dataDir, name), toJsonl(c.events));
      entries.push({ caseId: c.caseId, ruleId: rule.id, file: `../data/${name}`, expectTriggered: true, description: c.description });
    }
    const testFile = `generated/${m.task.key}.test.ts`;
    await writeFile(join(outDir, testFile), renderCaseTest(entries, { title: `${m.task.key}: ${m.task.summary}`, runnerImport }));
    await writeJson(join(outDir, `${m.task.key}.json`), { ...m.task, cases: entries.map((e) => e.caseId), testFile });
  }
  await writeJson(join(outDir, 'unmapped.json'), unmapped);
  return { mapped, unmapped: unmapped.length };
}
