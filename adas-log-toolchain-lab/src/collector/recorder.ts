import { createEngine, type EngineConfig } from '../engine/engine.ts';
import type { Anomaly, AnyRule } from '../engine/types.ts';
import { RULES } from '../rules/index.ts';
import type { AdasEvent } from '../schema/events.ts';
import { MAX_GAP_MS } from './quality.ts';
import { createPreconditionTracker, type PreconditionTracker } from './preconditions.ts';
import { createRingBuffer, type RingBuffer } from './ring-buffer.ts';

export interface RecorderOptions {
  bufferMs?: number;
  preMs?: number;
  postMs?: number;
  /**
   * Upper bound for a merged clip's window (end − start). A merge that would exceed it closes the
   * current clip and starts a new one. Default 60 s + pre + post: the window of an APA-02 60 s
   * timeout. A single trigger window is never split, whatever its length.
   */
  maxClipMs?: number;
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
  readonly options: { bufferMs: number; preMs: number; postMs: number; maxClipMs: number };
  push(e: AdasEvent): void;
  /** EOF: finish open scenarios and write their clips. */
  end(): void;
  /** Scenarios with live state (released at SCENARIO_END / EOF). */
  activeScenarios(): number;
  /** Events held across all ring buffers. */
  bufferedEvents(): number;
}

export const DEFAULTS = { bufferMs: 10000, preMs: 5000, postMs: 5000 } as const;
/** Added to pre + post for the default `maxClipMs`. */
export const DEFAULT_CLIP_CORE_MS = 60000;

interface Window { start: number; end: number; trigger: 'rule' | 'manual'; label: string; labelTs: number; ruleIds: string[] }
/** `jump`: an event far ahead of `now`, held until the next event shows whether it is a spike. */
interface Scenario { buffer: RingBuffer; states: PreconditionTracker; now: number; window: Window | null; jump: AdasEvent | null }

/**
 * Core recorder shared by file and live sources: ring buffer + rule/manual triggers → clips.
 *
 * - Window: `[startTs − pre, detectTs + post]` (manual: around the trigger ts); recording stops once
 *   scenario time passes its end, at SCENARIO_END or at EOF.
 * - Merging: overlapping windows in a scenario merge only while the first clip is still recording.
 *   A later trigger whose window overlaps an already closed clip gets a new clip (the two files
 *   share events). Merges are capped at `maxClipMs`.
 * - Scenario time is the max accepted ts. An event more than `MAX_GAP_MS` (the C3 ts-anomaly gap)
 *   ahead of it is held back for one event: if the next event of the scenario continues from it
 *   (ts >= held − MAX_GAP_MS) the jump is real and both are recorded in order; otherwise the held
 *   event is a lone ts spike and is dropped (not buffered, not seen by the rules), so it cannot
 *   close windows early or flush the buffer. A held event at EOF is kept. SCENARIO_END is never
 *   held. `quality.json` still counts the spike as two ts anomalies.
 */
