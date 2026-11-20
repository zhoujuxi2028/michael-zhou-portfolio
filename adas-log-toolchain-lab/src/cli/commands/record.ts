import { access } from 'node:fs/promises';
import { InvalidArgumentError, Option } from 'commander';
import { DEFAULTS } from '../../collector/recorder.ts';
import { recordSession, type SessionResult } from '../../collector/session.ts';
import { fileSource, liveSource } from '../../collector/source.ts';
import { UsageError } from '../../config.ts';
import { integer, type Io, type RegisterCommand } from '../context.ts';

function positiveInteger(v: string): number {
  const n = Number(v);
  if (!Number.isInteger(n) || n <= 0) throw new InvalidArgumentError('must be a positive integer');
  return n;
}

/** pre/post may be 0 (a window of exactly the anomaly), matching `cases regression`. */
function nonNegativeInteger(v: string): number {
  const n = Number(v);
  if (!Number.isInteger(n) || n < 0) throw new InvalidArgumentError('must be a non-negative integer');
  return n;
}

function positiveNumber(v: string): number {
  const n = Number(v);
  if (!Number.isFinite(n) || n <= 0) throw new InvalidArgumentError('must be a positive number');
  return n;
}

interface RecordOpts {
  source: 'file' | 'live';
  input?: string;
  seed: number;
  scenarios: number;
  speed: number;
  out: string;
  buffer: number;
  pre: number;
  post: number;
}

const pct = (rate: number) => `${Math.round(rate * 10000) / 100}%`;

function printSummary(io: Io, r: SessionResult, from: string, out: string): void {
  const clips = r.index.clips;
  const rule = clips.filter((c) => c.trigger === 'rule').length;
  const { totals: q } = r.quality;
  const d = r.dataQuality;
  io.out(`Recorded ${r.events} events in ${r.quality.byScenario.length} scenarios from ${from}`);
  io.out(`Clips: ${clips.length} (rule ${rule}, manual ${clips.length - rule}, truncated ${clips.filter((c) => c.truncated).length})`);
  io.out(`Quality: TRACK frames ${q.trackFrames}, dropped ${q.droppedFrames} (${pct(q.dropRate)}), duplicates ${q.duplicates}, ts anomalies ${q.tsAnomalies}`);
  io.out(`Skipped lines: invalid JSON ${d.invalidJson}, schema errors ${d.schemaErrors}, unknown events ${d.unknownEvents}, out of order ${d.outOfOrder}`);
  io.out(`Clips written to ${out}`);
}

export const registerRecord: RegisterCommand = ({ program, io }) => {
  program.command('record')
    .description('Record trigger-based clips from a log file or the live generator stream')
    .addOption(new Option('--source <kind>', 'data source').choices(['file', 'live']).default('file'))
    .option('--input <jsonl>', 'input log (--source file)')
    .option('--seed <n>', 'generator seed (--source live)', integer, 42)
    .option('--scenarios <n>', 'generator scenarios (--source live)', positiveInteger, 20)
    .option('--speed <n>', 'live playback speed factor', positiveNumber, 1)
    .option('--out <dir>', 'output directory', 'out/clips')
    .option('--buffer <ms>', 'ring buffer length per scenario', positiveInteger, DEFAULTS.bufferMs)
    .option('--pre <ms>', 'time recorded before the anomaly start', nonNegativeInteger, DEFAULTS.preMs)
    .option('--post <ms>', 'time recorded after detection', nonNegativeInteger, DEFAULTS.postMs)
    .action(async (o: RecordOpts) => {
      const recorder = { out: o.out, bufferMs: o.buffer, preMs: o.pre, postMs: o.post };
      let result: SessionResult, from: string;
      if (o.source === 'file') {
        if (!o.input) throw new UsageError('--source file requires --input <jsonl>');
        try {
          await access(o.input);
        } catch {
          throw new UsageError(`input file not found: ${o.input}`);
        }
        from = o.input;
        result = await recordSession(fileSource(o.input), { ...recorder, source: o.input });
      } else {
        if (o.input) throw new UsageError('--input is only valid with --source file');
        from = `live (seed ${o.seed}, speed ${o.speed}x)`;
        try {
          result = await recordSession(liveSource({ seed: o.seed, scenarios: o.scenarios, speed: o.speed }), { ...recorder, source: 'live' });
        } catch (e) {
          if (e instanceof RangeError) throw new UsageError(e.message);
          throw e;
        }
      }
      printSummary(io, result, from, o.out);
    });
};
