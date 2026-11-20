import type { EChartsOption } from 'echarts';
import type { Severity, Summary } from './summary.ts';

export interface Kpi {
  label: string;
  value: string;
  hint?: string;
}

const SEVERITY_COLOR: Record<Severity, string> = { P1: '#c0392b', P2: '#e08e0b' };
const LINE_COLOR = '#2a6fb0';

function title(text: string, isEmpty: boolean): EChartsOption['title'] {
  return { text, subtext: isEmpty ? 'No anomalies' : '', left: 'center' };
}

export function byRuleOption(s: Summary): EChartsOption {
  return {
    title: title('Anomalies by rule (P1 red, P2 amber)', s.byRule.length === 0),
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: s.byRule.map((r) => r.ruleId) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        data: s.byRule.map((r) => ({ value: r.count, itemStyle: { color: SEVERITY_COLOR[r.severity] } })),
      },
    ],
  };
}

export function byModuleOption(s: Summary): EChartsOption {
  return {
    title: title('Anomalies by module', s.byModule.length === 0),
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['35%', '65%'],
        data: s.byModule.map((m) => ({ name: m.module, value: m.count })),
      },
    ],
  };
}

export function timelineOption(s: Summary): EChartsOption {
  return {
    title: title('Anomalies over time (5 s buckets)', s.timeline.length === 0),
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: s.timeline.map((t) => `${t.bucketStartMs / 1000}s`) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'line',
        data: s.timeline.map((t) => t.count),
        areaStyle: {},
        itemStyle: { color: LINE_COLOR },
      },
    ],
  };
}

export function kpis(s: Summary): Kpi[] {
  const dq = s.dataQuality;
  const issues = dq.invalidJson + dq.schemaErrors + dq.unknownEvents + dq.outOfOrder;
  return [
    { label: 'Events', value: String(s.totals.events) },
    { label: 'Anomalies', value: String(s.totals.anomalies) },
    { label: 'P1', value: String(s.totals.p1) },
    { label: 'P2', value: String(s.totals.p2) },
    {
      label: 'Data quality issues',
      value: String(issues),
      hint: `invalid JSON ${dq.invalidJson}, schema ${dq.schemaErrors}, unknown ${dq.unknownEvents}, out of order ${dq.outOfOrder}`,
    },
    {
      label: 'Replay pass rate',
      value: s.replay ? `${(s.replay.passRate * 100).toFixed(1)}%` : 'n/a',
      hint: s.replay ? `${s.replay.passed}/${s.replay.total} passed` : 'no replay report',
    },
  ];
}
