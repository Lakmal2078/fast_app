import { Hono, type Context } from 'hono';
import type { Env } from './index';

export type Bindings = { Bindings: Env; Variables: { customer: string } };
export const customerApi = new Hono<Bindings>();
export async function digest(value: string) {
  const bytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return Array.from(new Uint8Array(bytes), n => n.toString(16).padStart(2, '0')).join('');
}

customerApi.use('*', async (c, next) => {
  if (c.req.path === '/api/status') return next();
  c.header('Cache-Control', 'private, no-store');
  if (!c.env.DB) return c.json({ error: 'Storage unavailable' }, 503);
  const token = /^Bearer ([a-f0-9]{64})$/.exec(c.req.header('Authorization') ?? '')?.[1];
  const customer = token && await c.env.DB.prepare(
    'SELECT customer_id FROM customer_credentials WHERE key_hash = ?'
  ).bind(await digest(token)).first<string>('customer_id');
  if (!customer) return c.json({ error: 'Sign in required' }, 401);
  c.set('customer', customer);
  await next();
});

customerApi.get('/session', c => c.json({ customerId: c.get('customer') }));
const fields = 'id, reference, type, status, amount, playerId, paymentMethod, rejectionReason, timestamp';

customerApi.get('/transactions', async c => {
  const { results } = await c.env.DB!.prepare(
    `SELECT ${fields} FROM transactions WHERE customer_id = ? ORDER BY timestamp DESC`
  ).bind(c.get('customer')).all();
  return c.json(results);
});
customerApi.get('/transactions/:ref', async c => {
  const result = await c.env.DB!.prepare(
    `SELECT ${fields} FROM transactions WHERE customer_id = ? AND reference = ?`
  ).bind(c.get('customer'), c.req.param('ref').trim().toUpperCase()).first();
  return result ? c.json(result) : c.json({ error: 'Request not found' }, 404);
});

function bounded(value: unknown, max: number, min = 1): value is string {
  return typeof value === 'string' && value.trim().length >= min && value.length <= max;
}
async function submit(c: Context<Bindings>, withdrawal: boolean) {
  const requestKey = c.req.header('Idempotency-Key');
  if (!requestKey || !/^[a-f0-9-]{36}$/i.test(requestKey)) return c.json({ error: 'Request key required' }, 400);
  const body = await c.req.json().catch(() => null);
  if (!body || !Number.isSafeInteger(body.amount) || body.amount < 1000 || body.amount > 500000 ||
      !bounded(body.playerId, 64, 4)) return c.json({ error: 'Invalid amount or player ID' }, 400);
  if (withdrawal && (!bounded(body.bank, 100) || !bounded(body.accountNumber, 64, 6) ||
      !bounded(body.securityCode, 128) || !bounded(body.contactNumber ?? '', 32, 0))) {
    return c.json({ error: 'Invalid withdrawal details' }, 400);
  }
  if (!withdrawal && (!bounded(body.paymentMethod ?? 'Bank transfer', 64) ||
      !bounded(body.receiptKey ?? '', 128, 0))) return c.json({ error: 'Invalid deposit details' }, 400);
  const values = [
    withdrawal ? 'WITHDRAWAL' : 'DEPOSIT', body.amount, body.playerId.trim(),
    withdrawal ? 'Bank payout' : (body.paymentMethod ?? 'Bank transfer').trim(),
    withdrawal ? body.bank.trim() : null, withdrawal ? body.accountNumber.trim() : null,
    withdrawal ? body.securityCode.trim() : null, withdrawal ? body.contactNumber?.trim() || null : null,
    withdrawal ? null : body.receiptKey?.trim() || null
  ];
  const owner = c.get('customer');
  const hash = await digest(JSON.stringify(values));
  const reference = 'TXN-' + crypto.randomUUID().toUpperCase();
  // A single conditional insert and read in a D1 transaction also handles concurrent retries.
  const db = c.env.DB!;
  const [, read] = await db.batch<Record<string, unknown> & { request_hash: string; reference: string }>([
    db.prepare(`INSERT INTO transactions
      (customer_id, request_key, request_hash, reference, timestamp, type, amount, playerId,
       paymentMethod, bank, accountNumber, securityCode, contactNumber, receiptKey)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(customer_id, request_key) DO NOTHING`)
      .bind(owner, requestKey, hash, reference, Date.now(), ...values),
    db.prepare(`SELECT ${fields}, request_hash FROM transactions WHERE customer_id = ? AND request_key = ?`)
      .bind(owner, requestKey)
  ]);
  const row = read.results[0];
  if (!row) return c.json({ error: 'Request not confirmed' }, 503);
  if (row.request_hash !== hash) return c.json({ error: 'Request key already used for different details' }, 409);
  const { request_hash, ...transaction } = row;
  return c.json(transaction, row.reference === reference ? 201 : 200);
}
customerApi.post('/deposits', c => submit(c, false));
customerApi.post('/withdrawals', c => submit(c, true));
customerApi.get('/agent-accounts', async c => c.json((await c.env.DB!.prepare(
  'SELECT id, label, accountNumber, method, isActive FROM agent_accounts WHERE isActive = 1'
).all()).results));
customerApi.get('/settings', async c => c.json(await c.env.DB!.prepare(
  'SELECT whatsappNumber, minTransaction, maxTransaction, promoCode FROM app_settings WHERE id = 1'
).first()));
// Notifications are local until the backend has owner-scoped notification records.
customerApi.get('/notifications', c => c.json([]));
