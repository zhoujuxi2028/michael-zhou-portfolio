import { describe, expect, it } from 'vitest';
import { mapTicket } from '../../src/ticket/map.ts';
import type { Ticket } from '../../src/ticket/parse.ts';

const t = (o: Partial<Ticket>): Ticket => ({ key: 'ADAS-1', summary: 's', description: 'Steps:\n# a\nExpected:\n# b', components: ['ACC'], labels: ['rule:ACC-01'], ...o });

describe('mapTicket', () => {
  it('maps component + rule label and extracts sections', () => {
    expect(mapTicket(t({}))).toEqual({ ok: true, task: { key: 'ADAS-1', summary: 's', module: 'ACC', ruleId: 'ACC-01', steps: ['a'], expected: ['b'] } });
  });
  it('is case-insensitive for component and rule label', () => {
    expect(mapTicket(t({ components: ['apa'], labels: ['Rule:apa-02'] }))).toMatchObject({ ok: true, task: { module: 'APA', ruleId: 'APA-02' } });
  });
  it('accepts a matching component among several', () => {
    expect(mapTicket(t({ components: ['Backend', 'ACC'] }))).toMatchObject({ ok: true });
  });
  const cases: [Partial<Ticket>, string][] = [
    [{ labels: ['bug'] }, 'no rule label'],
    [{ labels: ['rule:XYZ-99'] }, 'unknown rule id'],
    [{ components: ['HMI'] }, 'component does not match rule module'],
    [{ components: [] }, 'component does not match rule module'],
  ];
  it.each(cases)('unmapped %j -> %s', (o, reason) => {
    expect(mapTicket(t(o))).toEqual({ ok: false, key: 'ADAS-1', reason });
  });
});
