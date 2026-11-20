import { Command, CommanderError } from 'commander';
import { UsageError } from '../config.ts';
import { TOOL_VERSION } from '../version.ts';
import { COMMANDS } from './commands/index.ts';
import type { Io } from './context.ts';

export type { Io } from './context.ts';
export { exitCodeFor } from './commands/parse.ts';

const processIo: Io = { out: (s) => process.stdout.write(`${s}\n`), err: (s) => process.stderr.write(`${s}\n`) };

/** Run the CLI; returns the process exit code (0 ok, 1 gate triggered, 2 usage error). */
export async function main(argv: readonly string[], io: Io = processIo): Promise<number> {
  let code = 0;
  const program = new Command('adas-lab')
    .description('ADAS log toolchain lab (synthetic data only)')
    .version(TOOL_VERSION)
    .exitOverride()
    .configureOutput({ writeOut: (s) => io.out(s.trimEnd()), writeErr: (s) => io.err(s.trimEnd()) });
  const cases = program.command('cases').description('Generate test cases');
  const ctx = { program, cases, io, setExitCode: (c: number) => { code = c; } };
  for (const register of COMMANDS) register(ctx);

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
