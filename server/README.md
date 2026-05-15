# Plaid sandbox proxy

A minimal Express server that brokers Plaid API calls so the client_id and secret never live on device.

## Run

```bash
cd server
npm install
PLAID_CLIENT_ID=xxx PLAID_SECRET=yyy PLAID_ENV=sandbox npm start
```

## Point the Android app at it

Add to `local.properties`:

```
PLAID_BASE_URL=http://10.0.2.2:8787/
PLAID_CLIENT_ID=
PLAID_SECRET_SANDBOX=
```

The app's `client_id` / `secret` BuildConfig fields are still sent in the body but the proxy overwrites them before forwarding to Plaid. Leave them blank on device.

`10.0.2.2` is the host loopback for the Android emulator. On a real device, expose the server via your LAN IP.

## Endpoints

- `POST /link/token/create`
- `POST /item/public_token/exchange`
- `POST /liabilities/get`
- `POST /institutions/get_by_id`
- `GET /health`
