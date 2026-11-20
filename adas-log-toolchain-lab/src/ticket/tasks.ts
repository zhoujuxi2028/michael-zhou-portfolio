import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { renderCaseTest, type CaseTestEntry } from '../casegen/template.ts';
import { resolveEpsilon, resolveRuleConfig, type EngineConfig } from '../engine/engine.ts';
import type { AnyRule } from '../engine/types.ts';
import { toJsonl } from '../io/jsonl.ts';
import { startOwnedOutputs } from '../io/ownership.ts';
import { RULES } from '../rules/index.ts';
import { mapTicket } from './map.ts';
import type { Ticket } from './parse.ts';

const RUNNER = fileURLToPath(new URL('../casegen/case-runner.ts', import.meta.url));

export interface TaskSummary { mapped: number; unmapped: number }

const json = (data: unknown) => `${JSON.stringify(data, null, 2)}\n`;

/**
 * Write `<out>/<KEY>.json`, `data/<KEY>-<caseId>.jsonl`, `generated/<KEY>.test.ts` for mapped tickets
 * and `unmapped.json` for the rest. Repro data = the rule's boundary cases that must trigger.
 * Before writing, the files the previous `ticket` run wrote (listed in `.adas-ticket-outputs.json`)
 * are removed; nothing else under `outDir` is touched.
 */
export async function generateTasks(
  tickets: readonly Ticket[],
  outDir: string,
  opts: { rules?: readonly AnyRule[]; config?: EngineConfig } = {},
): Promise<TaskSummary> {
  const rules = opts.rules ?? RULES;
  const config = opts.config ?? {};
  const genDir = join(outDir, 'generated');
  const owned = await startOwnedOutputs(outDir, 'ticket');
  await owned.mkdir('data');
  await owned.mkdir('generated');
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
      await owned.write(`data/${name}`, toJsonl(c.events));
      entries.push({ caseId: c.caseId, ruleId: rule.id, file: `../data/${name}`, expectTriggered: true, description: c.description });
    }
    const testFile = `generated/${m.task.key}.test.ts`;
    await owned.write(testFile, renderCaseTest(entries, { title: `${m.task.key}: ${m.task.summary}`, runnerImport }));
    await owned.write(`${m.task.key}.json`, json({ ...m.task, cases: entries.map((e) => e.caseId), testFile }));
  }
  await owned.write('unmapped.json', json(unmapped));
  await owned.finish();
  return { mapped, unmapped: unmapped.length };
}
