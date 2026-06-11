# 0002 - Frontend Foundation

Status: Draft
Related ADRs: 0008

## Goal

Create the Angular frontend skeleton with the stack defined in ADR 0008.
The app boots, proxies API and WebSocket to the backend, supports PT-BR/EN
i18n, and passes lint and build.

## Scope

Project structure, dependencies, routing skeleton, proxy, i18n setup,
PrimeNG + Tailwind integration. No business screens.

## Business Context

Single-page application consumed by three personas (customer, operator,
admin). Customer is the dominant traffic. The app must work without
authentication on the public screenings list and authenticate for
reservation flows.

## Business Rules

- Angular 22 with standalone components and signals.
- TypeScript strict mode enabled.
- SCSS for component-specific styles; Tailwind for layout utilities; PrimeNG
  for components. No mixing of Material with PrimeNG.
- Folder structure MUST follow `architecture/frontend-angular.md`:

```text
src/app/core            auth config http interceptors guards layout
                         observability realtime
src/app/shared          components directives pipes validators utils
src/app/features/<...>  data-access models pages ui
```

- No root-level `components`, `services`, `models` folders.
- No NgRx. Local component state for UI; feature services with signals for
  shared feature state.
- Proxy in dev: `/api` → `http://localhost:8080`, `/ws` →
  `http://localhost:8080` with WebSocket upgrade.
- i18n via **ngx-translate** with JSON files under `src/assets/i18n/`.
  Default language `pt-BR`, fallback `en`. Runtime switch via core service.
- Secrets MUST NOT live in frontend files.
- Routing: path-based (HTML5), no hash routing.

## API Contracts

Not applicable in this spec — proxy targets backend endpoints defined in
later specs.

## Persistence Changes

Not applicable.

## Validation Rules

- TypeScript `strict`, `noImplicitAny`, `strictNullChecks`, all enabled.
- ESLint with `@angular-eslint` recommended set plus rules forbidding
  `any`, `console.log` in production code, unused imports.
- Stylelint with SCSS configuration if SCSS is used in features.

## Error Behavior

Global error handling will be specified in spec 0021. Foundation provides:

- `core/http/api-error.interceptor.ts`: parses backend error JSON shape
  `{ code, message, fields }` into a typed `ApiError`.
- Toast service from PrimeNG wired in `app.config.ts`.

## Observability Requirements

- Inject a correlation ID header (`X-Correlation-Id`) on every outgoing HTTP
  request via an interceptor in `core/http/`. Generate a UUID per
  navigation or per session if missing.
- Console error reporting via a `core/observability` service. Production
  integration with Sentry/equivalent is out of scope for foundation.

## Tests Required

- `ng test` passes (default `app.component` test).
- `ng build --configuration production` passes with no errors.
- `ng lint` passes.

## Acceptance Criteria

- `npm install && npm start` serves the app on `http://localhost:4200`.
- `npm run build` produces a production bundle.
- `npm test` runs the default test suite green.
- Switching language updates labels using ngx-translate.
- A placeholder page renders with PrimeNG components and Tailwind layout to
  prove both work together.
- `proxy.conf.json` proxies `/api` and `/ws` correctly (verifiable with the
  backend running).

## Open Questions

- Browser support matrix: latest Chrome/Edge/Firefox/Safari only, or include
  one previous major? Default: latest two majors of evergreen browsers.

## Out of Scope

- Real business screens.
- API integration.
- WebSocket subscription.
- Reservation flow.
- Authentication UI.
- Admin console.
