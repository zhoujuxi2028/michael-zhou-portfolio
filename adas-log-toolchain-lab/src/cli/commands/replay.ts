import { parseFilter, replayManifest } from '../../replay/replay.ts';
import { writeJson, type RegisterCommand } from '../context.ts';

export const registerReplay: RegisterCommand = ({ program, io, setExitCode }) => {
  program.command('replay')
    .description('Replay regression cases from a manifest and report pass / fail')
    .requiredOption('--manifest <file>', 'cases/manifest.json from `cases regression`')
    .option('--out <file>', 'report path', 'out/replay-report.json')
    .option('--filter <expr>', 'only replay matching cases: module=<ACC|APA|HMI|LKA>')
    .action(async (o: { manifest: string; out: string; filter?: string }) => {
      const report = await replayManifest(o.manifest, { filter: parseFilter(o.filter) });
      await writeJson(o.out, report);
      for (const r of report.results.filter((x) => x.status === 'FAIL')) {
        io.err(`FAIL ${r.caseId}: expected ${r.ruleId} to fire as recorded; fired rules [${r.firedRules.join(', ')}]${r.dataQualityIssues ? `; ${r.dataQualityIssues} data quality issues` : ''}${r.error ? `; error ${r.error}` : ''}`);
      }
      const s = report.summary;
      io.out(`Replayed ${s.total} cases: ${s.passed} passed, ${s.failed} failed (pass rate ${(s.passRate * 100).toFixed(1)}%) -> ${o.out}`);
      setExitCode(s.failed > 0 ? 1 : 0);
    });
};
