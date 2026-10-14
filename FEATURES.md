# Feature Status

This is the comprehensive implementation status of UTOMO Bank mobile app and back-office operations suite.

| Feature | Status | Notes |
|---|---|---|
| Splash screen | WORKING | Brand splash preserved with navigation handlers. |
| Sign in and sign up fields | WORKING | Full validation, password reveal toggles, and biometric login integration. |
| Onboarding | SIMULATED | Local ViewPager flow with complete slide graphics; cloud-driven dynamic onboarding slides are simulated. |
| Home navigation & Quick Actions | WORKING | Nine quick-action menu entries navigate to their respective banking activities. |
| Card deck & variants | WORKING | Dynamic swipeable Home/My Cards deck with page dots, scale/fade motion, haptic feedback, empty state, real-time Firestore synchronization, and automatic filtering of staff-blocked cards. |
| Card controls & PIN | WORKING | Online transactions toggle, customer freeze state, custom PIN bottom sheet, and back-office staff lock banner with support ticket launcher. |
| Card Security Interceptor | WORKING | Real-time security verification across all transaction pathways (Transfer, Bill Payment, Mobile Prepaid, Cash Withdraw, QRIS) preventing usage of frozen or staff-blocked cards with custom bottom sheet dialogs. |
| Transfers | SIMULATED | Complete validation, account lookup simulation, balance calculation, receipt generation, and security gate; external inter-bank settlement network (BI-FAST/RTGS) is simulated locally. |
| Bill payment | SIMULATED | Multi-biller categories (Electricity, Water, Internet, BPJS), inquiry simulation, balance deduction, and receipt sharing; live biller gateway is simulated. |
| Mobile prepaid | SIMULATED | Telco carrier detection, denomination selection, balance deduction, and receipt sharing; live telecom aggregator is simulated. |
| Withdraw (Cardless ATM) | SIMULATED | Nominal selection, 6-digit withdrawal token generation, countdown timer, and ATM locator; physical ATM cash dispenser network is simulated. |
| QRIS payments | SIMULATED | Camera scanner, gallery import, custom nominal keypad, transaction verification, and receipt generation; national merchant settlement switch is simulated. |
| Receive money & QRIS generator | SIMULATED | Dynamic personal QR code generation, account sharing, and customizable incoming request amounts; external payment settlement is simulated. |
| History and statistics | WORKING | Real-time balance calculations, categorized income/expense aggregations, date filtering, search by reference, and visual charts. |
| Savings Goals | WORKING | Full CRUD operations (create, edit, delete with automatic refund of accrued balance back to primary account), progress bars, and monthly spending budget controls. |
| Profile, language and dark mode | WORKING | Instant bilingual English/Indonesian switching, system and manual dark mode themes, user preferences caching, and logout session handling. |
| Support Tickets (Mobile) | WORKING | Real-time customer support ticket management, 7 category spinners, bidirectional chat messaging with staff, status updates (Open, In Progress, Resolved, Closed), and 1-5 star service rating sheet (zero emoji). |
| In-App Support Notifications | WORKING | Real-time listener for back-office ticket replies, card security state updates, and account notices with unread indicators and deep linking. |
| System Maintenance Screen | WORKING | Real-time Firestore snapshot listener intercepting app lifecycle when maintenance mode is active, displaying countdown timer, localized announcements, and auto-retry verification. |
| Suspended Account Screen | WORKING | Security enforcement screen locking banking access for suspended accounts with direct customer care hotline routing. |
| Back-Office Web Admin Panel | WORKING | Standalone responsive Single Page Application (HTML5, Vanilla CSS, ES Modules, modular Firebase Web SDK v10) with 9 complete management modules (Dashboard, Tickets Queue, Customers, Cards, Transactions Explorer, Maintenance Mode, Staff & Roles, Canned Responses, Audit Trail). |
| Role-Based Access Control (RBAC) | WORKING | Least-privilege role verification (`superadmin`, `support_agent`, `card_operator`, `viewer`) enforced via Firebase Auth Custom Claims, Firestore security rules, and web panel UI guards. |
| Firestore Security Rules & Indexes | WORKING | Production rules enforcing data isolation, immutable append-only audit trail, customer read boundaries, and staff-only internal notes. |
| Staff Provisioning Script (CLI) | WORKING | Node.js administrative script (`scripts/set-staff-role.js`) using `firebase-admin` to provision staff roles and custom claims on the Firebase Spark Free Tier. |
| Cloud Functions (Blaze Tier) | SIMULATED | Full codebase implemented in `functions/` (role management, token revocation, Auth blocking, FCM push notification triggers); simulated in Spark mode via Layer 1 Firestore rules and scripts, ready for immediate deployment on Blaze upgrade. |
