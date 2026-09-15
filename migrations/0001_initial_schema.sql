-- GUID-based shopping list schema.
-- Apply with Wrangler D1 migrations; this migration is safe to retry.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS lists (
    id_guid TEXT PRIMARY KEY NOT NULL,
    name TEXT,
    revision INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    list_id_guid TEXT NOT NULL,
    text TEXT NOT NULL,
    qty TEXT,
    checked INTEGER NOT NULL DEFAULT 0 CHECK (checked IN (0, 1)),
    sort_order INTEGER NOT NULL DEFAULT 0,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (list_id_guid) REFERENCES lists(id_guid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_items_list_sort
    ON items(list_id_guid, sort_order);

CREATE INDEX IF NOT EXISTS idx_lists_updated_at
    ON lists(updated_at);
