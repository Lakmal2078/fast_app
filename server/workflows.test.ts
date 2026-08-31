import { describe, expect, it } from "vitest";
import { appRouter } from "./routers";
import type { TrpcContext } from "./_core/context";

const baseContext = (role: "user" | "admin"): TrpcContext => ({
  user: { id: 7, openId: "test", name: "Test", email: "test@example.com", loginMethod: "test", role, createdAt: new Date(), updatedAt: new Date(), lastSignedIn: new Date() },
  req: {} as TrpcContext["req"],
  res: {} as TrpcContext["res"],
});

describe("Fast Cash protected workflows", () => {
  it("blocks non-admin queue access", async () => {
    const caller = appRouter.createCaller(baseContext("user"));
    await expect(caller.admin.list({ search: "TXN" })).rejects.toThrow("Administrator access required");
  });

  it("requires a reason when an admin rejects a request", async () => {
    const caller = appRouter.createCaller(baseContext("admin"));
    await expect(caller.admin.updateStatus({ id: 1, status: "REJECTED" })).rejects.toThrow("A rejection reason is required");
  });

  it("rejects non-image receipt payloads before storage", async () => {
    const caller = appRouter.createCaller(baseContext("user"));
    await expect(caller.requests.uploadReceipt({ fileName: "receipt.txt", dataUrl: "data:text/plain;base64,SGVsbG8=" })).rejects.toThrow("Only PNG, JPEG, or WebP");
  });
});
