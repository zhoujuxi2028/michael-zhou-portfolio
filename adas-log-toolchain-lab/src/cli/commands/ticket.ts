import { readFile } from 'node:fs/promises';
import { UsageError } from '../../config.ts';
import { generateTasks } from '../../ticket/tasks.ts';
import { parseExport } from '../../ticket/parse.ts';
import { optionalConfig, type RegisterCommand } from '../context.ts';

export const registerTicket: RegisterCommand = ({ program, io, setExitCode }) => {
  program.command('ticket')
    .description('Turn a synthetic Jira export into script tasks and check scripts')
    .argument('<export>', 'Jira export JSON')
    .option('--out <dir>', 'output directory', 'out/tasks')
    .option('--config <file>', 'rule config JSON')
    .option('--strict', 'exit 1 if any ticket is unmapped')
    .action(async (file: string, o: { out: string; config?: string; strict?: boolean }) => {
      let text: string;
      try {
        text = await readFile(file, 'utf8');
      } catch {
        throw new UsageError(`ticket export not found: ${file}`);
      }
      const tickets = parseExport(text);
      const r = await generateTasks(tickets, o.out, { config: await optionalConfig(o.config) });
      io.out(`${tickets.length} tickets: ${r.mapped} mapped, ${r.unmapped} unmapped; tasks written to ${o.out}`);
      if (r.unmapped > 0) io.out(`See ${o.out}/unmapped.json for reasons`);
      setExitCode(o.strict && r.unmapped > 0 ? 1 : 0);
    });
};
