import { ev, scenarioEnd, type AccState, type AdasEvent, type ApaStep, type LkaState } from '../schema/events.ts';
import { acc01 } from '../rules/acc-01.ts';
import { createRng, type Rng } from './prng.ts';

export interface Label { scenarioId: string; ruleId: string; startTs: number; endTs: number }
export interface GenOptions { seed: number; scenarios: number; interleave?: boolean }
export interface GenResult { events: AdasEvent[]; labels: Label[] }

export const MIN_SCENARIOS = 8;

const ACC_END_MS = 60000;
const TRACK_MS = 50;
const OBSTACLE_MS = 100;
const SLOT_A = 15000;
const SLOT_B = 40000;
const LANE_MS = 100;
const LKA_END_MS = 60000;
/** First scenarios keep the Phase 1 layout (even ACC, odd APA); later ones cycle ACC, APA, LKA. */
const PHASE1_SCENARIOS = 8;

type AccFault = 'ACC-01' | 'ACC-02' | 'HMI-01-wrong' | 'HMI-01-missing' | 'HMI-02-late' | 'HMI-02-missing';
type ApaFault = 'APA-01' | 'APA-02-timeout' | 'APA-02-unfinished';

const ACC_PLAN: readonly (readonly [AccFault, AccFault])[] = [
  ['ACC-01', 'ACC-02'],
  ['HMI-01-wrong', 'HMI-02-late'],
  ['ACC-01', 'HMI-01-missing'],
  ['ACC-02', 'HMI-02-missing'],
];
const APA_PLAN: readonly ApaFault[] = ['APA-01', 'APA-01', 'APA-02-timeout', 'APA-02-unfinished'];

type LkaFault = 'LKA-01' | 'LKA-02';
const LKA_PLAN: readonly (readonly [LkaFault, LkaFault])[] = [
  ['LKA-01', 'LKA-02'],
  ['LKA-02', 'LKA-01'],
];

const round2 = (x: number) => Math.round(x * 100) / 100;
const byTs = (es: readonly AdasEvent[]) => [...es].sort((a, b) => a.ts - b.ts);

export function generate(o: GenOptions): GenResult {
  if (!Number.isInteger(o.seed)) throw new RangeError('seed must be an integer');
  if (!Number.isInteger(o.scenarios) || o.scenarios < MIN_SCENARIOS) {
    throw new RangeError(`scenarios must be an integer >= ${MIN_SCENARIOS}`);
  }
  const events: AdasEvent[] = [];
  const labels: Label[] = [];
  for (let i = 0; i < o.scenarios; i++) {
    const id = `S${String(i + 1).padStart(3, '0')}`;
    const rng = createRng((o.seed + Math.imul(i + 1, 0x9e3779b9)) >>> 0);
    const kind = i < PHASE1_SCENARIOS ? (i % 2 === 0 ? 'ACC' : 'APA') : (['ACC', 'APA', 'LKA'] as const)[(i - PHASE1_SCENARIOS) % 3];
    const sc = kind === 'ACC'
      ? accScenario(id, rng, ACC_PLAN[i / 2] ?? null)
      : kind === 'APA'
        ? apaScenario(id, rng, APA_PLAN[(i - 1) / 2] ?? null)
        : lkaScenario(id, rng, LKA_PLAN[Math.floor((i - PHASE1_SCENARIOS) / 3)] ?? null);
    for (const e of byTs(sc.events)) events.push(e);
    labels.push(...sc.labels);
  }
  return { events: o.interleave ? byTs(events) : events, labels };
}

