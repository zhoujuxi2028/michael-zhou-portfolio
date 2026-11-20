import { isAbsolute, relative, resolve, sep } from 'node:path';

/**
 * Path of `path` relative to `base`, or `null` if it resolves outside `base`. Only a real `..`
 * segment escapes: names that merely start with dots (`..foo/x.jsonl`) stay inside.
 */
export function containedPath(base: string, path: string): string | null {
  const rel = relative(resolve(base), resolve(base, path));
  if (rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) return null;
  return rel;
}
