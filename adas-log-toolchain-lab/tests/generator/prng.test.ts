import { describe, expect, it } from 'vitest';
import { createRng, mulberry32 } from '../../src/generator/prng.ts';

describe('mulberry32', () => {
  it('is deterministic for the same seed', () => {
    const a = mulberry32(42), b = mulberry32(42);
    expect(Array.from({ length: 5 }, a)).toEqual(Array.from({ length: 5 }, b));
  });

  it('differs across seeds and stays in [0, 1)', () => {
    const a = Array.from({ length: 1000 }, mulberry32(1));
    const b = Array.from({ length: 1000 }, mulberry32(2));
    expect(a).not.toEqual(b);
    for (const x of a) expect(x >= 0 && x < 1).toBe(true);
  });
});

describe('createRng', () => {
  it('int() is inclusive and within bounds', () => {
    const rng = createRng(7);
    const seen = new Set<number>();
    for (let i = 0; i < 2000; i++) {
      const v = rng.int(50, 52);
      expect(v >= 50 && v <= 52 && Number.isInteger(v)).toBe(true);
      seen.add(v);
    }
    expect([...seen].sort()).toEqual([50, 51, 52]);
  });

  it('range() stays within [min, max)', () => {
    const rng = createRng(9);
    for (let i = 0; i < 1000; i++) {
      const v = rng.range(-0.5, 0.5);
      expect(v >= -0.5 && v < 0.5).toBe(true);
    }
  });
});
