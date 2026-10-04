import {
  users,
  transactions,
  transactionEvents,
  notifications,
  agentAccounts,
  appSettings,
  statementBatches,
  statementRows,
} from "../drizzle/schema";

function matchPredicate(row: any, predicate: any): boolean {
  if (!predicate) return true;
  if (predicate && Array.isArray(predicate.queryChunks)) {
    let colName: string | undefined;
    let expectedVal: any = undefined;
    for (const chunk of predicate.queryChunks) {
      if (chunk?.name) {
        colName = chunk.name;
      }
      if (chunk?.value !== undefined && chunk?.constructor?.name === "Param") {
        expectedVal = chunk.value;
      }
    }
    if (colName !== undefined) {
      return String(row[colName]) === String(expectedVal);
    }
  }
  return true;
}

function sortOrder(rows: any[], orderExpr: any): any[] {
  if (!orderExpr) return rows;
  let colName: string | undefined;
  let isDesc = true;
  if (orderExpr && Array.isArray(orderExpr.queryChunks)) {
    for (const chunk of orderExpr.queryChunks) {
      if (chunk?.name) colName = chunk.name;
      if (Array.isArray(chunk?.value) && chunk.value.some((v: any) => String(v).toLowerCase().includes("asc"))) {
        isDesc = false;
      }
    }
  }
  if (!colName) return rows;
  return [...rows].sort((a, b) => {
    const valA = a[colName!];
    const valB = b[colName!];
    if (valA === valB) return 0;
    if (valA === undefined || valA === null) return 1;
    if (valB === undefined || valB === null) return -1;
    if (valA instanceof Date && valB instanceof Date) {
      return isDesc ? valB.getTime() - valA.getTime() : valA.getTime() - valB.getTime();
    }
    return isDesc ? (valA < valB ? 1 : -1) : (valA > valB ? 1 : -1);
  });
}

