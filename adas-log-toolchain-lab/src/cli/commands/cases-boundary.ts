import { join } from 'node:path';
import { buildBoundaryCases, verifyBoundaryCases, writeBoundaryCases } from '../../casegen/boundary.ts';
import { optionalConfig, type RegisterCommand } from '../context.ts';

export const registerCasesBoundary: RegisterCommand = ({ cases, io, setExitCode }) => {
  cases.command('boundary')
    .description('Generate boundary value cases from rule thresholds and verify them')
    .option('--out <dir>', 'output directory', 'out/cases')
    .option('--config <file>', 'rule config JSON')
    .action(async (o: { out: string; config?: string }) => {
      const cfg = await optionalConfig(o.config);
      const list = buildBoundaryCases(undefined, cfg);
      await writeBoundaryCases(o.out, list);
      const results = await verifyBoundaryCases(list, undefined, cfg);
      const failed = results.filter((r) => !r.pass);
      for (const f of failed) io.err(`FAIL ${f.caseId}: expected triggered=${f.expectTriggered}, got ${f.triggered}; fired rules [${f.firedRules.join(', ')}]; data quality issues ${f.dataQualityIssues}`);
      io.out(`${results.length} boundary cases written to ${join(o.out, 'boundary')}; ${results.length - failed.length} passed, ${failed.length} failed`);
      setExitCode(failed.length > 0 ? 1 : 0);
    });
};
