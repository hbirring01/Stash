// Minimal Plaid sandbox proxy.
// The Android app talks to this server instead of plaid.com so the secret
// never lives on device. Endpoints are forwarded to Plaid with the
// server-side client_id + secret injected.

const express = require('express');
const fetch = require('node-fetch');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const cors = require('cors');

const app = express();

// --- Security hardening ---
app.use(helmet());                           // secure HTTP headers
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',    // restrict in production
  methods: ['POST', 'GET'],
}));
app.use(express.json({ limit: '1mb' }));

// Rate limit: 60 requests per minute per IP
app.use(rateLimit({
  windowMs: 60_000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later.' },
}));

// --- API key authentication middleware ---
// Set PROXY_API_KEY env var on the server; the Android app sends it
// in the x-api-key header with every request.
const PROXY_API_KEY = process.env.PROXY_API_KEY;
if (!PROXY_API_KEY) {
  console.error('Missing PROXY_API_KEY — all requests will be rejected');
  process.exit(1);
}

function requireApiKey(req, res, next) {
  const key = req.headers['x-api-key'];
  if (!key || key !== PROXY_API_KEY) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  next();
}

// Apply auth to all routes except /health
app.use((req, res, next) => {
  if (req.path === '/health') return next();
  requireApiKey(req, res, next);
});

const PORT = process.env.PORT || 8787;
const CLIENT_ID = process.env.PLAID_CLIENT_ID;
const SECRET = process.env.PLAID_SECRET;
const ENV = (process.env.PLAID_ENV || 'sandbox').toLowerCase();

if (!CLIENT_ID || !SECRET) {
  console.error('Missing PLAID_CLIENT_ID or PLAID_SECRET');
  process.exit(1);
}

const baseUrl =
  ENV === 'production' ? 'https://production.plaid.com'
  : ENV === 'development' ? 'https://development.plaid.com'
  : 'https://sandbox.plaid.com';

async function forward(path, body) {
  const resp = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...body, client_id: CLIENT_ID, secret: SECRET }),
  });
  const text = await resp.text();
  return { status: resp.status, body: text };
}

function relay(path) {
  return async (req, res) => {
    try {
      const incoming = req.body || {};
      // Strip any client-supplied credentials.
      delete incoming.client_id;
      delete incoming.secret;
      const r = await forward(path, incoming);
      res.status(r.status).type('application/json').send(r.body);
    } catch (err) {
      res.status(500).json({ error: err.message });
    }
  };
}

app.post('/link/token/create', relay('/link/token/create'));
app.post('/item/public_token/exchange', relay('/item/public_token/exchange'));
app.post('/liabilities/get', relay('/liabilities/get'));
app.post('/institutions/get_by_id', relay('/institutions/get_by_id'));
app.post('/transactions/sync', relay('/transactions/sync'));

app.get('/health', (_req, res) => res.json({ ok: true }));

app.listen(PORT, () => {
  console.log(`Plaid proxy listening on :${PORT} (${ENV})`);
});
