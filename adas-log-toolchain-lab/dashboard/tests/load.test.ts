import { describe, expect, it } from 'vitest';
import sample from '../public/summary.json' with { type: 'json' };
import { loadSummary } from '../src/load.ts';

const resp = (body: string, status = 200) => async () => new Response(body, { status });

describe('loadSummary', () => {
  it('returns the validated summary', async () => {
    const r = await loadSummary(resp(JSON.stringify(sample)));
    expect(r.ok).toBe(true);
  });

  it('reports a missing file', async () => {
    const r = await loadSummary(resp('nope', 404));
    expect(r).toEqual({ ok: false, error: expect.stringContaining('404') });
  });

  it('reports a network failure', async () => {
    const r = await loadSummary(async () => {
      throw new TypeError('offline');
    });
    expect(r).toEqual({ ok: false, error: expect.stringContaining('offline') });
  });

  it('reports invalid JSON', async () => {
    const r = await loadSummary(resp('{not json'));
    expect(r).toEqual({ ok: false, error: expect.stringContaining('not valid JSON') });
  });

  it('reports a schema violation', async () => {
    const r = await loadSummary(resp('{}'));
    expect(r.ok).toBe(false);
  });
});
