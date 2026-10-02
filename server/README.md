# Plaid sandbox proxy

A small, hardened Express server that brokers Plaid API calls so that the `client_id` and `secret` never live on a user's device.

> **Heads up — currently unused by the Android app.**
> The Stash app today talks to Plaid **directly** (bring-your-own client_id/secret architecture). This proxy is included as ready-to-use scaffolding for the day you decide to centralize Plaid credentials server-side. Until you wire the app at it, none of these endpoints are called.

## What it does

Forwards a fixed set of Plaid endpoints (see [Endpoints](#endpoints) below). The server:

- Strips any client-supplied `client_id` / `secret` from request bodies and injects the **server-side** values from env vars before forwarding to Plaid
- Requires an `x-api-key` header on every request (except `/health`) — shared secret between the proxy and any client
- Rate-limits to **60 requests / minute / IP**
- Sets security headers via [helmet](https://www.npmjs.com/package/helmet)
- Enforces CORS — defaults to `*`, override via `CORS_ORIGIN`
- Accepts request bodies up to **1 MB**

## Environment variables

| Variable           | Required | Default      | Notes                                                              |
| ------------------ | -------- | ------------ | ------------------------------------------------------------------ |
| `PLAID_CLIENT_ID`  | yes      | —            | From <https://dashboard.plaid.com/developers/keys>                 |
| `PLAID_SECRET`     | yes      | —            | The secret for the env you're targeting (sandbox / dev / prod)     |
| `PROXY_API_KEY`    | yes      | —            | Shared secret. Clients send this in the `x-api-key` header.        |
| `PLAID_ENV`        | no       | `sandbox`    | One of `sandbox`, `development`, `production`                      |
| `PORT`             | no       | `8787`       | HTTP listen port                                                   |
| `CORS_ORIGIN`      | no       | `*`          | Restrict to your client origin in production (e.g. `https://app.example.com`) |

The server **refuses to start** if `PLAID_CLIENT_ID`, `PLAID_SECRET`, or `PROXY_API_KEY` is missing.

## Run locally

```bash
cd server
npm install
PLAID_CLIENT_ID=xxx \
PLAID_SECRET=yyy \
PROXY_API_KEY=$(openssl rand -hex 32) \
PLAID_ENV=sandbox \
npm start
```

Health check:

```bash
curl http://localhost:8787/health
# {"ok":true}
```

Auth check (replace the key):

```bash
curl -X POST http://localhost:8787/link/token/create \
  -H "Content-Type: application/json" \
  -H "x-api-key: <your-PROXY_API_KEY>" \
  -d '{"client_name":"Stash","user":{"client_user_id":"local-user"}}'
```

A request without the header returns `401 Unauthorized`.

## Endpoints

All endpoints accept `POST` with a JSON body, except `/health` which is `GET`.

| Method | Path                            | Auth          | Purpose                                                  |
| ------ | ------------------------------- | ------------- | -------------------------------------------------------- |
| POST   | `/link/token/create`            | `x-api-key`   | Create a Plaid Link initialization token                 |
| POST   | `/item/public_token/exchange`   | `x-api-key`   | Exchange a Link `public_token` for a persistent `access_token` |
| POST   | `/liabilities/get`              | `x-api-key`   | Fetch credit card / loan liabilities for a linked item   |
| POST   | `/institutions/get_by_id`       | `x-api-key`   | Look up bank metadata (name, logo, etc.)                 |
| POST   | `/transactions/sync`            | `x-api-key`   | Incremental transaction sync via cursor                  |
| GET    | `/health`                       | none          | Liveness probe — returns `{ "ok": true }`                |

Any `client_id` / `secret` fields in the request body are **silently stripped** before forwarding to Plaid; the server-side values are used instead.

## Wiring the Android app to this proxy

The current `PlaidModule.kt` points `Retrofit.baseUrl()` at `sandbox.plaid.com` / `production.plaid.com` directly. To switch the app over to this proxy:

1. Add a `proxyBaseUrl` + `apiKey` to `PlaidCredentialsStore` (replacing or alongside the current `client_id` / `secret` fields)
2. Update `PlaidModule.baseUrl()` to read `proxyBaseUrl` from the store instead of the Plaid env → URL map
3. Add an OkHttp interceptor that injects `x-api-key: <apiKey>` on every request
4. Drop `client_id` / `secret` from the request DTOs (the server overwrites them anyway) or leave them blank
5. Update the in-app Plaid setup screen to capture the proxy URL + API key instead of the Plaid credentials

See the main README for the trade-offs of doing this vs. staying direct-to-Plaid.

## Deployment notes

Any Node-hosting service works (Fly.io, Render, Railway, Cloud Run, fly free tier is plenty for low traffic). Two minimums for production:

- Always set `CORS_ORIGIN` to your real client origin — `*` is fine for local dev but never for prod
- Always use a random, high-entropy `PROXY_API_KEY` (32+ bytes from `openssl rand`)
- Rotate the key by setting a new value on the server and the client at the same time
