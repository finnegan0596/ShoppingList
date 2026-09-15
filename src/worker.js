export default {
  async fetch(request, env) {
    const ip = request.headers.get('CF-Connecting-IP') ?? 'anonymous';

    if (env.LIST_RATE_LIMITER) {
      const decision = await env.LIST_RATE_LIMITER.limit({
        key: ip,
        cost: 1,
      });

      if (!decision.success) {
        return new Response(JSON.stringify({ error: 'Too many requests' }), {
          status: 429,
          headers: {
            'content-type': 'application/json',
            'Retry-After': String(decision.retryAfter ?? 60),
          },
        });
      }
    }

    return Response.json({
      ok: true,
      app: env.APP_NAME ?? 'ShoppingList',
      message: 'API ready for list GUID endpoints.',
    });
  },
};
