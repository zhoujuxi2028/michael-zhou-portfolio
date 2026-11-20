import { InvalidArgumentError } from 'commander';
import { buildRegressionCases } from '../../casegen/regression.ts';
import { integer, type RegisterCommand } from '../context.ts';

const nonNegative = (v: string): number => {
  const n = integer(v);
  if (n < 0) throw new InvalidArgumentError('must be >= 0');
  return n;
};

export const registerCasesRegression: RegisterCommand = ({ cases, io }) => {
  cases.command('regression')
    .description('Turn detected anomalies into self-contained regression cases')
    .requiredOption('--anomalies <file>', 'anomalies.json from `parse`')
    .requiredOption('--log <file>', 'original JSON Lines log')
    .option('--out <dir>', 'output directory', 'out/cases')
    .option('--pre <ms>', 'window start before the anomaly startTs', nonNegative, 3000)
    .option('--post <ms>', 'window end after the anomaly detectTs', nonNegative, 3000)
    .action(async (o: { anomalies: string; log: string; out: string; pre: number; post: number }) => {
      const { manifest, manifestPath, testPath } = await buildRegressionCases({
        anomalies: o.anomalies, log: o.log, out: o.out, preMs: o.pre, postMs: o.post,
      });
      io.out(`${manifest.cases.length} regression cases written to ${manifestPath}; test file ${testPath}`);
    });
};
