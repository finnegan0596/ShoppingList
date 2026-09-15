const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX_REQUESTS = 60;
const MAX_BODY_BYTES = 16 * 1024;
const recentRequests = new Map();

class RequestError extends Error {
  constructor(message, status = 400) {
    super(message);
    this.status = status;
  }
}

function checkRateLimit(ip) {
  const now = Date.now();
  const bucket = recentRequests.get(ip);

  if (!bucket || now >= bucket.resetAt) {
    recentRequests.set(ip, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return { allowed: true, retryAfter: 0 };
  }

  bucket.count += 1;
  if (bucket.count > RATE_LIMIT_MAX_REQUESTS) {
    return {
      allowed: false,
      retryAfter: Math.max(1, Math.ceil((bucket.resetAt - now) / 1000)),
    };
  }

  return { allowed: true, retryAfter: 0 };
}

function json(data, status = 200, headers = {}) {
  return Response.json(data, {
    status,
    headers: { 'cache-control': 'no-store', ...headers },
  });
}

function error(message, status, headers = {}) {
  return json({ error: message }, status, headers);
}

function isGuid(guid) {
  return /^[a-zA-Z0-9_-]{1,128}$/.test(guid);
}

function isInteger(value) {
  return Number.isInteger(value) && Number.isSafeInteger(value);
}

async function readJson(request) {
  const contentLength = Number(request.headers.get('content-length'));
  if (Number.isFinite(contentLength) && contentLength > MAX_BODY_BYTES) {
    throw new RequestError('Request body is too large');
  }

  const text = await request.text();
  if (text.length > MAX_BODY_BYTES) {
    throw new RequestError('Request body is too large');
  }

  try {
    return text ? JSON.parse(text) : {};
  } catch {
    throw new RequestError('Request body must be valid JSON');
  }
}

function validateItem(body, partial = false) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return 'Request body must be an object';
  }
  if (!partial && (typeof body.text !== 'string' || body.text.trim().length === 0)) {
    return 'text is required';
  }
  if (body.text !== undefined && (typeof body.text !== 'string' || body.text.trim().length === 0)) {
    return 'text must be a non-empty string';
  }
  if (body.qty !== undefined && body.qty !== null && typeof body.qty !== 'string') {
    return 'qty must be a string or null';
  }
  if (body.checked !== undefined && typeof body.checked !== 'boolean') {
    return 'checked must be a boolean';
  }
  if (body.sortOrder !== undefined && !isInteger(body.sortOrder)) {
    return 'sortOrder must be an integer';
  }
  return null;
}

function validateRevision(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return 'Request body must be an object';
  }
  if (body.revision !== undefined && !isInteger(body.revision)) {
    return 'revision must be an integer';
  }
  return null;
}

async function getList(env, guid) {
  return env.DB.prepare(
    `SELECT id_guid AS guid, name, revision, created_at AS createdAt, updated_at AS updatedAt
     FROM lists WHERE id_guid = ?`,
  ).bind(guid).first();
}

async function getItems(env, guid) {
  const result = await env.DB.prepare(
    `SELECT id, text, qty, checked, sort_order AS sortOrder, updated_at AS updatedAt
     FROM items WHERE list_id_guid = ? ORDER BY sort_order, id`,
  ).bind(guid).all();
  return result.results.map((item) => ({ ...item, checked: Boolean(item.checked) }));
}

async function getListResponse(env, guid) {
  const list = await getList(env, guid);
  if (!list) return null;
  return { ...list, items: await getItems(env, guid) };
}

async function createList(request, env) {
  const body = await readJson(request);
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return error('Request body must be an object', 400);
  }

  const guid = body.guid ?? crypto.randomUUID();
  if (typeof guid !== 'string' || !isGuid(guid)) {
    return error('guid must contain only letters, numbers, "_" or "-"', 400);
  }
  if (body.name !== undefined && body.name !== null && typeof body.name !== 'string') {
    return error('name must be a string or null', 400);
  }

  const now = new Date().toISOString();
  try {
    await env.DB.prepare(
      `INSERT INTO lists (id_guid, name, revision, created_at, updated_at)
       VALUES (?, ?, 0, ?, ?)`,
    ).bind(guid, body.name ?? null, now, now).run();
  } catch (cause) {
    if (String(cause).toLowerCase().includes('unique')) {
      return error('A list with this guid already exists', 409);
    }
    throw cause;
  }

  return json(await getListResponse(env, guid), 201);
}

async function checkRevision(env, guid, revision) {
  const list = await getList(env, guid);
  if (!list) return { response: error('List not found', 404) };
  if (revision !== undefined && revision !== list.revision) {
    return {
      response: json({ error: 'Revision conflict', revision: list.revision }, 409),
    };
  }
  return { list };
}

async function createItem(request, env, guid) {
  const body = await readJson(request);
  const validationError = validateItem(body) ?? validateRevision(body);
  if (validationError) return error(validationError, 400);

  const checked = body.checked === true ? 1 : 0;
  const now = new Date().toISOString();
  const revisionResult = await checkRevision(env, guid, body.revision);
  if (revisionResult.response) return revisionResult.response;
  const nextRevision = revisionResult.list.revision + 1;

  const result = await env.DB.batch([
    env.DB.prepare(
      `INSERT INTO items (list_id_guid, text, qty, checked, sort_order, updated_at)
       VALUES (?, ?, ?, ?, ?, ?)`,
    ).bind(guid, body.text.trim(), body.qty ?? null, checked, body.sortOrder ?? 0, now),
    env.DB.prepare(
      'UPDATE lists SET revision = ?, updated_at = ? WHERE id_guid = ? AND revision = ?',
    ).bind(nextRevision, now, guid, revisionResult.list.revision),
  ]);
  if (result[1].meta.changes !== 1) return error('Revision conflict', 409);

  return json(await getListResponse(env, guid), 201);
}

