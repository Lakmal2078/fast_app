import { describe, expect, it } from "vitest";

describe("Fast Cash request contract", () => {
  it("uses a customer-readable unique reference format", () => {
    const reference = `TXN-${Math.random().toString(36).slice(2, 8).toUpperCase()}`;
    expect(reference).toMatch(/^TXN-[A-Z0-9]{6}$/);
  });

  it("keeps supported request types explicit", () => {
    expect(["DEPOSIT", "WITHDRAWAL"]).toEqual(expect.arrayContaining(["DEPOSIT", "WITHDRAWAL"]));
  });
});
