import { mkdir, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { type Command, InvalidArgumentError } from 'commander';
import { loadConfig } from '../config.ts';
import type { EngineConfig } from '../engine/engine.ts';

export interface Io { out(s: string): void; err(s: string): void }

/** Shared state handed to every command module. */
export interface CliContext {
  program: Command;
  /** Parent of `cases <kind>` subcommands. */
  cases: Command;
  io: Io;
  setExitCode(code: number): void;
}

/** A command module registers its commands on the context; listed in `commands/index.ts`. */
export type RegisterCommand = (ctx: CliContext) => void;

export function integer(v: string): number {
  const n = Number(v);
  if (!Number.isInteger(n)) throw new InvalidArgumentError('must be an integer');
  return n;
}

export async function writeJson(path: string, data: unknown): Promise<void> {
  await mkdir(dirname(path), { recursive: true });
  await writeFile(path, `${JSON.stringify(data, null, 2)}\n`);
}

export const optionalConfig = (path: string | undefined): Promise<EngineConfig> =>
  (path ? loadConfig(path) : Promise.resolve({}));
