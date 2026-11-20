import type { AnyRule } from '../engine/types.ts';
import { acc01 } from './acc-01.ts';
import { acc02 } from './acc-02.ts';
import { apa01 } from './apa-01.ts';
import { apa02 } from './apa-02.ts';
import { hmi01 } from './hmi-01.ts';
import { hmi02 } from './hmi-02.ts';

/** Phase 1 ruleset (RULESET_VERSION 1.0.0). Add a rule: new file + one line here. */
export const RULES: readonly AnyRule[] = [acc01, acc02, apa01, apa02, hmi01, hmi02];
