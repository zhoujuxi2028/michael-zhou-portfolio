import { existsSync } from 'node:fs';
import { mkdir, readFile, rm, rmdir, writeFile } from 'node:fs/promises';
import { dirname, join, sep } from 'node:path';
import { z } from 'zod';
import { containedPath } from './paths.ts';

export type OutputTool = 'regression' | 'ticket';

/** Ownership file name per tool, so two tools can share one `--out` without deleting each other's files. */
export const ownershipFile = (tool: OutputTool) => `.adas-${tool}-outputs.json`;

const Ownership = z.object({ tool: z.string(), files: z.array(z.string()), dirs: z.array(z.string()) });

/** Writes files under `outDir` and records which ones (and which directories) this tool created. */
export interface OwnedOutputs {
  /** Create `rel` (and parents) under `outDir`; directories created here are owned. */
  mkdir(rel: string): Promise<void>;
  /** Write `rel` under `outDir` (overwriting a file of the same name) and own it. */
  write(rel: string, data: string | Uint8Array): Promise<void>;
  /** Write the ownership file listing everything owned by this run. */
  finish(): Promise<void>;
}

/** Relative paths in `list` that stay inside `outDir` (never `outDir` itself or the ownership file). */
function safeEntries(outDir: string, list: readonly string[], own: string): string[] {
  return list.flatMap((p) => {
    const rel = containedPath(outDir, p);
    return rel === null || rel === '' || rel === own ? [] : [rel];
  });
}

/**
 * Start a run of `tool` writing into `outDir`: delete exactly the files its ownership file lists
 * from the previous run, then the directories it created there if they are now empty. Nothing
 * else under `outDir` is touched; without a (valid) ownership file nothing is deleted.
 */
export async function startOwnedOutputs(outDir: string, tool: OutputTool): Promise<OwnedOutputs> {
  const own = ownershipFile(tool);
  let previous: z.infer<typeof Ownership> | null = null;
  try {
    const r = Ownership.safeParse(JSON.parse(await readFile(join(outDir, own), 'utf8')));
    if (r.success && r.data.tool === tool) previous = r.data;
  } catch {
    // missing or unreadable: nothing is ours yet
  }
  const keptDirs = new Set<string>();
  if (previous) {
    for (const f of safeEntries(outDir, previous.files, own)) await rm(join(outDir, f), { force: true });
    // Deepest first, so a nested owned dir is emptied before its parent.
    const dirs = safeEntries(outDir, previous.dirs, own).sort((a, b) => b.length - a.length);
    for (const d of dirs) {
      try {
        await rmdir(join(outDir, d));
      } catch {
        if (existsSync(join(outDir, d))) keptDirs.add(d); // not empty (user files): still ours
      }
    }
  }

  const files = new Set<string>();
  const dirs = new Set<string>();
  const inside = (rel: string) => {
    const r = containedPath(outDir, rel);
    if (r === null || r === '') throw new Error(`output path escapes ${outDir}: ${JSON.stringify(rel)}`);
    return r;
  };
  const makeDir = async (rel: string) => {
    const r = inside(rel);
    const parts = r.split(/[\\/]/);
    for (let i = 1; i <= parts.length; i++) {
      const d = join(...parts.slice(0, i));
      if (!existsSync(join(outDir, d))) {
        await mkdir(join(outDir, d), { recursive: true });
        dirs.add(d);
      } else if (keptDirs.has(d)) {
        dirs.add(d);
      }
    }
  };

  await mkdir(outDir, { recursive: true });
  return {
    mkdir: makeDir,
    async write(rel, data) {
      const r = inside(rel);
      if (dirname(r) !== '.') await makeDir(dirname(r));
      await writeFile(join(outDir, r), data);
      files.add(r);
    },
    async finish() {
      const posix = (set: Set<string>) => [...set].map((p) => p.split(sep).join('/')).sort();
      const record = { tool, files: posix(files), dirs: posix(dirs) };
      await writeFile(join(outDir, own), `${JSON.stringify(record, null, 2)}\n`);
    },
  };
}