function accScenario(id: string, rng: Rng, faults: readonly [AccFault, AccFault] | null): GenResult {
  const events: AdasEvent[] = [];
  const labels: Label[] = [];
  const label = (ruleId: string, startTs: number, endTs: number) => labels.push({ scenarioId: id, ruleId, startTs, endTs });
  const display = (ts: number, value: string) =>
    events.push(ev('HMI/DISPLAY', ts, id, { feature: 'ACC', item: 'STATE', value }));
  const state = (ts: number, from: AccState, to: AccState, reason: string, shown: 'ok' | 'none' = 'ok') => {
    events.push(ev('ACC/STATE', ts, id, { from, to, reason }));
    if (shown === 'ok') display(ts + rng.int(50, 150), to);
  };
  const brake = (ts: number, pressed: boolean) => events.push(ev('ACC/BRAKE_PEDAL', ts, id, { pressed }));
  const takeover = (ts: number, showAt: number | null) => {
    events.push(ev('ACC/TAKEOVER_REQUEST', ts, id, { reason: 'sensor_degraded' }));
    if (showAt !== null) events.push(ev('HMI/DISPLAY', showAt, id, { feature: 'ACC', item: 'TAKEOVER', value: 'SHOWN' }));
  };

  // Clean script.
  state(0, 'OFF', 'STANDBY', 'driver_on');
  state(1000, 'STANDBY', 'ACTIVE', 'driver_set');
  brake(28000, true);
  state(28100, 'ACTIVE', 'STANDBY', 'brake');
  brake(28600, false);
  state(30000, 'STANDBY', 'ACTIVE', 'resume');
  takeover(50000, 50000 + rng.int(50, 150));
  state(58000, 'ACTIVE', 'STANDBY', 'driver_off');

  // Fault injections: each triggers only its target rule.
  const tooClose = new Set<number>();
  const inject = (f: AccFault, s: number) => {
    switch (f) {
      case 'ACC-01':
        for (let t = s; t <= s + 1000; t += TRACK_MS) tooClose.add(t);
        label('ACC-01', s, s + 1000);
        break;
      case 'ACC-02':
        brake(s, true);
        state(s + 500, 'ACTIVE', 'STANDBY', 'late_exit');
        brake(s + 700, false);
        state(s + 2000, 'STANDBY', 'ACTIVE', 'resume');
        label('ACC-02', s, s + 200);
        break;
      case 'HMI-01-wrong':
        state(s, 'ACTIVE', 'OVERRIDE', 'driver_accel', 'none');
        display(s + 100, 'ACTIVE');
        state(s + 1500, 'OVERRIDE', 'ACTIVE', 'accel_release');
        label('HMI-01', s, s + 300);
        break;
      case 'HMI-01-missing':
        state(s, 'ACTIVE', 'OVERRIDE', 'driver_accel', 'none');
        state(s + 1500, 'OVERRIDE', 'ACTIVE', 'accel_release');
        label('HMI-01', s, s + 300);
        break;
      case 'HMI-02-late':
        takeover(s, s + 450);
        label('HMI-02', s, s + 200);
        break;
      case 'HMI-02-missing':
        takeover(s, null);
        label('HMI-02', s, s + 200);
        break;
    }
  };
  if (faults) {
    inject(faults[0], SLOT_A);
    inject(faults[1], SLOT_B);
  }

  const v0 = rng.range(15, 25);
  for (let t = 0; t < ACC_END_MS; t += TRACK_MS) {
    const egoSpeed = round2(v0 + rng.range(-0.2, 0.2));
    const threshold = Math.max(acc01.defaults.minGapM, egoSpeed * acc01.defaults.timeGapS);
    const leadDistance = tooClose.has(t) ? round2(threshold - 1.5) : round2(threshold + 8 + rng.range(0, 2));
    events.push(ev('ACC/TRACK', t, id, { egoSpeed, setSpeed: round2(v0 + 2), leadDistance, leadRelSpeed: round2(rng.range(-0.5, 0.5)) }));
  }
  events.push(scenarioEnd(id, ACC_END_MS));
  return { events, labels };
}

