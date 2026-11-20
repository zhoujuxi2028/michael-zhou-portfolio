import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { RULESET_VERSION, TOOL_VERSION } from '../src/version.ts';

describe('version', () => {
  it('TOOL_VERSION matches package.json', () => {
    const pkg = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8')) as { version: string };
    expect(TOOL_VERSION).toBe(pkg.version);
  });

  it('RULESET_VERSION is semver', () => {
    expect(RULESET_VERSION).toMatch(/^\d+\.\d+\.\d+$/);
  });

  it('RULESET_VERSION is 1.1.0 after adding the LKA rules (minor bump)', () => {
    expect(RULESET_VERSION).toBe('1.1.0');
  });
});
