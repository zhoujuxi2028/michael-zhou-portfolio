import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { byModuleOption, byRuleOption, kpis, timelineOption } from './charts.ts';
import { loadSummary } from './load.ts';
import type { Summary } from './summary.ts';

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer,
]);

function el(id: string): HTMLElement {
  const e = document.getElementById(id);
  if (!e) throw new Error(`missing element #${id}`);
  return e;
}

function showError(message: string): void {
  const box = el('error');
  box.textContent = `Failed to load dashboard data. ${message}`;
  box.hidden = false;
}

function render(s: Summary): void {
  el('meta').textContent =
    `tool ${s.meta.toolVersion} · ruleset ${s.meta.rulesetVersion} · analysis ${s.meta.analysisVersion}`;

  const cards = el('cards');
  for (const k of kpis(s)) {
    const card = document.createElement('div');
    card.className = 'card';
    for (const [cls, text] of [['label', k.label], ['value', k.value], ['hint', k.hint ?? '']] as const) {
      const d = document.createElement('div');
      d.className = cls;
      d.textContent = text;
      card.append(d);
    }
    cards.append(card);
  }
  el('content').hidden = false;

  const charts = [
    echarts.init(el('chart-rule')),
    echarts.init(el('chart-module')),
    echarts.init(el('chart-timeline')),
  ];
  charts[0]?.setOption(byRuleOption(s));
  charts[1]?.setOption(byModuleOption(s));
  charts[2]?.setOption(timelineOption(s));
  window.addEventListener('resize', () => charts.forEach((c) => c.resize()));
}

const result = await loadSummary((url) => fetch(url));
if (result.ok) render(result.value);
else showError(result.error);
