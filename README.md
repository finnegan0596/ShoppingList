# ShoppingList

A simple, offline-first Android shopping list app. There is no server and no
account required &mdash; everything is stored locally on your device.

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
- **Export / Import.** Your list is stored as a single, human-readable JSON
  file. Use the Data tab to share it (e.g. via Bluetooth, email, messaging
  apps) so someone else can import it, or to back it up. Import can either
  merge with or replace your current list.
- **No SQL database, no server.** Data lives in one plain-text JSON file in
  the app's private storage.

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

## Automated releases

Every push to `main` triggers [.github/workflows/release.yml](.github/workflows/release.yml),
which builds a debug APK and publishes it as a new GitHub Release with the
APK attached, ready to download and sideload onto an Android device. Pull
requests and other branches are built (and unit-tested) via
[.github/workflows/ci.yml](.github/workflows/ci.yml) without creating a release.

Since this is a personal/offline app, releases use a debug-signed APK rather
than a dedicated release signing key. If you later want a properly signed
release build, add a keystore and wire it up as GitHub Actions secrets, then
switch the workflow to run `assembleRelease`.

## D1 schema migrations

The GUID-based shared-list schema lives in `migrations/`. Install Wrangler
and configure a D1 database binding before applying migrations:

```sh
npx wrangler d1 migrations apply shopping-list --local
npx wrangler d1 migrations apply shopping-list --remote
```

The `--local` command applies migrations to Wrangler's local development
database. Use `--remote` only after reviewing the migration and backing up
production data. `migrations/rollback/0001_initial_schema.sql` is a manual,
destructive rollback; apply it only after exporting the database and stopping
writers. Keep rollback files outside the migration root so Wrangler does not
apply them as forward migrations.
