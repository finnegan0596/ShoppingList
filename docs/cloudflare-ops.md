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
