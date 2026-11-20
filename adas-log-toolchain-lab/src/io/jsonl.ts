import { createReadStream } from 'node:fs';
import { createInterface } from 'node:readline';

/** Stream a file line by line without loading it into memory. */
export function readLines(path: string): AsyncIterable<string> {
  return createInterface({ input: createReadStream(path, { encoding: 'utf8' }), crlfDelay: Infinity });
}

export function toJsonl(records: readonly unknown[]): string {
  return records.length === 0 ? '' : `${records.map((r) => JSON.stringify(r)).join('\n')}\n`;
}
