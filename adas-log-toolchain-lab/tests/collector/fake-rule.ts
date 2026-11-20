import { anomaly, type Rule } from '../../src/engine/types.ts';

/**
 * Test-only rule "T-1": fires on every ACC/TAKEOVER_REQUEST whose reason is `lag:<ms>`,
 * reporting startTs = ts − lag and detectTs = the scenario's current time.
 */
export const fakeRule: Rule<null, object> = {
  id: 'T-1', module: 'ACC', severity: 'P2', title: 'test rule', defaults: {},
  createState: () => null,
  onEvent(e, ctx) {
    if (e.module !== 'ACC' || e.type !== 'TAKEOVER_REQUEST' || !e.data.reason.startsWith('lag:')) return [];
    const lag = Number(e.data.reason.slice(4));
    return [anomaly(this, ctx, { startTs: e.ts - lag, endTs: e.ts, message: 'test' })];
  },
  onScenarioEnd: () => [],
  boundaryCases: () => [],
};
