import { Hono } from 'hono';
import { cors } from 'hono/cors';

export interface Env {
  DB: D1Database;
}

const app = new Hono<{ Bindings: Env }>();

// Enable CORS for cross-origin and mobile access
app.use('*', cors());

// Health Check
app.get('/api/status', (c) => {
  return c.json({
    status: 'online',
    service: 'Fast Cash Sri Lanka Edge API',
    timestamp: Date.now(),
    version: '1.0.0'
  });
});

// GET /api/transactions - List all transactions
app.get('/api/transactions', async (c) => {
  try {
    const status = c.req.query('status');
    let query = 'SELECT * FROM transactions ORDER BY timestamp DESC';
    let params: any[] = [];

    if (status && status !== 'ALL') {
      query = 'SELECT * FROM transactions WHERE status = ? ORDER BY timestamp DESC';
      params = [status];
    }

    const { results } = await c.env.DB.prepare(query).bind(...params).all();
    return c.json(results || []);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// GET /api/transactions/:ref - Lookup transaction by reference code
app.get('/api/transactions/:ref', async (c) => {
  try {
    const ref = c.req.param('ref');
    const result = await c.env.DB.prepare('SELECT * FROM transactions WHERE reference = ? LIMIT 1')
      .bind(ref.trim())
      .first();

    if (!result) {
      return c.json({ error: 'Transaction not found' }, 404);
    }
    return c.json(result);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// POST /api/deposits - Submit new deposit
app.post('/api/deposits', async (c) => {
  try {
    const body = await c.req.json<{
      amount: number;
      playerId: string;
      paymentMethod?: string;
      receiptKey?: string;
    }>();

    if (!body.amount || !body.playerId) {
      return c.json({ error: 'Missing required fields: amount, playerId' }, 400);
    }

    const randomSuffix = Math.random().toString(36).substring(2, 8).toUpperCase();
    const reference = `TXN-${randomSuffix}`;
    const timestamp = Date.now();
    const paymentMethod = body.paymentMethod || 'Bank transfer';

    const insertResult = await c.env.DB.prepare(`
      INSERT INTO transactions (reference, type, status, amount, playerId, paymentMethod, receiptKey, timestamp)
      VALUES (?, 'DEPOSIT', 'PENDING', ?, ?, ?, ?, ?)
    `).bind(
      reference,
      body.amount,
      body.playerId,
      paymentMethod,
      body.receiptKey || null,
      timestamp
    ).run();

    // Create Notification
    await c.env.DB.prepare(`
      INSERT INTO notifications (title, message, isRead, createdAt)
      VALUES (?, ?, 0, ?)
    `).bind(
      'Deposit Received',
      `Request ${reference} for LKR ${body.amount} submitted and under review.`,
      timestamp
    ).run();

    return c.json({
      success: true,
      reference,
      id: insertResult.meta.last_row_id,
      timestamp
    }, 201);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// POST /api/withdrawals - Submit new withdrawal
app.post('/api/withdrawals', async (c) => {
  try {
    const body = await c.req.json<{
      amount: number;
      playerId: string;
      securityCode: string;
      bank: string;
      accountNumber: string;
      contactNumber?: string;
    }>();

    if (!body.amount || !body.playerId || !body.securityCode || !body.bank || !body.accountNumber) {
      return c.json({ error: 'Missing required withdrawal fields' }, 400);
    }

    const randomSuffix = Math.random().toString(36).substring(2, 8).toUpperCase();
    const reference = `TXN-${randomSuffix}`;
    const timestamp = Date.now();

    const insertResult = await c.env.DB.prepare(`
      INSERT INTO transactions (reference, type, status, amount, playerId, paymentMethod, securityCode, bank, accountNumber, contactNumber, timestamp)
      VALUES (?, 'WITHDRAWAL', 'PENDING', ?, ?, 'Bank payout', ?, ?, ?, ?, ?)
    `).bind(
      reference,
      body.amount,
      body.playerId,
      body.securityCode,
      body.bank,
      body.accountNumber,
      body.contactNumber || null,
      timestamp
    ).run();

    // Create Notification
    await c.env.DB.prepare(`
      INSERT INTO notifications (title, message, isRead, createdAt)
      VALUES (?, ?, 0, ?)
    `).bind(
      'Withdrawal Received',
      `Request ${reference} for LKR ${body.amount} submitted and queued for payout.`,
      timestamp
    ).run();

    return c.json({
      success: true,
      reference,
      id: insertResult.meta.last_row_id,
      timestamp
    }, 201);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// PATCH /api/admin/transactions/:id - Admin update transaction status
app.patch('/api/admin/transactions/:id', async (c) => {
  try {
    const id = parseInt(c.req.param('id'), 10);
    const body = await c.req.json<{
      status: string;
      reason?: string;
    }>();

    if (!body.status) {
      return c.json({ error: 'Status is required' }, 400);
    }

    const tx = await c.env.DB.prepare('SELECT * FROM transactions WHERE id = ?').bind(id).first<any>();
    if (!tx) {
      return c.json({ error: 'Transaction not found' }, 404);
    }

    await c.env.DB.prepare(`
      UPDATE transactions SET status = ?, rejectionReason = ? WHERE id = ?
    `).bind(body.status, body.reason || null, id).run();

    // Create Notification
    const title = body.status === 'COMPLETED' ? 'Request Completed' :
                  body.status === 'REJECTED' ? 'Request Rejected' : 'Status Updated';
    const message = body.status === 'REJECTED' && body.reason ?
                    `${tx.reference} rejected: ${body.reason}` : `${tx.reference} status is now ${body.status}.`;

    await c.env.DB.prepare(`
      INSERT INTO notifications (title, message, isRead, createdAt)
      VALUES (?, ?, 0, ?)
    `).bind(title, message, Date.now()).run();

    return c.json({ success: true, id, status: body.status });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// GET /api/agent-accounts - Get active bank agent accounts
app.get('/api/agent-accounts', async (c) => {
  try {
    const { results } = await c.env.DB.prepare('SELECT * FROM agent_accounts WHERE isActive = 1').all();
    return c.json(results || []);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// GET /api/notifications - Get all notifications
app.get('/api/notifications', async (c) => {
  try {
    const { results } = await c.env.DB.prepare('SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 50').all();
    return c.json(results || []);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// POST /api/notifications/read-all - Mark all notifications as read
app.post('/api/notifications/read-all', async (c) => {
  try {
    await c.env.DB.prepare('UPDATE notifications SET isRead = 1').run();
    return c.json({ success: true });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// GET /api/settings - App & Network Settings
app.get('/api/settings', async (c) => {
  try {
    const settings = await c.env.DB.prepare('SELECT * FROM app_settings WHERE id = 1 LIMIT 1').first();
    return c.json(settings || {
      whatsappNumber: '+94765865387',
      minTransaction: 1000,
      maxTransaction: 500000,
      promoCode: 'VGSL'
    });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

export default app;
