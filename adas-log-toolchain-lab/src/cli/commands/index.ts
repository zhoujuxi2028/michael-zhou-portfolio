import type { RegisterCommand } from '../context.ts';
import { registerCasesBoundary } from './cases-boundary.ts';
import { registerCasesRegression } from './cases-regression.ts';
import { registerGen } from './gen.ts';
import { registerParse } from './parse.ts';
import { registerRecord } from './record.ts';
import { registerReplay } from './replay.ts';
import { registerTicket } from './ticket.ts';

/** Every CLI command module, in help order. Add one line per new command module. */
export const COMMANDS: readonly RegisterCommand[] = [
  registerGen,
  registerParse,
  registerRecord,
  registerCasesBoundary,
  registerCasesRegression,
  registerReplay,
  registerTicket,
];
