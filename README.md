# Fast Cash Sri Lanka (Android)

Official agent operations bridge between 1xBet player wallets and Fast Cash agents in Sri Lanka. Native Android application built using Kotlin, Jetpack Compose, and Material 3.

---

## 🌟 Key Features

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
