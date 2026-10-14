# UTOMO Bank - Back-Office System & Operations Setup Guide

This document outlines the architecture, setup procedure, role-based access control (RBAC), and deployment instructions for the **UTOMO Bank Production Back-Office System**.

---

## 1. Architecture Overview: Spark (Layer 1) vs. Blaze (Layer 2)

The system is deliberately partitioned into two operational layers to allow full functional deployment on **Firebase Spark (Free Tier)**, with seamless expansion into **Firebase Blaze (Pay-as-you-go)**.

| Component | Layer 1: Spark (Free Tier) | Layer 2: Blaze (Expansion) |
|---|---|---|
| **Web Admin Panel** | Single Page Application (HTML/CSS/ES Modules) hosted on Firebase Hosting or static hosting | Same SPA, communicates via callable Cloud Functions |
| **Authentication & RBAC** | Firebase Auth with Custom Claims (`superadmin`, `support_agent`, `card_operator`, `viewer`) provisioned via Node.js Admin CLI script | Same claims + callable `setStaffRole` Cloud Function |
| **Database & Security** | Cloud Firestore with strict RBAC rules, path isolation, staff-only `internalNotes`, masked card queries | Same rules + automated server-side audit logs & triggers |
| **Card Status Enforcement** | Real-time Firestore document updates on customer cards; client interceptor in mobile banking app | Trigger-based notification creation & webhooks |
| **Maintenance Mode** | Firestore `system_config/maintenance` document + live snapshot listener in Android app | Identity Platform Auth blocking functions (`beforeUserSignedIn`) |
| **Customer Support Tickets** | Bidirectional Firestore sync between web panel and Android `SupportTicketsActivity` | FCM trigger push notifications (`onTicketMessageCreated`) |
| **Account Suspension** | Status field `users/{uid}.status` enforced by Firestore security rules and Android lifecycle | Auth session token revocation (`admin.auth().revokeRefreshTokens(uid)`) |

---

## 2. Directory Structure

```
BankAppUtomo/
├── admin-panel/                   # Web Back-Office Single Page Application (SPA)
│   ├── index.html                 # Complete 9-page SPA shell & modal dialogs
│   ├── styles.css                 # Enterprise Cobalt & Slate banking design system
│   ├── icons.js                   # MIT-licensed SVG vector icons (Zero emoji policy)
│   ├── firebase-config.js         # Firebase Web SDK v10 configuration
│   └── app.js                     # Full reactive application logic, RBAC & page renderers
│
├── firestore.rules                # Production security rules enforcing least-privilege RBAC
├── firestore.indexes.json         # Composite indexes for tickets, audit logs, and queries
├── firebase.json                  # Firebase CLI configuration for rules, hosting, and functions
│
├── scripts/                       # Layer 1 Staff Provisioning & Administrative Tools
│   ├── package.json               # Node.js dependencies (firebase-admin)
│   └── set-staff-role.js          # CLI script to grant roles and set Custom Claims
│
├── functions/                     # Layer 2 Cloud Functions (Node.js 18 Blaze Tier)
│   ├── package.json
│   └── index.js                   # Callable functions, Auth blocking triggers, FCM push triggers
│
├── app/                           # Native Android Mobile Application
│   ├── src/main/java/.../
│   │   ├── SupportTicketsActivity.kt     # Customer tickets inbox & queue filter
│   │   ├── TicketDetailActivity.kt       # Live customer-staff chat & 1-5 star rating
│   │   ├── MaintenanceActivity.kt        # System maintenance screen with auto-retry
│   │   ├── SuspendedAccountActivity.kt   # Security lockout screen with hotline routing
│   │   ├── util/BackOfficeRepository.kt  # Centralized real-time Firestore listeners & actions
│   │   └── util/CardSecurityHelper.kt    # Card lock/freeze transaction interceptor
│   └── src/main/res/
│       ├── layout/activity_support_tickets.xml
│       ├── layout/activity_ticket_detail.xml
│       ├── layout/activity_maintenance.xml
│       ├── layout/activity_suspended_account.xml
│       ├── layout/bottom_sheet_new_ticket.xml
│       ├── layout/bottom_sheet_ticket_rating.xml
│       └── layout/bottom_sheet_card_locked_error.xml
│
├── BACKOFFICE_SETUP.md            # This operations and setup guide
└── ASSETS_LICENSES.md             # Vector icon licenses and provenance
```

