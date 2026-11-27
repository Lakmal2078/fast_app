# Fast Cash Sri Lanka (Android)

Official agent operations bridge between 1xBet player wallets and Fast Cash agents in Sri Lanka. Native Android application built using Kotlin, Jetpack Compose, and Material 3.

---

## 🌟 Key Features

- **Customer access** — Customer keys identify the owner of each request. The app shows that customer's history and profile.
- **Deposit and withdrawal requests** — Submit LKR 1,000–500,000 and receive a reference after the server has saved the request. Both forms accept a screenshot for WhatsApp. After saving the request, open WhatsApp, verify the agent number +94765865387, and tap Send. The attachment goes directly to WhatsApp. Withdrawal security codes are excluded from the caption.
- **Tracking** — Search your references, see a status timeline, and review decisions.
- **Languages** — English and Sinhala transaction labels, with English, Sinhala, and Tamil home content.
- **Notifications** — View local confirmations for your signed-in account.
- **Agent tools** — Authorized agents publish or hide deposit bank accounts, look up the WhatsApp reference and confirm a manual MobCash deposit or bank payout. The customer sees the decision in Track and the notification bell.
- **Human Channel Support & Responsible Gaming**:
  - Direct WhatsApp link to official agents (`+94765865387`).
  - Responsible gambling disclosure (18+ rules) and privacy policy.

---

## 🏗️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose, Material 3, Dark Neon Cyberpunk Theme (`#00E0FF` Cyan, `#FF2097` Pink).
- **State Management**: Android `ViewModel`, Kotlin Coroutines, and `StateFlow`.
- **Local Persistence**: Android Room Database (`AppDatabase`, `TransactionDao`, `NotificationDao`, `SettingsDao`, `Converters`).
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`).
- **CI/CD Pipeline**: GitHub Actions with Automated Testing & Debug APK Build Artifacts.

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- Android Studio Ladybug | 2024.2+ or higher (or JDK 21)
- Android SDK Platform 35

### Building the Project
Clone the repository and run the Gradle build commands:

```bash
# Clone the repository
git clone https://github.com/Lakmal2078/Manus_web.git
cd Manus_web

# Run Unit Tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

The generated APK will be available in:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📁 Project Structure

```
Manus_web/
├── .github/workflows/
│   └── android.yml            # Automated CI build and APK artifact generation
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/fastcash/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── local/      # Room AppDatabase, DAOs, TypeConverters
│   │   │   │   ├── model/      # Entity data models & Enums
│   │   │   │   └── repository/ # FastCash Repository & initial seeding
│   │   │   └── ui/
│   │   │       ├── components/ # TopBar and common Cyberpunk UI widgets
│   │   │       ├── screens/    # Home, Deposit, Withdraw, Track, Admin, Support
│   │   │       ├── theme/      # Colors, Typography, Shapes
│   │   │       └── viewmodel/  # FastCashViewModel & navigation states
│   │   └── res/                # Drawables, mipmaps, colors, themes, strings
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml      # Dependency versions catalog
│   └── wrapper/                # Gradle wrapper configuration
├── build.gradle.kts
├── gradlew
└── settings.gradle.kts
```

---

## 🔒 Security & Responsible Gaming
Fast Cash LK is strictly intended for users 18 years of age and older. Always gamble responsibly.

## Customer transaction staging

The debug variant uses the isolated Fast Cash staging Worker. To use another HTTPS staging deployment, pass `-PfastcashBackendUrl=https://your-worker.example/` to Gradle. Release builds require the authenticated Worker and a separately configured production D1 binding before rollout.

For a fresh database, apply `backend/schema.sql` and then `backend/migrations/001_customer_ownership.sql` and then `002_agent_fulfilment.sql` exactly once. For an existing database with the original schema, apply only outstanding migrations in numeric order. Back up the database before upgrading. Existing transactions retain their records with no assigned customer. Verify ownership independently before assigning historical records. The Android app likewise leaves the old unscoped Room database intact and uses a separate cache for each authenticated customer and backend.

Each customer needs an independently issued 32-byte random access key encoded as 64 lowercase hex characters. In a trusted provisioning environment generate it with a cryptographic random generator. Insert its lowercase SHA-256 hex digest (hash the UTF-8 hex string) and a stable opaque customer ID into `customer_credentials(key_hash, customer_id)` using bound parameters. Deliver the original key privately to that customer. Revoke access by removing its credential row. Never use a player ID as proof of ownership. The Android sign-in keeps the key in memory and requires sign-in after process death. A financial reference is returned after D1 has committed the request. Retrying the same pending form reuses its request key.

From `backend/`, use `npm ci`, then `npx wrangler deploy --env staging` to deploy only staging. Test that deployment with `python3 tests/staging.py` using the `STAGING_URL`, `STAGING_ALICE`, and `STAGING_BOB` environment variables. Use dedicated synthetic staging customers. The test creates synthetic deposit and withdrawal records. Verified bank destinations must be provisioned separately. No payment processor is connected.

The Android staging workflow builds with JDK 21 and SDK 35 and uses API 35 phone and tablet emulators. Configure the repository secrets `FASTCASH_STAGING_PHONE` and `FASTCASH_STAGING_TABLET` with separate staging-only customer keys. Both device profiles run the Sinhala deposit-to-tracking test and Room collision regression.

Use a distinct agent credential with `is_agent=1` for the operator. Regular customer keys default to zero. Sign in as the agent and use Admin to publish the bank, account holder and account number. Only published active accounts appear on the customer Deposit screen. Selecting or copying one selects the bank transfer destination for that request. The account can be hidden for future deposits while historical references remain reviewable. Complete the MobCash or bank action manually before confirming in Admin. Final decisions are immutable and visible only to that player through tracking and in-app notifications. The app refreshes on returning to the foreground and every 30 seconds while open.
