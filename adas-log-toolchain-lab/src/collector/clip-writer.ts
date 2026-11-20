import { createHash } from 'node:crypto';
import { mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { writeJson } from '../cli/context.ts';
import { toJsonl } from '../io/jsonl.ts';
import { RULESET_VERSION, TOOL_VERSION } from '../version.ts';
import type { Clip } from './recorder.ts';

export interface IndexMeta { source: string; bufferMs: number; preMs: number; postMs: number }

export interface ClipEntry {
  file: string;
  scenarioId: string;
  trigger: Clip['trigger'];
  ruleIds: string[];
  startTs: number;
  endTs: number;
  /** Lines in the file, preconditions included. */
  events: number;
  /** SHA-256 (hex) of the file bytes. */
  sha256: string;
  truncated: boolean;
}

export interface ClipIndex {
  meta: { toolVersion: string; rulesetVersion: string } & IndexMeta;
  clips: ClipEntry[];
}

export interface ClipWriter {
  write(clip: Clip): Promise<ClipEntry>;
  /** Write `index.json` and return it. */
  finish(): Promise<ClipIndex>;
}

/** Writes clip JSON Lines files into `dir` and collects `index.json` (contract C3). */
export function createClipWriter(dir: string, meta: IndexMeta): ClipWriter {
  const clips: ClipEntry[] = [];
  const used = new Set<string>();

  const uniqueName = (file: string) => {
    let name = file;
    for (let n = 2; used.has(name); n++) name = file.replace(/\.jsonl$/, `_${n}.jsonl`);
    used.add(name);
    return name;
  };

  return {
    async write(clip) {
      await mkdir(dir, { recursive: true });
      const file = uniqueName(clip.file);
      const bytes = Buffer.from(toJsonl(clip.events), 'utf8');
      await writeFile(join(dir, file), bytes);
      const entry: ClipEntry = {
        file, scenarioId: clip.scenarioId, trigger: clip.trigger, ruleIds: clip.ruleIds,
        startTs: clip.startTs, endTs: clip.endTs, events: clip.events.length,
        sha256: createHash('sha256').update(bytes).digest('hex'), truncated: clip.truncated,
      };
      clips.push(entry);
      return entry;
    },
    async finish() {
      const index: ClipIndex = { meta: { toolVersion: TOOL_VERSION, rulesetVersion: RULESET_VERSION, ...meta }, clips };
      await writeJson(join(dir, 'index.json'), index);
      return index;
    },
  };
}
