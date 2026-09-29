# Development environment

How to build, run, and point the app at each Cloudflare environment.

## Environments

| Role | Worker | Worker URL | D1 name | D1 id |
| --- | --- | --- | --- | --- |
| **Non-prod / preview** (debug target) | `shopping-list-api` | https://shopping-list-api.martintinycart.workers.dev | `shopping-list` | `95f5bc0e-d541-4c5f-851b-ba867a397cfb` |
| **Production** (release target) | `grocerybuddy-prod` | https://grocerybuddy-prod.martintinycart.workers.dev | `grocerybuddy-prod` | `ed456d44-3804-4119-b101-7aa5c403c8f5` |

The names are asymmetric **on purpose**: the legacy `shopping-list*` resources
are the shared **non-prod** environment, while `grocerybuddy-prod` is
**production**. Do not assume a `shopping-list*` resource is production.

Account id (non-secret): `c7bd7a727f37eb396ceb653a2bf14d54`.

## Prerequisites

- JDK 17 and the Android SDK (set `sdk.dir` in `local.properties`).
- Node 22+ and `npm install` at the repo root (installs Wrangler).
- A Cloudflare API token with **Workers Scripts:Edit** and **D1:Edit** for the
  account above. The same token covers non-prod and production — there is no
  separate preview token.
- No tokens, account ids, or D1 ids beyond the already-public non-prod id
  (`95f5bc0e-…`) are committed. `.wrangler/`, `node_modules/`, and
  `local.properties` stay out of version control.

## Build the debug app

Build and install the debug APK, which points at the **non-prod** API by
default:

```sh
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Preview loop

Deploy the Worker and apply migrations to the **non-prod** environment:

```sh
npm run deploy:preview          # wrangler deploy --env preview
npm run migrate:preview:remote  # migrations -> D1 shopping-list (95f5bc0e-…)
```

Run the API smoke test from `docs/cloudflare-ops.md` against
`https://shopping-list-api.martintinycart.workers.dev`.

## Production loop

> Production is **not** reachable from a bare `wrangler` command. It always
> requires an explicit `--env production`.

```sh
npm run deploy:prod             # wrangler deploy --env production
npm run migrate:prod:remote     # migrations -> D1 grocerybuddy-prod (ed456d44-…)
```

## Pointing the debug app at a different API

Debug builds read `REMOTE_API_BASE_URL` at build time, resolved in this order
(first non-blank wins):

1. Gradle property — `-PREMOTE_API_BASE_URL=…` or `REMOTE_API_BASE_URL=…` in
   `~/.gradle/gradle.properties`
2. `local.properties` entry `REMOTE_API_BASE_URL=…`
3. Environment variable `REMOTE_API_BASE_URL`

Example (no tracked file is edited):

```sh
./gradlew assembleDebug -PREMOTE_API_BASE_URL=http://10.0.2.2:8787
```

Release builds **ignore** the override entirely and always target production.

## Seeding / resetting data

- Preview / production: `npm run migrate:preview:remote` /
  `npm run migrate:prod:remote` apply pending migrations. Resetting a remote
  database is destructive — use `migrations/rollback/` (manual) only after
  exporting data.

## CI secrets

Both CI workflows reuse the existing repository secrets:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`

No `CLOUDFLARE_PREVIEW_API_TOKEN` is required. When the secrets are absent, the
Cloudflare workflows skip rather than fail.
