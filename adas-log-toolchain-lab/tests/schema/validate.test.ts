import { describe, expect, it } from 'vitest';
import { ev, scenarioEnd } from '../../src/schema/events.ts';
import { parseLine } from '../../src/schema/validate.ts';

const line = (o: unknown) => JSON.stringify(o);

describe('parseLine', () => {
  it('accepts a valid ACC TRACK event', () => {
    const e = ev('ACC/TRACK', 100, 'S001', { egoSpeed: 20, setSpeed: 22, leadDistance: null, leadRelSpeed: 0 });
    const r = parseLine(line(e));
    expect(r).toEqual({ ok: true, event: e });
  });

  it('round-trips every event built with ev()', () => {
    const events = [
      ev('ACC/STATE', 0, 'S', { from: 'OFF', to: 'ACTIVE', reason: 'x' }),
      ev('ACC/BRAKE_PEDAL', 1, 'S', { pressed: true }),
      ev('ACC/TAKEOVER_REQUEST', 2, 'S', { reason: 'x' }),
      ev('APA/STEP', 3, 'S', { step: 'MANEUVERING', reason: 'x' }),
      ev('APA/OBSTACLE', 4, 'S', { minDistance: 0.5 }),
      ev('HMI/DISPLAY', 5, 'S', { feature: 'APA', item: 'STATE', value: 'MANEUVERING' }),
      scenarioEnd('S', 6),
    ];
    for (const e of events) expect(parseLine(line(e))).toEqual({ ok: true, event: e });
  });

  it('classifies invalid JSON', () => {
    expect(parseLine('{not json')).toEqual({ ok: false, kind: 'invalidJson', detail: 'invalid JSON' });
  });

  it('classifies envelope errors with field path', () => {
    const r = parseLine(line({ ts: -1, scenarioId: 'S', module: 'ACC', type: 'TRACK', data: {} }));
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.kind).toBe('schemaError');
      expect(r.detail).toContain('ts');
    }
  });

  it('classifies data errors with data.* path', () => {
    const r = parseLine(line({ ts: 1, scenarioId: 'S', module: 'ACC', type: 'TRACK', data: { egoSpeed: 'fast', setSpeed: 1, leadDistance: null, leadRelSpeed: 0 } }));
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.kind).toBe('schemaError');
      expect(r.detail).toContain('data.egoSpeed');
    }
  });

  it('classifies unknown module/type', () => {
    expect(parseLine(line({ ts: 1, scenarioId: 'S', module: 'RADAR', type: 'POINTS', data: {} }))).toEqual({
      ok: false, kind: 'unknownEvent', detail: 'RADAR/POINTS',
    });
  });
});

describe('scenarioId', () => {
  const withId = (scenarioId: unknown) => line({ ts: 1, scenarioId, module: 'SYS', type: 'SCENARIO_END', data: {} });

  it.each(['S001', 'a', 'A-b_c.9', 'x'.repeat(64)])('accepts the safe id %j', (id) => {
    expect(parseLine(withId(id)).ok).toBe(true);
  });

  it.each(['x/../../../../escaped', 'a/b', 'a\\b', '.', '..', '', 'x'.repeat(65), 'a b', 'Sé'])('rejects %j as a schemaError', (id) => {
    const r = parseLine(withId(id));
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.kind).toBe('schemaError');
      expect(r.detail).toContain('scenarioId');
    }
  });
});
