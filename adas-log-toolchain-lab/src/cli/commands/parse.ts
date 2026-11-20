import { access } from 'node:fs/promises';
import { Option } from 'commander';
import { UsageError } from '../../config.ts';
import { parseFile, type Report } from '../../parser/parse.ts';
import { hasQualityIssues } from '../../parser/scan.ts';
import { optionalConfig, writeJson, type Io, type RegisterCommand } from '../context.ts';

type FailOn = 'P1' | 'P2';

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

export const registerParse: RegisterCommand = ({ program, io, setExitCode }) => {
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
      const { report } = await parseFile(input, { config: await optionalConfig(o.config) });
      await writeJson(o.out, report);
      printSummary(io, report, o.out);
      setExitCode(exitCodeFor(report, o.failOn, o.strict));
    });
};