---

## 3. Prerequisites

1. **Node.js** (v18 or higher recommended) and **npm**
2. **Firebase CLI** installed globally:
   ```bash
   npm install -g firebase-tools
   ```
3. **Android Studio** with Android SDK 34 / 35 and Java 17
4. A Firebase Project (e.g., `utomobank-ab445`) with:
   - **Firebase Authentication** enabled (Email/Password)
   - **Cloud Firestore** created in Production mode

---

## 4. Step-by-Step Setup Instructions

### Part 1: Provisioning the First Superadmin (Layer 1 Spark Tier)

Because Firebase client SDKs cannot assign Custom Claims to users directly, the initial administrator must be provisioned using the server-side Admin SDK script.

1. Go to the [Firebase Console](https://console.firebase.google.com/) -> Select your project -> **Project Settings** -> **Service accounts**.
2. Click **Generate new private key** and download the `.json` file.
3. Save the downloaded file as `scripts/serviceAccountKey.json` (this file is pre-configured in `.gitignore` and must **never** be committed to version control).
4. Install dependencies:
   ```bash
   cd scripts
   npm install
   ```
5. Create a user account in Firebase Console -> **Authentication** -> **Add user** (or sign up via the app).
6. Run the provisioning script with the user's UID:
   ```bash
   node set-staff-role.js <USER_UID> superadmin "Jane Doe"
   ```
7. Verify output:
   ```
   [SUCCESS] Successfully assigned role 'superadmin' to Jane Doe (UID: <USER_UID>).
   Custom claims set: { role: 'superadmin', staff: true }
   ```

### Part 2: Deploying Firestore Security Rules & Indexes

Deploy the production rules and composite indexes to ensure data security and performance:

1. Log in to Firebase CLI:
   ```bash
   firebase login
   ```
2. Select your Firebase project:
   ```bash
   firebase use utomobank-ab445
   ```
3. Deploy Firestore rules and indexes:
   ```bash
   firebase deploy --only firestore:rules,firestore:indexes
   ```

### Part 3: Deploying and Running the Web Admin Panel

#### Option A: Local Testing / Intranet Static Server
Because the web panel is built as a pure Vanilla ES Module single-page application, no bundling or build step is required:
```bash
# From the project root on Windows (recommended)
start-admin-panel.bat

# Using Python
cd admin-panel
python -m http.server 8080

# Or using npx serve
npx serve admin-panel
```
Open `http://localhost:8080` in Chrome, Edge, or Firefox.

The Windows launcher opens `http://localhost:8181/index.html` when that port is available. If Laragon/Apache already owns port 8181, it automatically selects the next available port and opens the correct Admin Panel URL instead of the Laragon document root.

#### Option B: Deploying to Firebase Hosting
```bash
firebase deploy --only hosting
```
The panel will be live at `https://utomobank-ab445.web.app`.

### Part 4: Deploying Layer 2 Cloud Functions (Optional - Blaze Tier)

When the Firebase project is upgraded to the Blaze tier:
1. Navigate to `functions/`:
   ```bash
   cd functions
   npm install
   ```
2. Deploy the functions:
   ```bash
   firebase deploy --only functions
   ```
This activates:
- Server-side callable role modification (`setStaffRole`)
- Server-side account token revocation (`suspendUser`, `reactivateUser`)
- Auth blocking functions (`beforeUserSignedIn`, `beforeUserCreated`)
- Automated FCM push notifications on ticket replies and card status changes

---

## 5. Role & Permissions Matrix (RBAC)

| Permission / Action | superadmin | support_agent | card_operator | viewer | Customer (Mobile) |
|---|:---:|:---:|:---:|:---:|:---:|
| **Dashboard KPIs & Charts** | Read | Read | Read | Read | No Access |
| **Support Tickets Queue** | Read / Write | Read / Write | Read Only | Read Only | Own tickets only |
| **Ticket Canned Responses** | Manage / Use | Use Only | Use Only | Read Only | No Access |
| **Ticket Internal Notes** | Read / Write | Read / Write | Read Only | Read Only | No Access (Hidden) |
| **Customer Lookup & Profile** | Full Detail | Full Detail | Full Detail | Read Only | Own profile only |
| **Account Suspension** | Yes | Yes (w/ reason) | No | No | No Access |
| **Card Freeze / Block** | Yes | No | Yes (w/ reason) | No | Freeze own card |
| **Card Unblock** | Yes | No | Yes | No | No Access (Staff only) |
| **Transaction Explorer** | Read / Flag | Read / Flag | Read / Flag | Read Only | Own transactions only |
| **System Maintenance Mode** | Read / Write | Read Only | Read Only | Read Only | Read Only (Banner) |
| **Staff & Role Management** | Read / Write | No Access | No Access | No Access | No Access |
| **Audit Trail Logs** | Read Only | Read Only | Read Only | Read Only | No Access |

*Note: All audit trail logs are strictly immutable and append-only across all roles.*

---

## 6. Security & Data Protection Policies

1. **Card PAN & Account Number Masking**:
   - Back-office UI displays only masked card numbers (e.g., `•••• •••• •••• 1234`).
   - Customer account numbers are masked to the last 4 digits (e.g., `••••• 5678`).
   - Raw CVV and Card PIN are never transmitted or stored in Firestore.
2. **Session Security & Inactivity Timeout**:
   - The admin panel actively tracks user keyboard and mouse activity.
   - If no interaction occurs for 30 minutes, the session is invalidated and the user is safely logged out.
3. **Append-Only Immutable Audit Trail**:
   - Every administrative state change (freezing a card, changing a ticket status, modifying maintenance mode, suspending a customer) creates an audit log entry in the `audit_logs` collection.
   - Security rules prohibit updating or deleting documents in `audit_logs`.
4. **Zero Emoji Policy**:
   - In compliance with enterprise design standards, all icons across both the web panel and native mobile apps are clean SVG vector drawables (licensed under MIT/Apache 2.0).
5. **Bilingual Support**:
   - Both the web panel and Android client provide comprehensive Indonesian and English localization. Canned support responses are automatically suggested in the customer's preferred language.

---

## 7. Manual End-to-End Verification Checklist

| Test Case | Steps | Expected Result | Verified |
|---|---|---|:---:|
| **1. Staff Login & Claims** | Log in to `admin-panel/` with superadmin account | Dashboard loads with all 9 navigation tabs; user pill displays "superadmin". | [x] |
| **2. Session Inactivity** | Leave panel inactive | Countdown timer reaches 0; confirmation prompt locks session and returns to login. | [x] |
| **3. Customer Support Ticket** | On mobile app: open Support, create new ticket with Category "Masalah Kartu" | Ticket appears immediately on Back-Office Tickets queue. | [x] |
| **4. Ticket Bidirectional Chat** | Back-office agent replies using canned response or manual message | Mobile app receives real-time chat bubble and in-app notification. | [x] |
| **5. Ticket Closure & Rating** | Customer or agent marks ticket as Resolved | Mobile app displays 1-5 star rating sheet; submitted rating syncs to Back-Office. | [x] |
| **6. Card Block Enforcement** | Back-office card operator blocks card with reason code `STOLEN` | Home card deck removes blocked card; transaction attempts trigger custom locked bottom sheet. | [x] |
| **7. Card Unblock & Restore** | Card operator unblocks card with audit reason | Card returns to active state on mobile app; transactions succeed. | [x] |
| **8. Maintenance Mode Lockout** | Back-office toggles Maintenance Mode ON | Mobile app immediately navigates to `MaintenanceActivity` with live countdown; retries check status. | [x] |
| **9. Maintenance Mode Lift** | Back-office toggles Maintenance Mode OFF | Mobile app dismisses maintenance screen and returns to active banking dashboard. | [x] |
| **10. Account Suspension** | Back-office marks user as suspended with reason `AML_CHECK` | Mobile app immediately displays `SuspendedAccountActivity` blocking all banking operations. | [x] |
| **11. Audit Trail Integrity** | Review `audit_logs` table in Back-Office | Every action performed in steps 4, 6, 7, 8, 10 appears with timestamp, staff UID, and details. | [x] |

---

## 8. Support & Troubleshooting

- **Firestore Permission Denied in Web Panel**: Ensure custom claims have been set via `scripts/set-staff-role.js` and that the staff user signed out and signed back in to refresh their Firebase ID Token.
- **Rules Compilation**: Validate rules syntax using `firebase deploy --only firestore:rules`.
- **Composite Index Errors**: Click the auto-generated creation URL in the Firebase console log or deploy `firestore.indexes.json`.
