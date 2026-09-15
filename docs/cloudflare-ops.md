# Cloudflare operations checklist

## Default guardrails

- Rate limit: 60 requests per minute per client IP for the public API.
- Monthly spend threshold: $1 USD.
- Review owner: assign a single maintainer in the repo or issue tracker.
- Review cadence: first business day of each month.

## Monthly review steps

1. Open the Cloudflare dashboard for Workers, D1, and Pages usage.
2. Confirm that usage is under the monthly spend threshold.
3. Review logs for failed requests and rate-limit events.
4. Check migration history to confirm the `main` deployment is current.
5. Record the review outcome in the issue tracker or project board.

## Deployment notes

- Production deploys run only on `main` after the migration workflow succeeds.
- Preview deploys are intentionally disabled until a real preview environment and D1 database are configured.
- Human approval is still required for repository policy and Cloudflare secret access.

## API smoke test

After applying the D1 migration and starting the Worker with `npx wrangler dev`,
run the following commands (replace `http://localhost:8787` when needed):

```sh
list=$(curl -sS -X POST http://localhost:8787/api/lists \
  -H 'content-type: application/json' -d '{"name":"Groceries"}')
guid=$(printf '%s' "$list" | jq -r .guid)
revision=$(printf '%s' "$list" | jq -r .revision)

curl -sS "http://localhost:8787/api/lists/$guid"
item=$(curl -sS -X POST "http://localhost:8787/api/lists/$guid/items" \
  -H 'content-type: application/json' \
  -d "{\"text\":\"Milk\",\"revision\":$revision}")
item_id=$(printf '%s' "$item" | jq -r '.items[-1].id')
revision=$(printf '%s' "$item" | jq -r .revision)

curl -sS -X PATCH "http://localhost:8787/api/lists/$guid/items/$item_id" \
  -H 'content-type: application/json' \
  -d "{\"checked\":true,\"revision\":$revision}"
curl -sS -X DELETE "http://localhost:8787/api/lists/$guid/items/$item_id" \
  -H 'content-type: application/json' \
  -d "{\"revision\":$((revision + 1))}"
```

Clients should include the list revision returned by the previous response on
every item mutation. A stale supplied revision returns `409` and the current
revision, preventing silent overwrites.
