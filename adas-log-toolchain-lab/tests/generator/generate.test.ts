import { describe, expect, it } from 'vitest';
import { generate, MIN_SCENARIOS } from '../../src/generator/generate.ts';
import { parseLine } from '../../src/schema/validate.ts';

describe('generate', () => {
  it('is deterministic for the same seed', () => {
    expect(generate({ seed: 42, scenarios: 20 })).toEqual(generate({ seed: 42, scenarios: 20 }));
  });

  it('changes values (not structure) across seeds', () => {
    const a = generate({ seed: 1, scenarios: 20 }), b = generate({ seed: 2, scenarios: 20 });
    expect(a.events).not.toEqual(b.events);
    expect(a.labels).toEqual(b.labels);
  });

  it('injects 12 labelled faults, 2 per Phase 1 rule', () => {
    const { labels } = generate({ seed: 42, scenarios: 20 });
    expect(labels).toHaveLength(12);
    const perRule = Object.groupBy(labels, (l) => l.ruleId);
    for (const id of ['ACC-01', 'ACC-02', 'APA-01', 'APA-02', 'HMI-01', 'HMI-02']) expect(perRule[id]).toHaveLength(2);
    expect(new Set(labels.map((l) => l.scenarioId)).size).toBe(8);
  });

  it('emits valid, per-scenario ordered events ending with SCENARIO_END', () => {
    const { events } = generate({ seed: 42, scenarios: 20 });
    const byScenario = Object.groupBy(events, (e) => e.scenarioId);
    expect(Object.keys(byScenario)).toHaveLength(20);
    for (const list of Object.values(byScenario)) {
      const ts = list!.map((e) => e.ts);
      expect(ts).toEqual([...ts].sort((x, y) => x - y));
      const last = list!.at(-1)!;
      expect([last.module, last.type]).toEqual(['SYS', 'SCENARIO_END']);
      expect(last.ts).toBeLessThanOrEqual(90000);
    }
    for (const e of events.slice(0, 500)) expect(parseLine(JSON.stringify(e)).ok).toBe(true);
  });

  it('uses the documented sampling periods', () => {
    const { events } = generate({ seed: 42, scenarios: 20 });
    const tracks = events.filter((e) => e.scenarioId === 'S009' && e.type === 'TRACK').map((e) => e.ts);
    expect(tracks).toHaveLength(1200);
    expect(tracks[1]! - tracks[0]!).toBe(50);
    const obstacles = events.filter((e) => e.scenarioId === 'S010' && e.type === 'OBSTACLE').map((e) => e.ts);
    expect(obstacles[1]! - obstacles[0]!).toBe(100);
  });

  it('interleaves scenarios while keeping per-scenario order', () => {
    const seq = generate({ seed: 42, scenarios: 20 });
    const mixed = generate({ seed: 42, scenarios: 20, interleave: true });
    expect(mixed.events).toHaveLength(seq.events.length);
    expect(mixed.events.slice(0, 20).map((e) => e.scenarioId)).not.toEqual(seq.events.slice(0, 20).map((e) => e.scenarioId));
    const s1 = (es: typeof seq.events) => es.filter((e) => e.scenarioId === 'S001');
    expect(s1(mixed.events)).toEqual(s1(seq.events));
  });

  it(`rejects fewer than ${MIN_SCENARIOS} scenarios`, () => {
    expect(() => generate({ seed: 1, scenarios: 7 })).toThrow(RangeError);
    expect(() => generate({ seed: 1, scenarios: 8.5 })).toThrow(RangeError);
  });
});
