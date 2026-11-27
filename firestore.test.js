const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "lakmal_admin_789";
const ADMIN_EMAIL = "lakmalsujith25@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("transactions").get());
  await assertFails(
    unauth.collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      reference: "TXN-1001",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 5000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(unauth.collection("app_config").doc("settings").get());
});

test("Player can create PENDING deposit and withdrawal, read own transactions, and cannot self-approve", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Valid deposit create (Kotlin-aligned map, no id field)
  await assertSucceeds(
    alice.collection("transactions").doc("tx_dep_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice",
      reference: "TXN-DEPOSIT1",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 25000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      agentAccountLabel: "People's Bank (Walasmulla Branch) - VGS Lakmal",
      agentAccountNumber: "120200380030196",
      receiptKey: "PB-99812",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid withdrawal create
  await assertSucceeds(
    alice.collection("transactions").doc("tx_wdr_1").set({
      userId: ALICE_UID,
      reference: "TXN-WITHDR1",
      type: "WITHDRAWAL",
      status: "PENDING",
      amount: 15000,
      playerId: "184920412",
      paymentMethod: "Bank payout",
      securityCode: "SEC-4412",
      bank: "Sampath Bank",
      accountNumber: "105456146706",
      contactNumber: "0771234567",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds; unfiltered list query fails for non-admin
  await assertSucceeds(
    alice.collection("transactions").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(alice.collection("transactions").get());

  // Player cannot create already COMPLETED deposit
  await assertFails(
    alice.collection("transactions").doc("tx_dep_cheat").set({
      userId: ALICE_UID,
      reference: "TXN-CHEAT",
      type: "DEPOSIT",
      status: "COMPLETED",
      amount: 25000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Player cannot update status to COMPLETED
  await assertFails(
    alice.collection("transactions").doc("tx_dep_1").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field rejected on create
  await assertFails(
    alice.collection("transactions").doc("tx_dep_ghost").set({
      userId: ALICE_UID,
      reference: "TXN-GHOST",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 5000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      isVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    alice.collection("transactions").doc("tx_dep_future").set({
      userId: ALICE_UID,
      reference: "TXN-FUTURE",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 5000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Deposit with valid compressed slipImageBase64 succeeds
  await assertSucceeds(
    alice.collection("transactions").doc("tx_dep_slip").set({
      userId: ALICE_UID,
      reference: "TXN-SLIP01",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 10000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      slipImageBase64: "/9j/4AAQSkZJRgABAQAAAQABAAD/",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Oversized slipImageBase64 (> 350000 chars) rejected
  await assertFails(
    alice.collection("transactions").doc("tx_dep_bigslip").set({
      userId: ALICE_UID,
      reference: "TXN-BIGSLIP",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 10000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      slipImageBase64: "A".repeat(360000),
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Cross-user isolation: Bob cannot read Alice's transactions or notifications", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("transactions").doc("alice_tx").set({
      userId: ALICE_UID,
      reference: "TXN-ALICE1",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 10000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      title: "Request Submitted",
      message: "TXN-ALICE1 submitted",
      type: "SUBMITTED",
      transactionRef: "TXN-ALICE1",
      read: false,
      createdAt: now,
    });
  });

  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bob.collection("transactions").doc("alice_tx").get());
  await assertFails(
    bob.collection("transactions").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("notifications").get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({
      read: true,
    })
  );
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("transactions").doc("alice_tx").set({
      userId: ALICE_UID,
      reference: "TXN-ALICE1",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 10000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      createdAt: now,
      updatedAt: now,
    });
  });

  const spoofed = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofed.collection("transactions").get());
  await assertFails(
    spoofed.collection("transactions").doc("alice_tx").update({
      status: "COMPLETED",
      agentNote: "Spoofed",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Verified Admin can list all transactions, complete request, and notify player", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("transactions").doc("alice_tx").set({
      userId: ALICE_UID,
      reference: "TXN-ALICE1",
      type: "DEPOSIT",
      status: "PENDING",
      amount: 25000,
      playerId: "184920412",
      paymentMethod: "Bank transfer",
      createdAt: created,
      updatedAt: created,
    });
  });

  const admin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(admin.collection("transactions").get());
  await assertSucceeds(
    admin.collection("transactions").doc("alice_tx").update({
      status: "COMPLETED",
      agentNote: "MobCash deposit credited to 1xBet",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin cannot mutate immutable amount or userId
  await assertFails(
    admin.collection("transactions").doc("alice_tx").update({
      amount: 999999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin creates player notification
  await assertSucceeds(
    admin.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_complete").set({
      title: "Deposit Credited via MobCash",
      message: "TXN-ALICE1 · LKR 25,000 credited to 1xBet",
      type: "COMPLETED",
      transactionRef: "TXN-ALICE1",
      read: false,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can list and mark her notification as read
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("notifications").get()
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_complete").update({
      read: true,
    })
  );
  // Alice cannot spoof COMPLETED notification directly
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_spoof").set({
      title: "Fake Complete",
      message: "Fake",
      type: "COMPLETED",
      read: false,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Player can manage own profile and read shared app_config", async () => {
  const created = new Date(Date.now() - 5000);
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Kamal Perera",
      playerId: "184920412",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      name: "Kamal S.",
      playerId: "184920412",
      savedPlayerIds: ["184920412", "1071114543"],
      savedBankPresets: [
        {
          id: "preset_1",
          bank: "Sampath Bank",
          accountNumber: "105456146706",
          contactNumber: "0771234567",
        },
      ],
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject oversized savedPlayerIds list (> 10 entries)
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      savedPlayerIds: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const admin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    admin.collection("app_config").doc("settings").set({
      whatsappNumber: "+94765865387",
      minTransaction: 1000,
      maxTransaction: 500000,
      promoCode: "VGSL",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(alice.collection("app_config").doc("settings").get());
  await assertFails(
    alice.collection("app_config").doc("settings").set({
      whatsappNumber: "+94700000000",
      minTransaction: 1000,
      maxTransaction: 500000,
      promoCode: "HACK",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
