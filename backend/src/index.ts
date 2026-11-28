import { Hono } from 'hono';
import { customerApi, type Bindings } from './transactions';

export interface Env {
  AI?: any;
  KV?: KVNamespace;
  DB?: D1Database;
}

const app = new Hono<Bindings>();

// ==========================================
// 🤖 Cloudflare Workers AI - Bank Slip OCR (Authenticated)
// ==========================================
const MAX_OCR_IMAGE_BASE64_LENGTH = 5 * 1024 * 1024; // ~5 MB base64 payload limit

customerApi.post('/slips/verify-ocr', async (c) => {
  try {
    const body = await c.req.json<{
      image_base64: string;
      expected_amount?: number;
      expected_reference?: string;
    }>().catch(() => null);

    if (!body || typeof body.image_base64 !== 'string' || !body.image_base64.trim()) {
      return c.json({ error: 'image_base64 is required' }, 400);
    }

    if (body.image_base64.length > MAX_OCR_IMAGE_BASE64_LENGTH) {
      return c.json({ error: 'image_base64 exceeds maximum allowed size' }, 413);
    }

    // Clean base64 string
    const base64Data = body.image_base64.replace(/^data:image\/[a-zA-Z0-9.+-]+;base64,/, '');
    let binaryData: Uint8Array;
    try {
      binaryData = Uint8Array.from(atob(base64Data), (char) => char.charCodeAt(0));
    } catch {
      return c.json({ error: 'Invalid base64 image data' }, 400);
    }

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

app.route('/api', customerApi);

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

export default app;
