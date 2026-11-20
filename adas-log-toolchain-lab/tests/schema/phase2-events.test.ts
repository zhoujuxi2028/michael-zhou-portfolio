import { describe, expect, it } from 'vitest';
import { ev } from '../../src/schema/events.ts';
import { parseLine } from '../../src/schema/validate.ts';

const line = (o: unknown) => JSON.stringify(o);

describe('Phase 2 events', () => {
  it('round-trips LKA, VEH and SYS/MANUAL_TRIGGER events', () => {
    const events = [
      ev('LKA/STATE', 0, 'S', { from: 'OFF', to: 'ACTIVE' }),
      ev('LKA/LANE', 1, 'S', { leftConf: 0.9, rightConf: 0.4, offset: -0.2 }),
      ev('LKA/STEER', 2, 'S', { torque: -0.6 }),
      ev('VEH/TURN_SIGNAL', 3, 'S', { side: 'LEFT' }),
      ev('SYS/MANUAL_TRIGGER', 4, 'S', { note: 'driver flagged' }),
      ev('HMI/DISPLAY', 5, 'S', { feature: 'LKA', item: 'UNAVAILABLE', value: 'SHOWN' }),
    ];
    for (const e of events) expect(parseLine(line(e))).toEqual({ ok: true, event: e });
  });

  it('rejects lane confidence outside 0..1 and unknown LKA states', () => {
    const lane = parseLine(line({ ts: 0, scenarioId: 'S', module: 'LKA', type: 'LANE', data: { leftConf: 1.2, rightConf: 0.5, offset: 0 } }));
    expect(lane).toMatchObject({ ok: false, kind: 'schemaError' });
    if (!lane.ok) expect(lane.detail).toContain('data.leftConf');
    const state = parseLine(line({ ts: 0, scenarioId: 'S', module: 'LKA', type: 'STATE', data: { from: 'OFF', to: 'PAUSED' } }));
    expect(state).toMatchObject({ ok: false, kind: 'schemaError' });
  });

  it('rejects an unknown turn signal side', () => {
    expect(parseLine(line({ ts: 0, scenarioId: 'S', module: 'VEH', type: 'TURN_SIGNAL', data: { side: 'UP' } })))
      .toMatchObject({ ok: false, kind: 'schemaError' });
  });
});
