import { z } from 'zod';

export const AccState = z.enum(['OFF', 'STANDBY', 'ACTIVE', 'OVERRIDE']);
export type AccState = z.infer<typeof AccState>;

export const ApaStep = z.enum(['SEARCHING', 'SLOT_SELECTED', 'MANEUVERING', 'COMPLETED', 'ABORTED']);
export type ApaStep = z.infer<typeof ApaStep>;

export const HmiFeature = z.enum(['ACC', 'APA', 'LKA']);
export type HmiFeature = z.infer<typeof HmiFeature>;

/** Common envelope shared by every event line. */
export const Envelope = z.object({
  ts: z.number().int().nonnegative(),
  scenarioId: z.string().min(1),
  module: z.string().min(1),
  type: z.string().min(1),
  data: z.unknown(),
});

/** Payload schema per `${module}/${type}` (Phase 1). */
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
