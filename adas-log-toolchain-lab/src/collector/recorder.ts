import { createEngine, type EngineConfig } from '../engine/engine.ts';
import type { Anomaly, AnyRule } from '../engine/types.ts';
import { RULES } from '../rules/index.ts';
import type { AdasEvent } from '../schema/events.ts';
import { createRingBuffer, type RingBuffer } from './ring-buffer.ts';

export interface RecorderOptions {
  bufferMs?: number;
  preMs?: number;
  postMs?: number;
  rules?: readonly AnyRule[];
  config?: EngineConfig;
}

export interface Clip {
  /** `<scenario>_<ts of first trigger>_<rule id | MANUAL>.jsonl` */
  file: string;
  scenarioId: string;
  trigger: 'rule' | 'manual';
  ruleIds: string[];
  /** Recorded window start (preconditions, if any, lie before it). */
  startTs: number;
  endTs: number;
  truncated: boolean;
  events: AdasEvent[];
}

export interface Recorder {
  readonly options: { bufferMs: number; preMs: number; postMs: number };
  push(e: AdasEvent): void;
  /** EOF: finish open scenarios and write their clips. */
  end(): void;
}

export const DEFAULTS = { bufferMs: 10000, preMs: 5000, postMs: 5000 } as const;

interface Window { start: number; end: number; trigger: 'rule' | 'manual'; label: string; labelTs: number; ruleIds: string[] }
interface Scenario { buffer: RingBuffer; now: number; window: Window | null }

/** Core recorder shared by file and live sources: ring buffer + rule/manual triggers → clips. */
export function createRecorder(opts: RecorderOptions, onClip: (c: Clip) => void): Recorder {
  const options = {
    bufferMs: opts.bufferMs ?? DEFAULTS.bufferMs,
    preMs: opts.preMs ?? DEFAULTS.preMs,
    postMs: opts.postMs ?? DEFAULTS.postMs,
  };
  const engine = createEngine(opts.rules ?? RULES, opts.config);
  const scenarios = new Map<string, Scenario>();

  const open = (id: string): Scenario => {
    let sc = scenarios.get(id);
    if (!sc) scenarios.set(id, (sc = { buffer: createRingBuffer(options.bufferMs), now: -Infinity, window: null }));
    return sc;
  };

  const close = (id: string, sc: Scenario) => {
    const w = sc.window;
    if (!w) return;
    sc.window = null;
    sc.buffer.pin(null);
    const events = sc.buffer.events().filter((e) => e.ts >= w.start && e.ts <= w.end);
    onClip({
      file: `${id}_${w.labelTs}_${w.label}.jsonl`, scenarioId: id, trigger: w.trigger, ruleIds: w.ruleIds,
      startTs: Math.max(0, w.start), endTs: Math.min(w.end, sc.now), truncated: false, events,
    });
  };

  const onAnomaly = (a: Anomaly) => {
    const sc = open(a.scenarioId);
    sc.window = {
      start: a.startTs - options.preMs, end: a.detectTs + options.postMs,
      trigger: 'rule', label: a.ruleId, labelTs: a.startTs, ruleIds: [a.ruleId],
    };
    sc.buffer.pin(sc.window.start);
  };

  return {
    options,
    push(e) {
      const sc = open(e.scenarioId);
      sc.now = Math.max(sc.now, e.ts);
      sc.buffer.push(e);
      engine.push(e).forEach(onAnomaly);
      if (sc.window && sc.now > sc.window.end) close(e.scenarioId, sc);
    },
    end() {
      engine.end().forEach(onAnomaly);
      for (const [id, sc] of scenarios) close(id, sc);
      scenarios.clear();
    },
  };
}
