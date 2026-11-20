// tests/rules/index.test.ts
import { describe, expect, it } from 'vitest';
import { RULES } from '../../src/rules/index.ts';

describe('RULES registry', () => {
  it('registers the six Phase 1 rules with unique ids', () => {
    expect(RULES.map((r) => r.id)).toEqual(['ACC-01', 'ACC-02', 'APA-01', 'APA-02', 'HMI-01', 'HMI-02']);
  });
});
