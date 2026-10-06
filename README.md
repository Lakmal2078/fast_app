# Fast Cash Sri Lanka (Android)

Official agent operations bridge between 1xBet player wallets and Fast Cash agents in Sri Lanka. Re-architected as a native Android application using Kotlin, Jetpack Compose, and Material 3.

## Features

- **Multi-Language Support**: Switch seamlessly between English, Sinhala (සිංහල), and Tamil (தமிழ்).
- **Network Liquidity HUD**: Real-time network balance monitor with animated cyberpunk signal equalizer bars and active agent indicators.
- **Deposit Channel**:
  - Load player wallet with LKR 1,000 to 500,000 limits.
  - Active agent bank accounts (BOC, People's Bank, Sampath) with quick copy buttons.
  - Bank slip reference and receipt screenshot attachment.
  - Instant unique reference code generation (`TXN-XXXXXX`) and progress tracking.
- **Withdrawal Channel**:
  - Route funds from 1xBet player wallet to Sri Lankan bank accounts.
  - Verification using 1xBet Cash withdrawal security codes.
  - Instant tracking reference generation and payout queueing.
- **Real-Time Track & Trace**:
  - Look up any transaction by reference.
  - 3-stage visual interactive timeline (`SUBMITTED` -> `PROCESSING` -> `COMPLETED`).
  - Clear decision note explanation if a request is rejected.
- **Customer Profile**:
  - Save customer name and 1xBet Player ID to prefill future transactions.
- **Notification Centre**:
  - In-app notification drawer with unread counter badges and timeline status alerts.
- **Admin Control Room**:
  - Live queue monitoring with reference search and status filters (`ALL`, `PENDING`, `PROCESSING`, `COMPLETED`, `REJECTED`).
  - Request review with status updates and mandatory rejection decision notes.
  - Bank Statement Reconciliation engine matching bank statements (BOC, People's Bank, LOLC, Sampath, iPay) to pending customer requests.
  - Network settings control (WhatsApp number, limits, promo codes).
- **Human Channel Support & Responsible Gaming**:
  - Direct WhatsApp link to official agents (`+94765865387`).
  - Responsible gambling disclosure (18+ rules) and privacy policy.

## Architecture

- **UI**: Jetpack Compose, Material 3, Dark Neon Cyberpunk Theme (`#00E0FF` Cyan, `#FF2097` Pink).
- **State Management**: Android `ViewModel`, Kotlin Coroutines, and `StateFlow`.
- **Persistence**: Android Room Database (`AppDatabase`, `TransactionDao`, `NotificationDao`, `SettingsDao`).
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`).