export function createRecorder(opts: RecorderOptions, onClip: (c: Clip) => void): Recorder {
  const preMs = opts.preMs ?? DEFAULTS.preMs;
  const postMs = opts.postMs ?? DEFAULTS.postMs;
  const options = {
    bufferMs: opts.bufferMs ?? DEFAULTS.bufferMs,
    preMs,
    postMs,
    maxClipMs: opts.maxClipMs ?? DEFAULT_CLIP_CORE_MS + preMs + postMs,
  };
  const engine = createEngine(opts.rules ?? RULES, opts.config);
  const scenarios = new Map<string, Scenario>();

  const open = (id: string): Scenario => {
    let sc = scenarios.get(id);
    if (!sc) {
      const states = createPreconditionTracker();
      const buffer = createRingBuffer(options.bufferMs, states.evicted);
      scenarios.set(id, (sc = { buffer, states, now: -Infinity, window: null, jump: null }));
    }
    return sc;
  };

  const close = (id: string, sc: Scenario) => {
    const w = sc.window;
    if (!w) return;
    sc.window = null;
    // Extract before unpinning: unpinning evicts anything older than bufferMs.
    const buffered = sc.buffer.events();
    const events = buffered.filter((e) => e.ts >= w.start && e.ts <= w.end);
    const preconditions = sc.states.before(w.start, buffered);
    // The pin stops eviction at w.start, so data inside the window was lost only if it was
    // evicted before the window opened (or before a merge moved the start back).
    const truncated = w.start <= sc.buffer.evictedUpTo();
    sc.buffer.pin(null);
    const startTs = truncated ? events.reduce((m, e) => Math.min(m, e.ts), Infinity) : Math.max(0, w.start);
    onClip({
      file: `${id}_${w.labelTs}_${w.label}.jsonl`, scenarioId: id, trigger: w.trigger, ruleIds: w.ruleIds,
      startTs, endTs: Math.min(w.end, sc.now), truncated, events: [...preconditions, ...events],
    });
  };

  /**
   * Start a window, or merge it into the one still recording when they overlap. Triggers run
   * before the close check for the same event, so after a ts jump the open window may already
   * be over: close it first. A merge that would make the window longer than `maxClipMs` closes
   * the current clip (ending at the current scenario time) and starts a new one, so a chain of
   * overlapping triggers cannot pin the ring buffer forever.
   */
  const trigger = (id: string, next: Window) => {
    const sc = open(id);
    const w = sc.window;
    const mergedMs = w ? Math.max(w.end, next.end) - Math.min(w.start, next.start) : 0;
    if (w && (next.start > w.end || mergedMs > options.maxClipMs)) close(id, sc);
    const cur = sc.window;
    if (cur) {
      cur.start = Math.min(cur.start, next.start);
      cur.end = Math.max(cur.end, next.end);
      for (const r of next.ruleIds) if (!cur.ruleIds.includes(r)) cur.ruleIds.push(r);
    } else {
      sc.window = next;
    }
    sc.buffer.pin((cur ?? next).start);
  };

  const onAnomaly = (a: Anomaly) => trigger(a.scenarioId, {
    start: a.startTs - options.preMs, end: a.detectTs + options.postMs,
    trigger: 'rule', label: a.ruleId, labelTs: a.startTs, ruleIds: [a.ruleId],
  });

  const accept = (e: AdasEvent) => {
    const sc = open(e.scenarioId);
    sc.now = Math.max(sc.now, e.ts);
    sc.buffer.push(e);
    if (e.module === 'SYS' && e.type === 'MANUAL_TRIGGER') {
      trigger(e.scenarioId, {
        start: e.ts - options.preMs, end: e.ts + options.postMs,
        trigger: 'manual', label: 'MANUAL', labelTs: e.ts, ruleIds: [],
      });
    }
    engine.push(e).forEach(onAnomaly);
    if (e.module === 'SYS' && e.type === 'SCENARIO_END') {
      close(e.scenarioId, sc);
      scenarios.delete(e.scenarioId);
    } else if (sc.window && sc.now > sc.window.end) {
      close(e.scenarioId, sc);
    }
  };

  return {
    options,
    push(e) {
      const sc = open(e.scenarioId);
      const held = sc.jump;
      if (held) {
        sc.jump = null;
        // The jump is real if recording continues from it; otherwise it was a lone spike: drop it.
        if (e.ts >= held.ts - MAX_GAP_MS) accept(held);
      }
      const isEnd = e.module === 'SYS' && e.type === 'SCENARIO_END';
      if (!isEnd && Number.isFinite(sc.now) && e.ts - sc.now > MAX_GAP_MS) sc.jump = e;
      else accept(e);
    },
    end() {
      for (const sc of [...scenarios.values()]) if (sc.jump) accept(sc.jump);
      engine.end().forEach(onAnomaly);
      for (const [id, sc] of scenarios) close(id, sc);
      scenarios.clear();
    },
    activeScenarios: () => scenarios.size,
    bufferedEvents: () => [...scenarios.values()].reduce((n, sc) => n + sc.buffer.size(), 0),
  };
}
