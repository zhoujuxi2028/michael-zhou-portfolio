import type { AnyRule } from '../engine/types.ts';
import { RULES } from '../rules/index.ts';
import { parseDescription, type Ticket } from './parse.ts';

export interface Task { key: string; summary: string; module: string; ruleId: string; steps: string[]; expected: string[] }
export type MapResult = { ok: true; task: Task } | { ok: false; key: string; reason: string };

export const UNMAPPED_REASONS = {
  noLabel: 'no rule label',
  unknownRule: 'unknown rule id',
  mismatch: 'component does not match rule module',
} as const;

/** Map a ticket to a task via its `rule:<ID>` label and component; otherwise explain why not. */
export function mapTicket(ticket: Ticket, rules: readonly AnyRule[] = RULES): MapResult {
  const fail = (reason: string): MapResult => ({ ok: false, key: ticket.key, reason });
  const label = ticket.labels.map((l) => /^rule:(.+)$/i.exec(l.trim())?.[1]).find((x) => x !== undefined);
  if (label === undefined) return fail(UNMAPPED_REASONS.noLabel);
  const rule = rules.find((r) => r.id.toLowerCase() === label.trim().toLowerCase());
  if (!rule) return fail(UNMAPPED_REASONS.unknownRule);
  if (!ticket.components.some((c) => c.trim().toUpperCase() === rule.module)) return fail(UNMAPPED_REASONS.mismatch);
  return { ok: true, task: { key: ticket.key, summary: ticket.summary, module: rule.module, ruleId: rule.id, ...parseDescription(ticket.description) } };
}
