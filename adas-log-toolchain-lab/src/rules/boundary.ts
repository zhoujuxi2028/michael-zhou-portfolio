import type { AdasEvent } from '../schema/events.ts';
import type { BoundaryCase } from '../engine/types.ts';

const SIDES = [['minus', -1], ['eq', 0], ['plus', 1]] as const;

/** Three boundary cases (threshold −ε / exact / +ε); `triggerSide` is the only side expected to fire. */
export function triplet(
  prefix: string,
  triggerSide: 'minus' | 'plus',
  build: (sign: -1 | 0 | 1, caseId: string) => { description: string; events: AdasEvent[] },
): BoundaryCase[] {
  return SIDES.map(([tag, sign]) => {
    const caseId = `${prefix}-${tag}`;
    return { caseId, expectTriggered: tag === triggerSide, ...build(sign, caseId) };
  });
}
