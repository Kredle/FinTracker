# FinTracker - Personal Finance Android App

A modern Android application for personal and shared budget management. FinTracker helps individuals and groups track income and expenses, set spending limits, and gain clear insights into their financial habits.

---

## 🚀 Quick Start

### Running Tests

The easiest way to verify everything works:

```powershell
# Make sure a device/emulator is running (check Android Studio → Device Manager)
# Then run all tests
.\run-tests.ps1
```

**New to testing?** → Read **[HOW_TO_TEST.md](HOW_TO_TEST.md)** for a complete guide

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Testing](#-testing)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

---

## ✨ Features

- **User Authentication** — Secure registration and login
- **Account Management** — Create and manage multiple financial accounts (Card, Cash, etc.)
- **Tag/Category Management** — Organize expenses with custom tags
- **Transaction Tracking** — Record income and expense transactions
- **Spending Limits** — Set and monitor spending limits per account or category
- **Shared Accounts** — Collaborate on budgets with role-based access control
- **Data Validation** — Comprehensive input validation at the business logic layer
- **Offline-First** — Local Room database with sync capability

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|------------|
| **Platform** | Android SDK (API 24+) |
| **Language** | Java 11 |
| **Database** | Room Persistence Library (SQLite) |
| **Testing** | JUnit 4, AndroidX Test |
| **Build System** | Gradle 9.2.1 |
| **IDE** | Android Studio |

**Planned:**
- Firebase Realtime Database (cloud sync)
- Firebase Authentication
- MPAndroidChart (data visualization)

---

## 🏗 Architecture

The project follows a **clean three-tier architecture**:

```
┌─────────────────────────────────────────┐
│         UI Layer (Activities)           │
│  LoginActivity, MainActivity, etc.      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│   Business Logic Layer (BLL)            │
│  - Validators (User, Account, Tag)      │
│  - ViewModels (planned)                 │
│  - Use Cases (planned)                  │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│   Data Access Layer (DAL)               │
│  - Room Database (AppDatabase)          │
│  - DAOs (UserDao, AccountDao, TagDao)   │
│  - Entities (UserEntity, etc.)          │
└─────────────────────────────────────────┘
```

**Key Principles:**
- ✅ Separation of concerns
- ✅ Single Responsibility Principle
- ✅ Business logic isolated from UI
- ✅ Data layer abstracted from business logic

---

## 🎯 Getting Started

### Prerequisites

- **Android Studio** (latest stable version)
- **JDK 11+** (bundled with Android Studio)
- **Android device** or **emulator** (API 24+)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/YourUsername/FinTracker.git
   cd FinTracker
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - File → Open
   - Select the `FinTracker` folder

3. **Sync Gradle**
   - Android Studio will automatically sync
   - Or click: File → Sync Project with Gradle Files

4. **Run the app**
   - Connect a device or start an emulator
   - Click the green **Run** button (▶)

---

## 🧪 Testing

We have comprehensive instrumented tests covering:
- ✅ User authentication & validation
- ✅ Account creation & management
- ✅ Tag creation & management
- ✅ Data validation (NaN, Infinity, negative values)
- ✅ Soft-delete protection
- ✅ Idempotent operations

### Run Tests

**Option 1: Helper Script (Recommended)**
```powershell
.\run-tests.ps1
```

**Option 2: Android Studio**
1. Open `app/src/androidTest/java/com/example/fintracker/AppDatabaseTest.java`
2. Right-click → **Run 'AppDatabaseTest'**

**Option 3: Gradle Wrapper**
```powershell
.\gradlew.ps1 connectedAndroidTest
```

### Test Coverage

Current test suite includes **15+ tests**:

| Category | Tests |
|----------|-------|
| User Authentication | 5 tests |
| Account Management | 6 tests |
| Tag Management | 4 tests |

### View Test Reports

After running tests, open the HTML report:
```
app\build\reports\androidTests\connected\index.html
```

**📚 Testing Guides:**
- **[HOW_TO_TEST.md](HOW_TO_TEST.md)** - Complete testing guide for developers
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Quick reference for common tasks
- **[RUNNING_TESTS.md](RUNNING_TESTS.md)** - Detailed documentation

---

## 📁 Project Structure

```
FinTracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/fintracker/
│   │   │   │   ├── bll/validators/          # Business logic validators
│   │   │   │   │   ├── UserValidator.java
│   │   │   │   │   ├── AccountValidator.java
│   │   │   │   │   └── TagValidator.java
│   │   │   │   ├── dal/local/               # Data access layer
│   │   │   │   │   ├── dao/                 # Database DAOs
│   │   │   │   │   │   ├── UserDao.java
│   │   │   │   │   │   ├── AccountDao.java
│   │   │   │   │   │   └── TagDao.java
│   │   │   │   │   ├── entities/            # Room entities
│   │   │   │   │   │   ├── UserEntity.java
│   │   │   │   │   │   ├── AccountEntity.java
│   │   │   │   │   │   ├── TagEntity.java
│   │   │   │   │   │   ├── TransactionEntity.java
│   │   │   │   │   │   ├── LimitEntity.java
│   │   │   │   │   │   └── SharedAccountMemberEntity.java
│   │   │   │   │   └── database/
│   │   │   │   │       └── AppDatabase.java # Room database
│   │   │   │   └── ui/activities/           # UI layer
│   │   │   │       └── LoginActivity.java
│   │   │   └── res/                         # Resources (layouts, etc.)
│   │   ├── androidTest/                     # Instrumented tests
│   │   │   └── java/com/example/fintracker/
│   │   │       └── AppDatabaseTest.java     # Database tests
│   │   └── test/                            # Unit tests (planned)
│   ├── build.gradle.kts                     # App build configuration
│   └── schemas/                             # Room database schemas
├── gradle/                                  # Gradle wrapper
├── build.gradle.kts                         # Project build config
├── settings.gradle.kts                      # Gradle settings
├── run-tests.ps1                            # Test runner script
├── gradlew.ps1                              # Gradle wrapper script
├── HOW_TO_TEST.md                           # Testing guide
├── QUICK_REFERENCE.md                       # Quick reference
├── RUNNING_TESTS.md                         # Test documentation
└── REFACTORING_SUMMARY.md                   # Recent changes log
```

---

## 🎨 Database Schema

### Current Entities (6 tables)

1. **users** - User accounts with authentication
2. **accounts** - Financial accounts (Card, Cash, etc.)
3. **tags** - Expense categories/tags
4. **transactions** - Income and expense records
5. **limits** - Spending limits
6. **shared_account_members** - Shared account permissions

**Database Version:** 2 (with AutoMigration from v1)

**Schema Export Location:** `app/schemas/`

---

## 🤝 Contributing

### Development Workflow

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Follow existing code style
   - Add validators for new business logic
   - Update DAOs for new database operations

3. **Write tests**
   - Add tests to `AppDatabaseTest.java`
   - Ensure all tests pass: `.\run-tests.ps1`

4. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: Add your feature description"
   ```

5. **Push and create a Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```

### Code Style Guidelines

- **Validators:** Place in `bll/validators/`, use static methods
- **DAOs:** Place in `dal/local/dao/`, use Room annotations
- **Entities:** Place in `dal/local/entities/`, keep fields public
- **Tests:** Add to `AppDatabaseTest.java`, use descriptive names
- **Documentation:** Update relevant .md files

### Before Submitting PR

✅ All tests pass (`.\run-tests.ps1`)  
✅ No compiler warnings in modified files  
✅ Code follows existing patterns  
✅ Added tests for new features  
✅ Updated documentation if needed

---

## 📝 Recent Changes

See **[REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)** for detailed changelog of recent architectural improvements.

**Highlights (March 6, 2026):**
- ✅ Fixed AccountValidator to reject NaN and Infinity
- ✅ Added soft-delete protection to AccountDao.updateAccountBalance()
- ✅ Created comprehensive test suite (15+ tests)
- ✅ Refactored LoginActivity (removed debug code)
- ✅ Added idempotent operation support
- ✅ Improved documentation

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🆘 Need Help?

- **Testing Issues?** → Read [HOW_TO_TEST.md](HOW_TO_TEST.md)
- **Quick Reference?** → See [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
- **Architecture Questions?** → Review code comments in validators and DAOs
- **Found a Bug?** → Open an issue on GitHub

---

## 🎯 Roadmap

- [ ] Implement Firebase sync
- [ ] Add data visualization (charts)
- [ ] Implement transaction filtering
- [ ] Add bank notification parsing
- [ ] Create comprehensive UI
- [ ] Add unit tests for validators
- [ ] Set up CI/CD pipeline
- [ ] Add performance monitoring

---

**Happy Coding! 🚀**

Made with ☕ by the FinTracker team