async function updateItem(request, env, guid, itemId) {
  const body = await readJson(request);
  const validationError = validateItem(body, true) ?? validateRevision(body);
  if (validationError) return error(validationError, 400);
  if (Object.keys(body).every((key) => !['text', 'qty', 'checked', 'sortOrder'].includes(key))) {
    return error('At least one item field is required', 400);
  }

  const revisionResult = await checkRevision(env, guid, body.revision);
  if (revisionResult.response) return revisionResult.response;
  const current = await env.DB.prepare(
    'SELECT id, text, qty, checked, sort_order AS sortOrder FROM items WHERE id = ? AND list_id_guid = ?',
  ).bind(itemId, guid).first();
  if (!current) return error('Item not found', 404);

  const now = new Date().toISOString();
  const nextRevision = revisionResult.list.revision + 1;
  const result = await env.DB.batch([
    env.DB.prepare(
      `UPDATE items SET text = ?, qty = ?, checked = ?, sort_order = ?, updated_at = ?
       WHERE id = ? AND list_id_guid = ?`,
    ).bind(
      body.text === undefined ? current.text : body.text.trim(),
      body.qty === undefined ? current.qty : body.qty,
      body.checked === undefined ? current.checked : (body.checked ? 1 : 0),
      body.sortOrder === undefined ? current.sortOrder : body.sortOrder,
      now, itemId, guid,
    ),
    env.DB.prepare(
      'UPDATE lists SET revision = ?, updated_at = ? WHERE id_guid = ? AND revision = ?',
    ).bind(nextRevision, now, guid, revisionResult.list.revision),
  ]);
  if (result[0].meta.changes !== 1) return error('Item not found', 404);
  if (result[1].meta.changes !== 1) return error('Revision conflict', 409);

  return json(await getListResponse(env, guid));
}

async function deleteItem(request, env, guid, itemId) {
  const body = await readJson(request);
  const validationError = validateRevision(body);
  if (validationError) return error(validationError, 400);

  const revisionResult = await checkRevision(env, guid, body.revision);
  if (revisionResult.response) return revisionResult.response;
  const now = new Date().toISOString();
  const nextRevision = revisionResult.list.revision + 1;
  const result = await env.DB.batch([
    env.DB.prepare('DELETE FROM items WHERE id = ? AND list_id_guid = ?').bind(itemId, guid),
    env.DB.prepare(
      'UPDATE lists SET revision = ?, updated_at = ? WHERE id_guid = ? AND revision = ?',
    ).bind(nextRevision, now, guid, revisionResult.list.revision),
  ]);
  if (result[0].meta.changes !== 1) return error('Item not found', 404);
  if (result[1].meta.changes !== 1) return error('Revision conflict', 409);

  return json(await getListResponse(env, guid));
}

async function route(request, env) {
  const url = new URL(request.url);
  const segments = url.pathname.split('/').filter(Boolean);
  if (url.pathname === '/' && request.method === 'GET') {
    return json({
      ok: true,
      app: env.APP_NAME ?? 'ShoppingList',
      message: 'API ready for list GUID endpoints.',
    });
  }
  if (segments[0] !== 'api') return error('Not found', 404);

  if (request.method === 'POST' && segments.length === 2 && segments[1] === 'lists') {
    return createList(request, env);
  }
  if (segments.length < 3 || segments[1] !== 'lists' || !isGuid(segments[2])) {
    return error('Not found', 404);
  }

  const guid = segments[2];
  if (request.method === 'GET' && segments.length === 3) {
    const list = await getListResponse(env, guid);
    return list ? json(list) : error('List not found', 404);
  }
  if (segments[3] !== 'items') return error('Not found', 404);
  if (request.method === 'POST' && segments.length === 4) return createItem(request, env, guid);
  if (segments.length !== 5 || !/^\d+$/.test(segments[4])) return error('Not found', 404);

  const itemId = Number(segments[4]);
  if (request.method === 'PATCH') return updateItem(request, env, guid, itemId);
  if (request.method === 'DELETE') return deleteItem(request, env, guid, itemId);
  return error('Method not allowed', 405, { Allow: 'GET, POST, PATCH, DELETE' });
}

export default {
  async fetch(request, env) {
    const ip = request.headers.get('CF-Connecting-IP') ?? 'anonymous';
    const rateLimit = checkRateLimit(ip);
    if (!rateLimit.allowed) {
      return error('Too many requests', 429, {
        'Retry-After': String(rateLimit.retryAfter),
      });
    }

    try {
      return await route(request, env);
    } catch (cause) {
      console.error(JSON.stringify({
        message: 'Request failed',
        error: cause instanceof Error ? cause.message : String(cause),
      }));
      return error(
        cause instanceof RequestError ? cause.message : 'Internal server error',
        cause instanceof RequestError ? cause.status : 500,
      );
    }
  },
};
