# FinTracker

A modern Android application for personal and shared budget management. FinTracker helps individuals and groups track income and expenses, set spending limits, and gain clear insights into their financial habits through intuitive data visualization.

---

## Key Features

- **User Authentication** — Secure registration and login for every user.
- **Account and Category Management** — Create and manage multiple financial accounts and customizable tags/categories for transactions.
- **Manual Transaction Logging** — Record income and expense transactions manually with full offline support; data is synchronized once connectivity is restored.
- **Shared Accounts with Role-Based Access Control** — Invite other users to a shared account. Admins can configure spending limits and manage members; standard Users can record spending against the shared budget.
- **Customizable Spending Limits and Notifications** — Define category-level or account-level spending limits and receive in-app notifications when those limits are approached or exceeded.
- **Advanced Statistics, Filtering, and Data Visualization** — Explore spending patterns through filterable transaction lists, pie charts, and trend graphs.
- **Automatic Expense Tracking via Bank Push Notifications** — The application can securely read and parse push notifications delivered by banking apps. Recognized transactions are extracted and logged automatically, eliminating manual data entry for common bank operations.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile platform | Android SDK (API 26+) |
| Programming language | Java |
| Local persistence | Room Persistence Library (SQLite) |
| Remote data and authentication | Firebase Realtime Database and Firebase Authentication |
| Charts and graphs | MPAndroidChart |

---

## Architecture

The project follows a strict three-tier architecture:

1. **UI Layer** — Activities, Fragments, and XML layouts responsible solely for presenting data and capturing user input.
2. **Business Logic Layer (BLL)** — ViewModels, use cases, and service classes that encapsulate all application rules, validations, and workflows.
3. **Data Access Layer (DAL)** — Room DAOs, Firebase repository implementations, and data-transfer objects that abstract all persistence and network operations from the rest of the application.

---

## Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
- Android device or emulator running Android 8.0 (API 26) or higher
- A Firebase project configured with Realtime Database and Authentication enabled

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Kredle/FinTracker.git
   ```
2. Open the project in Android Studio via **File > Open** and select the cloned directory.
3. Add your `google-services.json` file (downloaded from your Firebase project console) to the `app/` directory.
4. Sync the project with Gradle and run it on your device or emulator.

---

## Team

| Name | Role |
|---|---|
| TBD | TBD |
