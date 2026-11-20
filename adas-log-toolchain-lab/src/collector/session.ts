import { join } from 'node:path';
import { createClipWriter, writeJsonFile, type ClipIndex } from './clip-writer.ts';
import { createQualityMonitor, type QualityReport } from './quality.ts';
import { createRecorder, type RecorderOptions } from './recorder.ts';
import type { Source, SourceResult } from './source.ts';

export interface SessionOptions extends RecorderOptions {
  /** Output directory for clips, `index.json` and `quality.json`. */
  out: string;
  /** `live` or the input path, recorded in the index meta. */
  source: string;
}

export interface SessionResult {
  index: ClipIndex;
  quality: QualityReport;
  events: number;
  dataQuality: SourceResult['dataQuality'];
}

/** Run a source through the recorder core and write clips, `index.json` and `quality.json`. */
export async function recordSession(source: Source, o: SessionOptions): Promise<SessionResult> {
  const quality = createQualityMonitor();
  let writes: Promise<unknown> = Promise.resolve();
  const recorder = createRecorder(o, (clip) => { writes = writes.then(() => writer.write(clip)); });
  const { bufferMs, preMs, postMs } = recorder.options;
  const writer = createClipWriter(o.out, { source: o.source, bufferMs, preMs, postMs });

  const result = await source((e, line) => {
    quality.push(e, line);
    recorder.push(e);
  });
  recorder.end();
  await writes;
  const index = await writer.finish();
  const report = quality.report();
  await writeJsonFile(join(o.out, 'quality.json'), report);
  return { index, quality: report, events: result.events, dataQuality: result.dataQuality };
}
