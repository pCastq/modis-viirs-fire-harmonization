# Dashboard — Fire Calendar

React + TypeScript + Vite dashboard for the `firecal` backend (harmonized MODIS/VIIRS hotspots). It answers the challenge's questions:
_which periods of the year burn the most, which years were worse, how does the present compare with the past_, for an area chosen by
the user, with CSV downloads. In the Docker image it is built and served by the backend on port 8080 (see the main README).

## Run it for development

```bash
docker compose up -d db && ./mvnw spring-boot:run   # backend on :8080 (from the repository root)
cd frontend && npm install && npm run dev           # dashboard on :5173, /api is proxied to the backend
```

Another backend: `VITE_API_PROXY=http://host:port npm run dev`.

| Command | What it does |
|---|---|
| `npm run dev` | dev server with hot reload |
| `npm run build` | type check + production build |
| `npm test` | tests (Vitest + Testing Library) |
| `npm run lint` | ESLint (types, React hooks) |
| `npm run format:check` | Prettier |

## What the dashboard does

- **Where**: a country, an area **drawn** on the map (rectangle or polygon), or typed **coordinates**.
- **When / what to count**: period, harmonized or raw, vegetation or all sources, day/night, confidence.
- **Calendar**: years × months with an accessible colour ramp (ColorBrewer YlOrRd) and a "Versus typical" view (diverging PuOr). Colour is never the
  only signal: every cell shows its number, incomplete months have a hatch and a "!", months with no data are hatched (never a zero).
- **Indicators**: peak month, worst year, trend (Mann–Kendall / Sen), anomaly of the month against the other years, seasonal profile, annual totals with uncertainty.
- **Harmonization panel**: the multipliers k used for the chosen country (per quarter and day/night), calibration years and uncertainty bands.
- **Detail**: click a month for its days per satellite, click a day for the hotspots on the map. **Export**: CSV of monthly counts and of hotspots.
- **Shareable**: the whole state (area, period, filters, month, day) lives in the URL.

## Languages

English by default, Italian from the selector (`?lang=it` in the URL, then the saved choice, then English). `src/i18n/messages/en.ts` is the reference catalogue;
`it.ts` has the same type, so a missing key does not compile. The server's notes and errors follow the same language (`Accept-Language`).
To add a language: an entry in `locales.ts`, a `messages/<code>.ts` and its entry in `translate.ts`.

## Structure

```
src/
  api/          backend contract: types, HTTP + errors, one method per endpoint
  domain/       pure logic, no React or DOM: colour scales, calendar model, geometry, drawing state machine, periods, formats, anomaly, trend
  state/        app state = the URL
  hooks/        data fetching with TanStack Query
  components/   UI primitives with no domain knowledge
  features/     one folder per area: search, calendar, insights, detail, export, status
  test/         fixtures and a fake server for the integration tests
```

Rules the code follows: dependencies go one way (`features → hooks/state/components → domain/api`); the URL is the single source of truth for state; server data
stays in the TanStack Query cache; no chart library (the charts are hand-written SVG/HTML on tested scales); accessibility (the calendar is a `role="grid"`
with keyboard navigation, text/cell contrast ≥ 4.5:1 checked by a test, light/dark theme, reduced motion); transparency about the data (method, incomplete months and
resolution are always visible).

## Good to know

- Maps use the public OpenStreetMap tiles (attribution included) and need a network connection.
- The "Versus typical" view computes the per-month median on the calendar already downloaded (display only); the formal comparison is the backend's `/anomaly`.
- Tests do not load Leaflet (it needs a real layout): maps are replaced by placeholders there and checked in the browser.
