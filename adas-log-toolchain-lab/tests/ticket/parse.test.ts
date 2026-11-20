import { describe, expect, it } from 'vitest';
import { UsageError } from '../../src/config.ts';
import { parseDescription, parseExport } from '../../src/ticket/parse.ts';

describe('parseDescription', () => {
  it('reads Jira wiki headings with # lists', () => {
    const d = 'Intro text\nh3. Steps\n# Engage ACC\n# Cut in a vehicle\nh3. Expected\n# Takeover request is raised';
    expect(parseDescription(d)).toEqual({ steps: ['Engage ACC', 'Cut in a vehicle'], expected: ['Takeover request is raised'] });
  });
  it('reads bold headings and * / - / 1. list items', () => {
    const d = '*Steps*\n* one\n- two\n1. three\n*Expected*\n2) four';
    expect(parseDescription(d)).toEqual({ steps: ['one', 'two', 'three'], expected: ['four'] });
  });
  it('reads plain "Steps:" headings, case-insensitively, with inline text', () => {
    expect(parseDescription('steps:\n- a\nEXPECTED: it works\n- b')).toEqual({ steps: ['a'], expected: ['it works', 'b'] });
  });
  it('ignores other sections and returns empty arrays when missing', () => {
    expect(parseDescription('h2. Notes\n# x')).toEqual({ steps: [], expected: [] });
    expect(parseDescription(undefined)).toEqual({ steps: [], expected: [] });
    expect(parseDescription('Steps:\n# a\nh3. Notes\n# not a step')).toEqual({ steps: ['a'], expected: [] });
  });
});

describe('parseExport', () => {
  it('parses the field subset and defaults missing optional fields', () => {
    const r = parseExport(JSON.stringify({ issues: [{ key: 'ADAS-1', fields: { summary: 's' } }] }));
    expect(r).toEqual([{ key: 'ADAS-1', summary: 's', description: '', components: [], labels: [] }]);
  });
  it('flattens components to names', () => {
    const r = parseExport(JSON.stringify({ issues: [{ key: 'A-1', fields: { summary: 's', components: [{ name: 'ACC' }], labels: ['x'] } }] }));
    expect(r[0]).toMatchObject({ components: ['ACC'], labels: ['x'] });
  });
  it.each(['not json', '{}', '{"issues":[{"key":"A-1"}]}', '{"issues":[{"key":"a b","fields":{"summary":"s"}}]}'])('rejects %s with UsageError', (t) => {
    expect(() => parseExport(t)).toThrow(UsageError);
  });
  it('rejects duplicate keys', () => {
    const i = { key: 'A-1', fields: { summary: 's' } };
    expect(() => parseExport(JSON.stringify({ issues: [i, i] }))).toThrow(/duplicate/);
  });
});
