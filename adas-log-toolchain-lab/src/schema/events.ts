import { z } from 'zod';

export const AccState = z.enum(['OFF', 'STANDBY', 'ACTIVE', 'OVERRIDE']);
export type AccState = z.infer<typeof AccState>;

export const ApaStep = z.enum(['SEARCHING', 'SLOT_SELECTED', 'MANEUVERING', 'COMPLETED', 'ABORTED']);
export type ApaStep = z.infer<typeof ApaStep>;

export const LkaState = z.enum(['OFF', 'STANDBY', 'ACTIVE', 'SUPPRESSED', 'UNAVAILABLE']);
export type LkaState = z.infer<typeof LkaState>;

export const HmiFeature = z.enum(['ACC', 'APA', 'LKA']);
export type HmiFeature = z.infer<typeof HmiFeature>;

/**
 * Scenario ids end up in file names (regression case data), so only a safe subset is allowed:
 * 1-64 chars of `[A-Za-z0-9_.-]`, and never `.` or `..`.
 */
export const ScenarioId = z.string()
  .regex(/^[A-Za-z0-9_.-]{1,64}$/, 'must be 1-64 characters of [A-Za-z0-9_.-]')
  .refine((s) => s !== '.' && s !== '..', 'must not be "." or ".."');

/** Common envelope shared by every event line. */
export const Envelope = z.object({
  ts: z.number().int().nonnegative(),
  scenarioId: ScenarioId,
  module: z.string().min(1),
  type: z.string().min(1),
  data: z.unknown(),
});

/** Payload schema per `${module}/${type}` (Phase 1 + Phase 2 LKA / VEH / manual trigger). */
export const dataSchemas = {
  'ACC/STATE': z.object({ from: AccState, to: AccState, reason: z.string() }),
  'ACC/TRACK': z.object({
    egoSpeed: z.number().nonnegative(),
    setSpeed: z.number().nonnegative(),
    leadDistance: z.number().nonnegative().nullable(),
    leadRelSpeed: z.number(),
  }),
  'ACC/BRAKE_PEDAL': z.object({ pressed: z.boolean() }),
  'ACC/TAKEOVER_REQUEST': z.object({ reason: z.string() }),
  'APA/STEP': z.object({ step: ApaStep, reason: z.string() }),
  'APA/OBSTACLE': z.object({ minDistance: z.number().nonnegative() }),
  'HMI/DISPLAY': z.object({
    feature: HmiFeature,
    item: z.enum(['STATE', 'TAKEOVER', 'UNAVAILABLE']),
    value: z.string(),
  }),
  'SYS/SCENARIO_END': z.object({}),
  'SYS/MANUAL_TRIGGER': z.object({ note: z.string() }),
  'LKA/STATE': z.object({ from: LkaState, to: LkaState }),
  'LKA/LANE': z.object({
    leftConf: z.number().min(0).max(1),
    rightConf: z.number().min(0).max(1),
    offset: z.number(),
  }),
  'LKA/STEER': z.object({ torque: z.number() }),
  'VEH/TURN_SIGNAL': z.object({ side: z.enum(['LEFT', 'RIGHT', 'OFF']) }),
} as const;

export type EventKey = keyof typeof dataSchemas;
export type DataOf<K extends EventKey> = z.infer<(typeof dataSchemas)[K]>;
type ModuleOf<K> = K extends `${infer M}/${string}` ? M : never;
type TypeOf<K> = K extends `${string}/${infer T}` ? T : never;

/** Discriminated union: narrow with `e.module === 'ACC' && e.type === 'TRACK'`. */
export type AdasEvent = {
  [K in EventKey]: { ts: number; scenarioId: string; module: ModuleOf<K>; type: TypeOf<K>; data: DataOf<K> };
}[EventKey];

export type Module = AdasEvent['module'];

/** Build a typed event; key order matches the JSON Lines output. */
export function ev<K extends EventKey>(key: K, ts: number, scenarioId: string, data: DataOf<K>): AdasEvent {
  const [module, type] = key.split('/');
  return { ts, scenarioId, module, type, data } as unknown as AdasEvent;
}

export function scenarioEnd(scenarioId: string, ts: number): AdasEvent {
  return ev('SYS/SCENARIO_END', ts, scenarioId, {});
}
