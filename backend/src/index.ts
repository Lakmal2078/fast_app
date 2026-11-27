import { Hono } from 'hono';
import { cors } from 'hono/cors';

export interface Env {
  AI?: any;
  KV?: KVNamespace;
  DB?: D1Database;
}

const app = new Hono<{ Bindings: Env }>();

app.use('*', cors());

// Health Check
app.get('/api/status', (c) => {
  return c.json({
    status: 'online',
    service: 'Fast Cash Sri Lanka Edge API & AI Engine',
    timestamp: Date.now(),
    features: ['REST_API', 'WORKERS_AI_OCR', 'EDGE_STORAGE'],
    version: '1.2.0'
  });
});

// ==========================================
// 🤖 Cloudflare Workers AI - Bank Slip OCR
// ==========================================
app.post('/api/slips/verify-ocr', async (c) => {
  try {
    const body = await c.req.json<{
      image_base64: string;
      expected_amount?: number;
      expected_reference?: string;
    }>();

    if (!body.image_base64) {
      return c.json({ error: 'image_base64 is required' }, 400);
    }

    // Clean base64 string
    const base64Data = body.image_base64.replace(/^data:image\/[a-z]+;base64,/, '');
    const binaryData = Uint8Array.from(atob(base64Data), (char) => char.charCodeAt(0));

    let extractedData = {
      bank: 'Unknown',
      amount: 0,
      reference: '',
      date: '',
      status: 'UNMATCHED',
      confidence: 0,
      raw_text: ''
    };

    // If Cloudflare AI binding is available, execute Llama 3.2 Vision model
    if (c.env.AI) {
      try {
        const aiResponse: any = await c.env.AI.run('@cf/meta/llama-3.2-11b-vision-instruct', {
          image: [...binaryData],
          prompt: `Analyze this bank transfer slip or receipt from Sri Lanka. Extract and return strict JSON with these keys:
          - "bank": (e.g. "BOC", "People's Bank", "Commercial Bank", "Sampath Bank", "iPay", "Unknown")
          - "amount": numeric value transferred
          - "reference": transaction or reference number found on slip
          - "date": date string
          - "is_valid_slip": boolean true/false
          
          Return ONLY valid JSON format.`
        });

        if (aiResponse && aiResponse.response) {
          try {
            const parsed = JSON.parse(aiResponse.response.trim());
            extractedData = {
              bank: parsed.bank || extractedData.bank,
              amount: parsed.amount || extractedData.amount,
              reference: parsed.reference || extractedData.reference,
              date: parsed.date || extractedData.date,
              status: 'UNMATCHED', // OCR alone cannot verify settlement
              confidence: 0,
              raw_text: aiResponse.response
            };
          } catch (jsonErr) {
            extractedData.raw_text = aiResponse.response;
          }
        }
      } catch (aiErr: any) {
        console.warn('AI Vision execution note:', aiErr.message);
      }
    }

    return c.json({
      success: true,
      data: extractedData,
      timestamp: Date.now()
    });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// ==========================================
// 💳 Transactions API
// ==========================================
app.get('/api/transactions', async (c) => {
  try {
    if (c.env.DB) {
      const status = c.req.query('status');
      let query = 'SELECT * FROM transactions ORDER BY timestamp DESC';
      let params: any[] = [];
      if (status && status !== 'ALL') {
        query = 'SELECT * FROM transactions WHERE status = ? ORDER BY timestamp DESC';
        params = [status];
      }
      const { results } = await c.env.DB.prepare(query).bind(...params).all();
      return c.json(results || []);
    }

    // KV / Memory Fallback
    if (c.env.KV) {
      const data = await c.env.KV.get('TRANSACTIONS_LIST');
      if (data) {
        return c.json(JSON.parse(data));
      }
    }

    // Default active transactions
    return c.json([
      {
        id: 1,
        reference: "TXN-7F42A",
        type: "DEPOSIT",
        status: "PROCESSING",
        amount: 25000,
        playerId: "184920412",
        paymentMethod: "Bank transfer",
        bank: "Bank of Ceylon (BOC)",
        accountNumber: "8823104921",
        timestamp: Date.now() - 3600000
      },
      {
        id: 2,
        reference: "TXN-19AC2",
        type: "WITHDRAWAL",
        status: "COMPLETED",
        amount: 12500,
        playerId: "184920412",
        paymentMethod: "Bank payout",
        securityCode: "8914-XK",
        bank: "Commercial Bank",
        accountNumber: "1094002914",
        timestamp: Date.now() - 86400000
      }
    ]);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

app.get('/api/transactions/:ref', async (c) => {
  try {
    const ref = c.req.param('ref').trim().toUpperCase();

    if (c.env.DB) {
      const result = await c.env.DB.prepare('SELECT * FROM transactions WHERE reference = ? LIMIT 1')
        .bind(ref)
        .first();
      if (result) return c.json(result);
    }

    return c.json({
      id: 1,
      reference: ref,
      type: "DEPOSIT",
      status: "PROCESSING",
      amount: 25000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      bank: "Bank of Ceylon (BOC)",
      timestamp: Date.now() - 1800000
    });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

app.post('/api/deposits', async (c) => {
  try {
    const body = await c.req.json<{
      amount: number;
      playerId: string;
      paymentMethod?: string;
      receiptKey?: string;
    }>();

    const randomSuffix = Math.random().toString(36).substring(2, 8).toUpperCase();
    const reference = `TXN-${randomSuffix}`;
    const timestamp = Date.now();

    if (c.env.DB) {
      await c.env.DB.prepare(`
        INSERT INTO transactions (reference, type, status, amount, playerId, paymentMethod, receiptKey, timestamp)
        VALUES (?, 'DEPOSIT', 'PENDING', ?, ?, ?, ?, ?)
      `).bind(
        reference,
        body.amount,
        body.playerId,
        body.paymentMethod || 'Bank transfer',
        body.receiptKey || null,
        timestamp
      ).run();
    }

    return c.json({
      success: true,
      reference,
      timestamp
    }, 201);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

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

    const randomSuffix = Math.random().toString(36).substring(2, 8).toUpperCase();
    const reference = `TXN-${randomSuffix}`;
    const timestamp = Date.now();

    if (c.env.DB) {
      await c.env.DB.prepare(`
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
    }

    return c.json({
      success: true,
      reference,
      timestamp
    }, 201);
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

app.get('/api/agent-accounts', (c) => {
  return c.json([
    {
      id: 1,
      label: "Bank of Ceylon (BOC) - Fast Cash LK",
      accountNumber: "8340192841",
      method: "Bank transfer",
      isActive: 1
    },
    {
      id: 2,
      label: "People's Bank - Walasmulla Operations",
      accountNumber: "2049102941",
      method: "Bank transfer",
      isActive: 1
    },
    {
      id: 3,
      label: "Sampath Bank - Fast Cash Corporate",
      accountNumber: "1004819204",
      method: "iPay / Bank",
      isActive: 1
    }
  ]);
});

app.get('/api/settings', (c) => {
  return c.json({
    whatsappNumber: '+94765865387',
    minTransaction: 1000,
    maxTransaction: 500000,
    promoCode: 'VGSL'
  });
});

app.get('/api/notifications', (c) => {
  return c.json([
    {
      id: 1,
      title: "System Ready",
      message: "Fast Cash official agent bridge is active. Agents online.",
      isRead: 0,
      createdAt: Date.now() - 7200000
    },
    {
      id: 2,
      title: "Request Processing",
      message: "TXN-7F42A is currently under agent verification.",
      isRead: 0,
      createdAt: Date.now() - 3600000
    }
  ]);
});

export default app;
