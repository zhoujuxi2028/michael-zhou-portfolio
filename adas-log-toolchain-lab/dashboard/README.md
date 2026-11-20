# ADAS Dashboard

Static TypeScript + ECharts dashboard for the ADAS log toolchain (FR-ADAS-010). It reads `summary.json` (contract C4, produced by `analysis/`) and renders KPI cards plus anomalies by rule, by module and over time. Synthetic data only.

```bash
npm ci
npm run dev        # http://localhost:5173, uses public/summary.json (sample)
npm run build      # tsc --noEmit && vite build -> dist/
npm test           # Vitest: validator, chart options, loader, workflow checks
npm run lint && npm run typecheck
```

- `src/summary.ts`: C4 types and runtime validator
- `src/charts.ts`: pure `summary -> ECharts option` functions and `kpis()`
- `src/load.ts` / `src/main.ts`: fetch + validate + render; an error panel is shown if `summary.json` is missing or invalid

The real `summary.json` is generated in CI by `.github/workflows/adas-pages.yml` (gen, parse, regression, replay, analysis, build) and deployed to GitHub Pages. The deploy job runs only when the repository variable `ADAS_PAGES_ENABLED` is `true`. To enable it (owner action, not done yet): Settings -> Pages -> Source "GitHub Actions", then set `ADAS_PAGES_ENABLED=true`.