export function createMockDb() {
  const usersStore: any[] = [
    {
      id: 1,
      openId: "demo-user-openid",
      name: "Demo Customer",
      email: "customer@fastcash.lk",
      loginMethod: "preview",
      playerId: "1029384",
      role: "admin",
      createdAt: new Date(),
      updatedAt: new Date(),
      lastSignedIn: new Date(),
    },
  ];

  const transactionsStore: any[] = [
    {
      id: 1,
      reference: "TXN-7F42A",
      userId: 1,
      type: "DEPOSIT",
      status: "PROCESSING",
      amount: "25000",
      playerId: "1029384",
      paymentMethod: "Bank transfer",
      bank: "Bank of Ceylon",
      accountNumber: "7128938100",
      contactNumber: "0765865387",
      receiptKey: null,
      rejectionReason: null,
      createdAt: new Date(Date.now() - 3600000),
      updatedAt: new Date(Date.now() - 3600000),
    },
    {
      id: 2,
      reference: "TXN-19AC2",
      userId: 1,
      type: "WITHDRAWAL",
      status: "COMPLETED",
      amount: "12500",
      playerId: "1029384",
      paymentMethod: "Bank payout",
      securityCode: "SEC-9812",
      bank: "Sampath Bank",
      accountNumber: "1002345678",
      contactNumber: "0765865387",
      receiptKey: null,
      rejectionReason: null,
      createdAt: new Date(Date.now() - 86400000),
      updatedAt: new Date(Date.now() - 86400000),
    },
  ];

  const transactionEventsStore: any[] = [
    {
      id: 1,
      transactionId: 1,
      status: "PENDING",
      note: "Request submitted",
      createdAt: new Date(Date.now() - 3600000),
    },
    {
      id: 2,
      transactionId: 1,
      status: "PROCESSING",
      note: "Agent assigned for verification",
      createdAt: new Date(Date.now() - 1800000),
    },
    {
      id: 3,
      transactionId: 2,
      status: "COMPLETED",
      note: "Payout dispatched to bank account",
      createdAt: new Date(Date.now() - 86400000),
    },
  ];

  const notificationsStore: any[] = [
    {
      id: 1,
      userId: 1,
      title: "Request received",
      message: "TXN-7F42A is now under review.",
      readAt: null,
      createdAt: new Date(Date.now() - 3600000),
    },
    {
      id: 2,
      userId: 1,
      title: "Withdrawal completed",
      message: "TXN-19AC2 has been settled to your account.",
      readAt: new Date(Date.now() - 80000000),
      createdAt: new Date(Date.now() - 86400000),
    },
  ];

  const agentAccountsStore: any[] = [
    {
      id: 1,
      label: "Bank of Ceylon (BOC) - Fast Cash Ops",
      accountNumber: "7128938100",
      method: "Bank transfer",
      active: 1,
      createdAt: new Date(),
    },
    {
      id: 2,
      label: "People's Bank - Central Branch",
      accountNumber: "1928374100",
      method: "Bank transfer",
      active: 1,
      createdAt: new Date(),
    },
    {
      id: 3,
      label: "iPay Fast Settlement Route",
      accountNumber: "0765865387",
      method: "iPay wallet",
      active: 1,
      createdAt: new Date(),
    },
  ];

  const appSettingsStore: any[] = [
    {
      id: 1,
      whatsappNumber: "+94765865387",
      minTransaction: 1000,
      maxTransaction: 500000,
      promoCode: "VGSL",
    },
  ];

  const statementBatchesStore: any[] = [];
  const statementRowsStore: any[] = [];

  function getStore(table: any): any[] {
    if (table === users || table?._?.name === "users") return usersStore;
    if (table === transactions || table?._?.name === "transactions") return transactionsStore;
    if (table === transactionEvents || table?._?.name === "transactionEvents") return transactionEventsStore;
    if (table === notifications || table?._?.name === "notifications") return notificationsStore;
    if (table === agentAccounts || table?._?.name === "agentAccounts") return agentAccountsStore;
    if (table === appSettings || table?._?.name === "appSettings") return appSettingsStore;
    if (table === statementBatches || table?._?.name === "statementBatches") return statementBatchesStore;
    if (table === statementRows || table?._?.name === "statementRows") return statementRowsStore;
    return [];
  }

  const mockDb = {
    select() {
      return {
        from(table: any) {
          const store = getStore(table);
          let current = [...store];

          const query = {
            where(predicate: any) {
              current = current.filter(row => matchPredicate(row, predicate));
              return query;
            },
            orderBy(orderExpr: any) {
              current = sortOrder(current, orderExpr);
              return query;
            },
            limit(count: number) {
              current = current.slice(0, count);
              return query;
            },
            then(resolve?: (val: any) => any, reject?: (err: any) => any) {
              const res = [...current];
              return Promise.resolve(res).then(resolve, reject);
            },
          };
          return query;
        },
      };
    },

    insert(table: any) {
      const store = getStore(table);
      return {
        values(data: any) {
          const items = Array.isArray(data) ? data : [data];
          const insertedIds: { id: number }[] = [];
          for (const item of items) {
            const nextId = store.length > 0 ? Math.max(...store.map((s: any) => s.id || 0)) + 1 : 1;
            const newItem = {
              id: item.id ?? nextId,
              createdAt: new Date(),
              updatedAt: new Date(),
              ...item,
            };
            store.push(newItem);
            insertedIds.push({ id: newItem.id });
          }

          const insertChain = {
            $returningId() {
              return Promise.resolve(insertedIds);
            },
            onDuplicateKeyUpdate({ set }: { set: Record<string, unknown> }) {
              const first = items[0];
              if (first?.openId) {
                const existing = store.find((s: any) => s.openId === first.openId);
                if (existing) {
                  Object.assign(existing, set, { updatedAt: new Date() });
                }
              }
              return Promise.resolve(insertedIds);
            },
            then(resolve?: (val: any) => any, reject?: (err: any) => any) {
              return Promise.resolve(insertedIds).then(resolve, reject);
            },
          };
          return insertChain;
        },
      };
    },

    update(table: any) {
      const store = getStore(table);
      return {
        set(updates: Record<string, any>) {
          let updatedCount = 0;
          const updateChain = {
            where(predicate: any) {
              for (const row of store) {
                if (matchPredicate(row, predicate)) {
                  Object.assign(row, updates, { updatedAt: new Date() });
                  updatedCount++;
                }
              }
              return updateChain;
            },
            then(resolve?: (val: any) => any, reject?: (err: any) => any) {
              return Promise.resolve({ affectedRows: updatedCount }).then(resolve, reject);
            },
          };
          return updateChain;
        },
      };
    },

    delete(table: any) {
      const store = getStore(table);
      let deletedCount = 0;
      let hasWhere = false;
      const deleteChain = {
        where(predicate: any) {
          hasWhere = true;
          for (let i = store.length - 1; i >= 0; i--) {
            if (matchPredicate(store[i], predicate)) {
              store.splice(i, 1);
              deletedCount++;
            }
          }
          return deleteChain;
        },
        then(resolve?: (val: any) => any, reject?: (err: any) => any) {
          if (!hasWhere && store.length > 0) {
            deletedCount = store.length;
            store.length = 0;
          }
          return Promise.resolve({ affectedRows: deletedCount }).then(resolve, reject);
        },
      };
      return deleteChain;
    },
  };

  return mockDb;
}
