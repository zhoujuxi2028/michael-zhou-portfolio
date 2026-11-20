import { anomaly, type Rule } from '../../src/engine/types.ts';

/**
 * Test-only rule: fires on every ACC/TAKEOVER_REQUEST whose reason is `<id>:<lag ms>`,
 * reporting startTs = ts − lag and detectTs = the scenario's current time.
 */
export function makeFakeRule(id: string): Rule<null, object> {
  const meta = { id, module: 'ACC', severity: 'P2' } as const;
  return {
    ...meta, title: 'test rule', defaults: {},
    createState: () => null,
    onEvent(e, ctx) {
      if (e.module !== 'ACC' || e.type !== 'TAKEOVER_REQUEST' || !e.data.reason.startsWith(`${id}:`)) return [];
      const lag = Number(e.data.reason.slice(id.length + 1));
      return [anomaly(meta, ctx, { startTs: e.ts - lag, endTs: e.ts, message: 'test' })];
    },
    onScenarioEnd: () => [],
    boundaryCases: () => [],
  };
}

export const fakeRule = makeFakeRule('T-1');
export const fakeRule2 = makeFakeRule('T-2');
