# GroceryBuddy

A simple, no-auth shopping list app.

## Features

- **Add items by typing a name.** Previously used item names are remembered
  and suggested again as autocomplete options, so you don't have to retype
  "Milk" every week.
- **Shops.** Add the shops you use and tag each
  item with the shop(s) it's available at.
- **Filter by shop.** When you're at a particular shop, filter your list down
  to just the items tagged for that shop.
- **Check items off** as you buy them, and clear purchased items when you're
  done.
- **Export / Import.** Export a human-readable JSON
  file. Use the Data tab to share it (e.g. via Bluetooth, email, messaging
  apps) so someone else can import it, or to back it up. Import can either
  merge with or replace your current list.

## Project structure

- [app/src/main/java/com/finnegan0596/shoppinglist/data](app/src/main/java/com/finnegan0596/shoppinglist/data) &mdash;
  data models and the `ShoppingListRepository` that reads/writes the local
  JSON file and handles export/import.
- [app/src/main/java/com/finnegan0596/shoppinglist/ui](app/src/main/java/com/finnegan0596/shoppinglist/ui) &mdash;
  the `ShoppingListViewModel` and Jetpack Compose screens (list, shops, data).

## Building locally

This project uses the standard Gradle wrapper, so you don't need Android
Studio to build it, though it's the easiest way to run/debug on a device or
emulator:

```
./gradlew assembleDebug
```

The resulting APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Debug builds talk to the **non-prod** API
(`shopping-list-api.…workers.dev`); release builds talk to **production**
(`grocerybuddy-prod.…workers.dev`). To point a debug build somewhere else
without editing tracked files, pass the `REMOTE_API_BASE_URL` override:

```
./gradlew assembleDebug -PREMOTE_API_BASE_URL=http://10.0.2.2:8787
```

See [docs/development-environment.md](docs/development-environment.md) for the
full environment map, local/preview/production loops, and seeding/resetting.

## Automated releases

Every push to `main` triggers [.github/workflows/release.yml](.github/workflows/release.yml),
which builds two APKs and publishes them as a new GitHub Release, ready to
download and sideload onto an Android device:

| Asset | API target |
| --- | --- |
| `GroceryBuddy-<version>.apk` | **production** (`grocerybuddy-prod.…workers.dev`) |
| `GroceryBuddy-<version>-preview-debug.apk` | **non-prod / preview** (`shopping-list-api.…workers.dev`) |

Pull requests and other branches are built (and unit-tested) via
[.github/workflows/ci.yml](.github/workflows/ci.yml) without creating a release.

> The preview APK is published alongside the production one as a convenience; a
> dedicated preview build workflow would be cleaner. See issue #43.

Both APKs are currently signed with the Android **debug** signing key. That key
is generated fresh on each CI runner, so every release is effectively signed
with a **different certificate** — which means a new release cannot be installed
over an already-installed copy (uninstall first). A dedicated, stable release
keystore is needed to fix this properly; tracked in issue #44.

## Cloudflare deployment and operations

The repo includes a minimal Cloudflare Worker configuration for the no-auth
shared-list backend, along with production deploy and D1 migration workflows
under [.github/workflows](.github/workflows). 

## D1 schema migrations

The GUID-based shared-list schema lives in `migrations/`. Install Wrangler
and configure a D1 database binding before applying migrations:

```sh
# Non-prod / preview D1 (shopping-list, 95f5bc0e-…)
npm run migrate:preview:remote

# Production D1 (grocerybuddy-prod, ed456d44-…)
npm run migrate:prod:remote
```

Use the `:remote` commands only after reviewing the migration and backing up
data — `migrate:prod:remote` targets production. `migrations/rollback/0001_initial_schema.sql`
is a manual, destructive rollback; apply it only after exporting the database
and stopping writers. Keep rollback files outside the migration root so
Wrangler does not apply them as forward migrations.
