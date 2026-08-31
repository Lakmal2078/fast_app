# Project TODO

- [x] Import and adapt the supplied Fast Cash application into the managed website project
- [x] Build responsive cyberpunk public homepage and navigation
- [x] Add clear deposit, withdrawal, tracking, support, privacy, and responsible-gambling content
- [x] Add Sinhala, English, and Tamil interface support
- [x] Add authenticated customer profile with saved Player ID details
- [x] Add deposit and withdrawal request forms with validation and unique references
- [x] Add protected receipt upload references without storing file bytes in transaction rows
- [x] Add customer transaction history with status timeline and rejection reasons
- [x] Add in-app notification centre with unread indicators
- [x] Send owner alert on each new deposit or withdrawal request
- [x] Build admin operations dashboard with request search and filters
- [x] Add admin status updates and required rejection reasons
- [x] Add configurable agent bank/wallet details and transaction limits
- [x] Add configurable WhatsApp support contact and promotional content
- [x] Add Vitest coverage for request, notification, and admin workflows
- [x] Run typecheck, lint, test, and production build checks
- [x] Verify desktop and mobile responsive presentation
- [x] Save final checkpoint and deliver project version

## Follow-up validation items

- [x] Replace remaining hardcoded form/admin/support strings with complete Sinhala, English, and Tamil translation coverage
- [x] Add persistent agent account, transaction-limit, WhatsApp, and promotion configuration procedures/UI
- [x] Add workflow-level tests for protected upload, customer history, notification persistence, and admin status updates
- [x] Add a production admin authentication journey test before launch

## Bank/API reconciliation and automatic verification

- [ ] Define provider-neutral bank transaction import and webhook contract
- [ ] Add reconciliation records, provider transactions, idempotency keys, and verification status schema
- [ ] Add secure provider adapter with signature verification and secrets-based configuration
- [ ] Add automatic matching by amount, reference, account, and time window
- [ ] Add verification states and manual-review fallback for unmatched or ambiguous payments
- [ ] Add admin reconciliation queue, match details, and manual override UI
- [ ] Add owner alerts for failed/ambiguous verification and reconciliation exceptions
- [ ] Add tests for idempotency, signature validation, matching, and admin authorization
- [ ] Run typecheck, lint, tests, build, and responsive verification
- [ ] Save and deliver reconciliation feature checkpoint

## Option B — secure statement upload providers

- [ ] Add BOC, People's Bank, LOLC, Sampath Bank, and iPay provider selection
- [ ] Add secure CSV/Excel statement upload with protected file references
- [ ] Parse provider-normalized date, amount, debit/credit, reference, account, and description fields
- [ ] Add statement batch and row idempotency records
- [ ] Match statement rows to customer requests using reference, amount, provider account, and time window
- [ ] Add VERIFIED, UNMATCHED, AMBIGUOUS, and EXCEPTION reconciliation states
- [ ] Add admin reconciliation queue with manual link/ignore actions and audit notes
- [ ] Add import summary, duplicate detection, and owner exception alerts
- [ ] Add unit tests for all supported providers, malformed files, duplicate batches, and matching rules
- [ ] Run final quality checks and save a new feature checkpoint
