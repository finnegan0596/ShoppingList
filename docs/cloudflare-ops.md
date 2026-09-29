# Cloudflare operations checklist

## Default guardrails

- Rate limit: 60 requests per minute per client IP for the public API.
- Monthly spend threshold: $1 USD.
- Review owner: assign a single maintainer in the repo or issue tracker.
- Review cadence: first business day of each month.

### Spend guardrail with two environments

The $1 USD/month guardrail is re-confirmed with **2 Workers**
(`shopping-list-api` non-prod + `grocerybuddy-prod` production) and **2 D1
databases** (`shopping-list` + `grocerybuddy-prod`). Both fit inside the
Cloudflare Workers **Free** plan and the D1 free allowance at expected traffic;
no paid add-on is enabled.

## Monthly review steps

1. Open the Cloudflare dashboard for Workers, D1, and Pages usage.
2. Confirm that usage is under the monthly spend threshold.
3. Review logs for failed requests and rate-limit events.
4. Check migration history to confirm the `main` deployment is current.
5. Record the review outcome in the issue tracker or project board.

## Deployment notes

- Two environments exist (see `docs/development-environment.md` for the full map):
  - **Non-prod / preview**: Worker `shopping-list-api`, D1 `shopping-list`
    (`95f5bc0e-d541-4c5f-851b-ba867a397cfb`).
  - **Production**: Worker `grocerybuddy-prod`, D1 `grocerybuddy-prod`
    (`ed456d44-3804-4119-b101-7aa5c403c8f5`).
- Naming is asymmetric on purpose: `shopping-list*` = **non-prod**,
  `grocerybuddy-prod` = **production**. Don't mistake the legacy names for prod.
- Preview deploys are **enabled**: `npm run deploy:preview`
  (`wrangler deploy --env preview`) and `npm run migrate:preview:remote`.
- Production deploys run only on `main` after the migration workflow succeeds.
  They always require an explicit `--env production` (`npm run deploy:prod`); a
  bare `wrangler deploy` targets non-prod and can never reach production.
- On pull requests, the migration workflow validates against the **non-prod**
  D1 and fails closed if the resolved target is the production id
  (`ed456d44-…`).
- Nothing beyond the already-public non-prod D1 id is committed. `.wrangler/`,
  `node_modules/`, and `local.properties` are git-ignored.
- Human approval is still required for repository policy and Cloudflare secret
  access.

## Credentials

- Reuse the existing repository secrets `CLOUDFLARE_API_TOKEN` and
  `CLOUDFLARE_ACCOUNT_ID`; the same token covers non-prod and production, so no
  `CLOUDFLARE_PREVIEW_API_TOKEN` is needed. See
  `docs/development-environment.md` for the required token scopes.

## API smoke test

Run these against the **non-prod / preview** API
(`https://shopping-list-api.martintinycart.workers.dev`). Reviewers running it
by hand can swap `$api` for another environment, but production should only be
exercised deliberately.

```sh
api=https://shopping-list-api.martintinycart.workers.dev

list=$(curl -sS -X POST "$api/api/lists" \
  -H 'content-type: application/json' -d '{"name":"Groceries"}')
guid=$(printf '%s' "$list" | jq -r .guid)
revision=$(printf '%s' "$list" | jq -r .revision)

curl -sS "$api/api/lists/$guid"
item=$(curl -sS -X POST "$api/api/lists/$guid/items" \
  -H 'content-type: application/json' \
  -d "{\"text\":\"Milk\",\"revision\":$revision}")
item_id=$(printf '%s' "$item" | jq -r '.items[-1].id')
revision=$(printf '%s' "$item" | jq -r .revision)

curl -sS -X PATCH "$api/api/lists/$guid/items/$item_id" \
  -H 'content-type: application/json' \
  -d "{\"checked\":true,\"revision\":$revision}"
curl -sS -X DELETE "$api/api/lists/$guid/items/$item_id" \
  -H 'content-type: application/json' \
  -d "{\"revision\":$((revision + 1))}"
```

Clients should include the list revision returned by the previous response on
every item mutation. A stale supplied revision returns `409` and the current
revision, preventing silent overwrites.
