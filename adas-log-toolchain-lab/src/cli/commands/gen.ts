import { mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { UsageError } from '../../config.ts';
import { generate } from '../../generator/generate.ts';
import { toJsonl } from '../../io/jsonl.ts';
import { integer, writeJson, type RegisterCommand } from '../context.ts';

export const registerGen: RegisterCommand = ({ program, io }) => {
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
};
