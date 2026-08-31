import { int, mysqlEnum, mysqlTable, text, timestamp, varchar } from "drizzle-orm/mysql-core";

/**
 * Core user table backing auth flow.
 * Extend this file with additional tables as your product grows.
 * Columns use camelCase to match both database fields and generated types.
 */
export const users = mysqlTable("users", {
  /**
   * Surrogate primary key. Auto-incremented numeric value managed by the database.
   * Use this for relations between tables.
   */
  id: int("id").autoincrement().primaryKey(),
  /** Manus OAuth identifier (openId) returned from the OAuth callback. Unique per user. */
  openId: varchar("openId", { length: 64 }).notNull().unique(),
  name: text("name"),
  email: varchar("email", { length: 320 }),
  loginMethod: varchar("loginMethod", { length: 64 }),
  playerId: varchar("playerId", { length: 64 }),
  role: mysqlEnum("role", ["user", "admin"]).default("user").notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  lastSignedIn: timestamp("lastSignedIn").defaultNow().notNull(),
});

export type User = typeof users.$inferSelect;
export type InsertUser = typeof users.$inferInsert;

export const transactions = mysqlTable("transactions", {
  id: int("id").autoincrement().primaryKey(),
  reference: varchar("reference", { length: 24 }).notNull().unique(),
  userId: int("userId"),
  type: mysqlEnum("type", ["DEPOSIT", "WITHDRAWAL"]).notNull(),
  status: mysqlEnum("status", ["PENDING", "PROCESSING", "COMPLETED", "REJECTED"]).default("PENDING").notNull(),
  amount: varchar("amount", { length: 32 }).notNull(),
  playerId: varchar("playerId", { length: 64 }).notNull(),
  paymentMethod: varchar("paymentMethod", { length: 64 }),
  securityCode: varchar("securityCode", { length: 128 }),
  bank: varchar("bank", { length: 160 }),
  accountNumber: varchar("accountNumber", { length: 128 }),
  contactNumber: varchar("contactNumber", { length: 40 }),
  receiptKey: text("receiptKey"),
  rejectionReason: text("rejectionReason"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export const transactionEvents = mysqlTable("transactionEvents", {
  id: int("id").autoincrement().primaryKey(),
  transactionId: int("transactionId").notNull(),
  status: varchar("status", { length: 24 }).notNull(),
  note: text("note"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export const notifications = mysqlTable("notifications", {
  id: int("id").autoincrement().primaryKey(),
  userId: int("userId").notNull(),
  title: varchar("title", { length: 160 }).notNull(),
  message: text("message").notNull(),
  readAt: timestamp("readAt"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type Transaction = typeof transactions.$inferSelect;
export type TransactionEvent = typeof transactionEvents.$inferSelect;
export type Notification = typeof notifications.$inferSelect;

export const agentAccounts = mysqlTable("agentAccounts", {
  id: int("id").autoincrement().primaryKey(),
  label: varchar("label", { length: 160 }).notNull(),
  accountNumber: varchar("accountNumber", { length: 128 }).notNull(),
  method: varchar("method", { length: 64 }).notNull(),
  active: int("active").default(1).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export const statementBatches = mysqlTable("statementBatches", {
  id: int("id").autoincrement().primaryKey(),
  provider: varchar("provider", { length: 64 }).notNull(),
  fileKey: text("fileKey").notNull(),
  fileHash: varchar("fileHash", { length: 128 }).notNull().unique(),
  rowCount: int("rowCount").default(0).notNull(),
  importedBy: int("importedBy").notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export const statementRows = mysqlTable("statementRows", {
  id: int("id").autoincrement().primaryKey(),
  batchId: int("batchId").notNull(),
  externalId: varchar("externalId", { length: 160 }).notNull(),
  transactionDate: varchar("transactionDate", { length: 64 }).notNull(),
  amount: varchar("amount", { length: 32 }).notNull(),
  direction: mysqlEnum("direction", ["CREDIT", "DEBIT"]).notNull(),
  account: varchar("account", { length: 128 }),
  reference: varchar("reference", { length: 160 }),
  description: text("description"),
  verificationStatus: mysqlEnum("verificationStatus", ["UNMATCHED", "AMBIGUOUS", "VERIFIED", "EXCEPTION"]).default("UNMATCHED").notNull(),
  transactionId: int("transactionId"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export const appSettings = mysqlTable("appSettings", {
  id: int("id").autoincrement().primaryKey(),
  whatsappNumber: varchar("whatsappNumber", { length: 40 }).notNull(),
  minTransaction: int("minTransaction").notNull(),
  maxTransaction: int("maxTransaction").notNull(),
  promoCode: varchar("promoCode", { length: 64 }).notNull(),
});
