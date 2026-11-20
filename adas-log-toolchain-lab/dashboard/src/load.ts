import { parseSummary, type ParseResult } from './summary.ts';

export type Fetcher = (url: string) => Promise<Response>;

export async function loadSummary(fetcher: Fetcher, url = './summary.json'): Promise<ParseResult> {
  let res: Response;
  try {
    res = await fetcher(url);
  } catch (e) {
    return { ok: false, error: `Could not fetch ${url}: ${e instanceof Error ? e.message : String(e)}` };
  }
  if (!res.ok) return { ok: false, error: `Could not load ${url}: HTTP ${res.status}` };
  let data: unknown;
  try {
    data = await res.json();
  } catch {
    return { ok: false, error: `${url} is not valid JSON` };
  }
  const parsed = parseSummary(data);
  return parsed.ok ? parsed : { ok: false, error: `${url} does not match the expected format: ${parsed.error}` };
}
