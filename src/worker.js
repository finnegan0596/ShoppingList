const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX_REQUESTS = 60;
const recentRequests = new Map();

function checkRateLimit(ip) {
  const now = Date.now();
  const bucket = recentRequests.get(ip);

  if (!bucket) {
    recentRequests.set(ip, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return { allowed: true, retryAfter: 0 };
  }

  if (now >= bucket.resetAt) {
    recentRequests.set(ip, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return { allowed: true, retryAfter: 0 };
  }

  const nextCount = bucket.count + 1;
  if (nextCount > RATE_LIMIT_MAX_REQUESTS) {
    return {
      allowed: false,
      retryAfter: Math.max(1, Math.ceil((bucket.resetAt - now) / 1000)),
    };
  }

  bucket.count = nextCount;
  return { allowed: true, retryAfter: 0 };
}

export default {
  async fetch(request, env) {
    const ip = request.headers.get('CF-Connecting-IP') ?? 'anonymous';
    const rateLimit = checkRateLimit(ip);

    if (!rateLimit.allowed) {
      return new Response(
        JSON.stringify({ error: 'Too many requests' }),
        {
          status: 429,
          headers: {
            'content-type': 'application/json',
            'Retry-After': String(rateLimit.retryAfter),
          },
        },
      );
    }

    return Response.json({
      ok: true,
      app: env.APP_NAME ?? 'ShoppingList',
      message: 'API ready for list GUID endpoints.',
    });
  },
};
