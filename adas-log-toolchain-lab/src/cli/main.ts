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
      const results = await verifyBoundaryCases(list, undefined, cfg);
      const failed = results.filter((r) => !r.pass);
      for (const f of failed) io.err(`FAIL ${f.caseId}: expected triggered=${f.expectTriggered}, got ${f.triggered}; fired rules [${f.firedRules.join(', ')}]; data quality issues ${f.dataQualityIssues}`);
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
