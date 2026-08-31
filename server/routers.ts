import { z } from "zod";
import * as XLSX from "xlsx";
import { createHash } from "node:crypto";
import { desc, eq } from "drizzle-orm";
import { COOKIE_NAME } from "@shared/const";
import { getSessionCookieOptions } from "./_core/cookies";
import { systemRouter } from "./_core/systemRouter";
import { publicProcedure, protectedProcedure, router } from "./_core/trpc";
import { notifyOwner } from "./_core/notification";
import { getDb } from "./db";
import { storagePut } from "./storage";
import { agentAccounts, appSettings, notifications, statementBatches, statementRows, transactionEvents, transactions, users } from "../drizzle/schema";

const requestInput = z.object({
  type: z.enum(["DEPOSIT", "WITHDRAWAL"]),
  amount: z.number().int().min(1000).max(500000),
  playerId: z.string().trim().min(3).max(64),
  paymentMethod: z.string().max(64).optional(),
  securityCode: z.string().max(128).optional(),
  bank: z.string().max(160).optional(),
  accountNumber: z.string().max(128).optional(),
  contactNumber: z.string().max(40).optional(),
  receiptKey: z.string().max(1000).optional(),
});

export const appRouter = router({
  system: systemRouter,
  auth: router({
    me: publicProcedure.query(({ ctx }) => ctx.user),
    logout: publicProcedure.mutation(({ ctx }) => { ctx.res.clearCookie(COOKIE_NAME, { ...getSessionCookieOptions(ctx.req), maxAge: -1 }); return { success: true } as const; }),
  }),
  profile: router({
    update: protectedProcedure.input(z.object({ name: z.string().trim().min(2).max(120), playerId: z.string().trim().min(3).max(64) })).mutation(async ({ input, ctx }) => {
      const db = await getDb(); if (!db) throw new Error("Database is not available.");
      await db.update(users).set({ name: input.name, playerId: input.playerId }).where(eq(users.id, ctx.user.id));
      return { success: true };
    }),
  }),
  requests: router({
    uploadReceipt: protectedProcedure.input(z.object({ fileName: z.string().max(180), dataUrl: z.string().max(8_000_000) })).mutation(async ({ input, ctx }) => {
      const [header, encoded] = input.dataUrl.split(",", 2);
      const contentType = header?.slice(5);
      if (!contentType || !["image/png", "image/jpeg", "image/webp"].includes(contentType) || !encoded) throw new Error("Only PNG, JPEG, or WebP receipt images are allowed.");
      const buffer = Buffer.from(encoded, "base64");
      if (buffer.byteLength > 5_000_000) throw new Error("Receipt image must be smaller than 5 MB.");
      return storagePut(`receipts/${ctx.user.id}/${input.fileName.replace(/[^a-zA-Z0-9._-]/g, "-")}`, buffer, contentType);
    }),
    create: protectedProcedure.input(requestInput).mutation(async ({ input, ctx }) => {
      const db = await getDb(); if (!db) throw new Error("Database is not available.");
      const reference = `TXN-${Math.random().toString(36).slice(2, 8).toUpperCase()}`;
      const [created] = await db.insert(transactions).values({ reference, userId: ctx.user.id, type: input.type, amount: String(input.amount), playerId: input.playerId, paymentMethod: input.paymentMethod, securityCode: input.securityCode, bank: input.bank, accountNumber: input.accountNumber, contactNumber: input.contactNumber, receiptKey: input.receiptKey }).$returningId();
      if (created?.id) {
        await db.insert(transactionEvents).values({ transactionId: created.id, status: "PENDING", note: "Request submitted" });
        await db.insert(notifications).values({ userId: ctx.user.id, title: "Request received", message: `${reference} is now under review.` });
      }
      await notifyOwner({ title: `New ${input.type.toLowerCase()} request`, content: `${reference} · LKR ${input.amount.toLocaleString()} · Player ID ${input.playerId}` });
      return { reference };
    }),
    mine: protectedProcedure.query(async ({ ctx }) => {
      const db = await getDb(); if (!db) throw new Error("Database is not available.");
      const rows = await db.select().from(transactions).where(eq(transactions.userId, ctx.user.id)).orderBy(desc(transactions.createdAt));
      return rows;
    }),
    notifications: protectedProcedure.query(async ({ ctx }) => {
      const db = await getDb(); if (!db) throw new Error("Database is not available.");
      return db.select().from(notifications).where(eq(notifications.userId, ctx.user.id)).orderBy(desc(notifications.createdAt));
    }),
  }),
  settings: router({
    public: publicProcedure.query(async () => { const db = await getDb(); if (!db) throw new Error("Database is not available."); const [settings] = await db.select().from(appSettings).limit(1); const accounts = await db.select().from(agentAccounts).where(eq(agentAccounts.active, 1)); return { settings, accounts }; }),
    update: protectedProcedure.input(z.object({ whatsappNumber: z.string().min(8).max(40), minTransaction: z.number().int().min(1), maxTransaction: z.number().int().min(1), promoCode: z.string().max(64), accounts: z.array(z.object({ label: z.string().max(160), accountNumber: z.string().max(128), method: z.string().max(64), active: z.number().int().min(0).max(1) })) })).mutation(async ({ input, ctx }) => { if (ctx.user.role !== "admin") throw new Error("Administrator access required."); const db = await getDb(); if (!db) throw new Error("Database is not available."); const { accounts, ...settings } = input; await db.update(appSettings).set(settings).where(eq(appSettings.id, 1)); if (accounts.length) { await db.delete(agentAccounts); await db.insert(agentAccounts).values(accounts); } return { success: true }; }),
  }),
  reconciliation: router({
    importStatement: protectedProcedure.input(z.object({ provider: z.enum(["BOC", "PEOPLES_BANK", "LOLC", "SAMPATH", "IPAY"]), fileName: z.string().max(180), data: z.string().max(15_000_000) })).mutation(async ({ input, ctx }) => {
      if (ctx.user.role !== "admin") throw new Error("Administrator access required.");
      const db = await getDb(); if (!db) throw new Error("Database is not available.");
      const buffer = Buffer.from(input.data, "base64");
      if (buffer.byteLength > 10_000_000) throw new Error("Statement must be smaller than 10 MB.");
      const fileHash = createHash("sha256").update(buffer).digest("hex");
      const duplicate = await db.select().from(statementBatches).where(eq(statementBatches.fileHash, fileHash)).limit(1);
      if (duplicate.length) throw new Error("This statement file has already been imported.");
      const workbook = XLSX.read(buffer, { type: "buffer", cellDates: true });
      const firstSheet = workbook.Sheets[workbook.SheetNames[0] || ""];
      if (!firstSheet) throw new Error("The statement has no readable worksheet.");
      const rawRows = XLSX.utils.sheet_to_json<Record<string, unknown>>(firstSheet, { defval: "" });
      const headerValue = (row: Record<string, unknown>, names: string[]) => { const key = Object.keys(row).find((candidate) => names.some((name) => candidate.toLowerCase().replace(/[^a-z0-9]/g, "").includes(name))); return key ? String(row[key] ?? "").trim() : ""; };
      const normalized = rawRows.map((row, index) => { const credit = headerValue(row, ["credit", "deposit", "inward"]); const debit = headerValue(row, ["debit", "withdrawal", "outward"]); const amount = Number((credit || debit || headerValue(row, ["amount", "value"])).replace(/[^0-9.-]/g, "")); return { externalId: headerValue(row, ["id", "transaction", "serial"]) || `${fileHash.slice(0, 12)}-${index + 1}`, transactionDate: headerValue(row, ["date", "valuedate", "timestamp"]), amount: String(Math.abs(amount)), direction: Number(credit.replace(/[^0-9.-]/g, "")) > 0 ? "CREDIT" as const : "DEBIT" as const, account: headerValue(row, ["account", "accountnumber"]), reference: headerValue(row, ["reference", "ref", "remarks"]), description: headerValue(row, ["description", "narration", "details"]), }; }).filter((row) => Number(row.amount) > 0);
      const [batch] = await db.insert(statementBatches).values({ provider: input.provider, fileKey: `statements/${ctx.user.id}/${input.fileName}`, fileHash, rowCount: normalized.length, importedBy: ctx.user.id }).$returningId();
      const pending = await db.select().from(transactions).where(eq(transactions.status, "PENDING"));
      let verified = 0; let ambiguous = 0;
      for (const row of normalized) {
        const refMatch = pending.filter((tx) => row.reference && row.reference.toLowerCase().includes(tx.reference.toLowerCase()));
        const amountMatch = pending.filter((tx) => Number(tx.amount) === Number(row.amount) && (row.direction === "CREDIT" ? tx.type === "DEPOSIT" : tx.type === "WITHDRAWAL"));
        const candidates = refMatch.length ? refMatch : amountMatch;
        const status = candidates.length === 1 ? "VERIFIED" : candidates.length > 1 ? "AMBIGUOUS" : "UNMATCHED";
        if (status === "VERIFIED") verified += 1; if (status === "AMBIGUOUS") ambiguous += 1;
        const transactionId = status === "VERIFIED" ? candidates[0]?.id : undefined;
        await db.insert(statementRows).values({ batchId: batch.id, externalId: row.externalId, transactionDate: row.transactionDate || "unknown", amount: row.amount, direction: row.direction, account: row.account || null, reference: row.reference || null, description: row.description || null, verificationStatus: status, transactionId });
        if (transactionId) { await db.update(transactions).set({ status: "PROCESSING" }).where(eq(transactions.id, transactionId)); await db.insert(transactionEvents).values({ transactionId, status: "PROCESSING", note: `Auto-verified from ${input.provider} statement` }); }
      }
      if (ambiguous || normalized.length - verified - ambiguous) await notifyOwner({ title: "Reconciliation exceptions need review", content: `${input.provider}: ${verified} verified, ${ambiguous} ambiguous, ${normalized.length - verified - ambiguous} unmatched.` });
      return { provider: input.provider, rows: normalized.length, verified, ambiguous, unmatched: normalized.length - verified - ambiguous, batchId: batch.id };
    }),
    queue: protectedProcedure.query(async ({ ctx }) => { if (ctx.user.role !== "admin") throw new Error("Administrator access required."); const db = await getDb(); if (!db) throw new Error("Database is not available."); return db.select().from(statementRows).orderBy(desc(statementRows.createdAt)); }),
    resolve: protectedProcedure.input(z.object({ id: z.number(), action: z.enum(["VERIFY", "IGNORE"]), transactionId: z.number().optional(), note: z.string().max(500).optional() })).mutation(async ({ input, ctx }) => { if (ctx.user.role !== "admin") throw new Error("Administrator access required."); const db = await getDb(); if (!db) throw new Error("Database is not available."); const status = input.action === "VERIFY" ? "VERIFIED" : "EXCEPTION"; await db.update(statementRows).set({ verificationStatus: status, transactionId: input.transactionId }).where(eq(statementRows.id, input.id)); if (input.transactionId && input.action === "VERIFY") await db.update(transactions).set({ status: "PROCESSING" }).where(eq(transactions.id, input.transactionId)); return { success: true, note: input.note }; }),
  }),
  admin: router({
    list: protectedProcedure.input(z.object({ search: z.string().optional(), status: z.string().optional(), type: z.string().optional(), paymentMethod: z.string().optional() }).optional()).query(async ({ input, ctx }) => {
      if (ctx.user.role !== "admin") throw new Error("Administrator access required.");
      const db = await getDb(); if (!db) throw new Error("Database is not available.");
      const rows = await db.select().from(transactions).orderBy(desc(transactions.createdAt));
      const query = input?.search?.toLowerCase().trim();
      return rows.filter((row) => (!query || `${row.reference} ${row.playerId} ${row.bank ?? ""}`.toLowerCase().includes(query)) && (!input?.status || input.status === "ALL" || row.status === input.status) && (!input?.type || input.type === "ALL" || row.type === input.type) && (!input?.paymentMethod || input.paymentMethod === "ALL" || row.paymentMethod === input.paymentMethod));
    }),
    updateStatus: protectedProcedure.input(z.object({ id: z.number(), status: z.enum(["PENDING", "PROCESSING", "COMPLETED", "REJECTED"]), rejectionReason: z.string().trim().max(1000).optional() })).mutation(async ({ input, ctx }) => {
      if (ctx.user.role !== "admin") throw new Error("Administrator access required.");
      if (input.status === "REJECTED" && !input.rejectionReason) throw new Error("A rejection reason is required.");
      const db = await getDb(); if (!db) throw new Error("Database is not available.");
      await db.update(transactions).set({ status: input.status, rejectionReason: input.status === "REJECTED" ? input.rejectionReason : null }).where(eq(transactions.id, input.id));
      await db.insert(transactionEvents).values({ transactionId: input.id, status: input.status, note: input.status === "REJECTED" ? input.rejectionReason : null });
      return { success: true };
    }),
  }),
});

export type AppRouter = typeof appRouter;