function apaScenario(id: string, rng: Rng, fault: ApaFault | null): GenResult {
  const events: AdasEvent[] = [];
  const labels: Label[] = [];
  const step = (ts: number, s: ApaStep, reason: string) => {
    events.push(ev('APA/STEP', ts, id, { step: s, reason }));
    events.push(ev('HMI/DISPLAY', ts + rng.int(50, 150), id, { feature: 'APA', item: 'STATE', value: s }));
  };

  step(0, 'SEARCHING', 'driver_request');
  step(3000, 'SLOT_SELECTED', 'slot_found');
  const start = 4000;
  step(start, 'MANEUVERING', 'start');

  let stop: number;
  let terminal: ApaStep | null = 'COMPLETED';
  const tooClose = new Set<number>();
  switch (fault) {
    case 'APA-01':
      for (let t = 14000; t <= 14400; t += OBSTACLE_MS) tooClose.add(t);
      stop = 14450;
      terminal = 'ABORTED';
      labels.push({ scenarioId: id, ruleId: 'APA-01', startTs: 14000, endTs: 14200 });
      break;
    case 'APA-02-timeout':
      stop = 69000;
      labels.push({ scenarioId: id, ruleId: 'APA-02', startTs: start, endTs: start + 60000 });
      break;
    case 'APA-02-unfinished':
      stop = 30000;
      terminal = null;
      labels.push({ scenarioId: id, ruleId: 'APA-02', startTs: start, endTs: stop });
      break;
    default:
      stop = start + rng.int(200, 400) * OBSTACLE_MS;
  }

  for (let t = start; t < stop; t += OBSTACLE_MS) {
    events.push(ev('APA/OBSTACLE', t, id, { minDistance: tooClose.has(t) ? 0.2 : round2(0.8 + rng.range(0, 1)) }));
  }
  if (terminal) step(stop, terminal, terminal === 'ABORTED' ? 'obstacle' : 'parked');
  events.push(scenarioEnd(id, terminal ? stop + 2000 : stop));
  return { events, labels };
}

function lkaScenario(id: string, rng: Rng, faults: readonly [LkaFault, LkaFault] | null): GenResult {
  const events: AdasEvent[] = [];
  const labels: Label[] = [];
  const label = (ruleId: string, startTs: number, endTs: number) => labels.push({ scenarioId: id, ruleId, startTs, endTs });
  const state = (ts: number, from: LkaState, to: LkaState) => {
    events.push(ev('LKA/STATE', ts, id, { from, to }));
    events.push(ev('HMI/DISPLAY', ts + rng.int(50, 150), id, { feature: 'LKA', item: 'STATE', value: to }));
  };
  const signal = (ts: number, side: 'LEFT' | 'RIGHT' | 'OFF') => events.push(ev('VEH/TURN_SIGNAL', ts, id, { side }));
  const unavailable = (ts: number) =>
    events.push(ev('HMI/DISPLAY', ts, id, { feature: 'LKA', item: 'UNAVAILABLE', value: 'SHOWN' }));

  // Clean script: normal turn signal use with gentle steering, a short (<= 500 ms) lane loss,
  // and a long lane loss that is properly indicated.
  state(0, 'OFF', 'STANDBY');
  state(1000, 'STANDBY', 'ACTIVE');
  signal(22000, 'LEFT');
  signal(24000, 'OFF');
  const lowLane = new Set<number>();
  const strongSteer = new Set<number>();
  const loss = (from: number, to: number) => {
    for (let t = from; t <= to; t += LANE_MS) lowLane.add(t);
  };
  loss(28000, 28400);
  loss(48000, 49000);
  unavailable(48000 + rng.int(300, 500));
  state(58000, 'ACTIVE', 'STANDBY');

  // Fault injections: each triggers only its target rule.
  const inject = (f: LkaFault, s: number) => {
    if (f === 'LKA-01') {
      signal(s - 500, 'LEFT');
      signal(s + 1500, 'OFF');
      for (let t = s; t <= s + 500; t += LANE_MS) strongSteer.add(t);
      label('LKA-01', s, s + 500);
    } else {
      loss(s, s + 1000);
      label('LKA-02', s, s + 800);
    }
  };
  if (faults) {
    inject(faults[0], SLOT_A);
    inject(faults[1], SLOT_B);
  }

  for (let t = 0; t < LKA_END_MS; t += LANE_MS) {
    const low = lowLane.has(t);
    events.push(ev('LKA/LANE', t, id, {
      leftConf: low ? round2(rng.range(0.1, 0.3)) : round2(rng.range(0.8, 1)),
      rightConf: round2(rng.range(0.8, 1)),
      offset: round2(rng.range(-0.3, 0.3)),
    }));
    const torque = strongSteer.has(t) ? round2(rng.range(0.8, 1.2)) : round2(rng.range(-0.3, 0.3));
    events.push(ev('LKA/STEER', t, id, { torque }));
  }
  events.push(scenarioEnd(id, LKA_END_MS));
  return { events, labels };
}
