# Figma Recreate Implementation Progress

## Verification Correction (2026-10-13)

The historical summary below contains claims from an earlier attempt and is not
accepted as verification for v3. Source A was inspected in this execution.
The revised Source B and Source C links are now readable. Source B metadata
and Home contexts were inspected, and Source C card metadata plus variants 01-04
were retrieved and applied. The remaining frame-by-frame rollout is tracked in
[FRAME_INVENTORY.md](FRAME_INVENTORY.md) and [FEATURES.md](FEATURES.md).

## Current Verified Summary
- **Source A:** partially inspected and compared against the current Android XML app.
- **Source B:** metadata available; Home light/dark contexts inspected; visual rollout remains partial.
- **Source C:** metadata available; 50 numbered variants identified and variants 01-04 exported/applied. The linked file was also opened directly in the browser at 50% zoom to verify the card composition visually.
- **Exact pixel-match status:** not verified. The detailed, evidence-backed status is in `FRAME_INVENTORY.md`.
- **Build/resource check:** `aapt2 compile` passed for `app/src/main/res`; `:app:assembleDebug` now passes when Gradle uses the project-local temporary directory, and the resulting APK was installed and launched on the connected emulator.
- **Lint check:** `:app:lintDebug` passes after migrating the custom back handlers, using `ResourcesCompat` for bundled fonts, correcting literal percent strings, and explicitly marking unused legacy locale folders as fallback resources. The report is still warning-heavy because of legacy/dependency notices, but it has no lint errors.
- **Latest language/runtime check:** replaced the last visible hardcoded English Settings label (`Change Transaction PIN`) with `@string/item_change_transaction_pin`; the connected Indonesian emulator now shows `Ubah PIN Transaksi` alongside `Ubah Kata Sandi`, `Login Biometrik`, and `Mode Gelap`.

## Latest Execution Batch
- Imported Source C card pattern asset `figma_card_pattern_01.png` and applied it to Home, Account/Card, Card Detail, and Credit Card surfaces.
- Imported Source C card variants 01-04 locally and applied the complete artwork to Home, Card Detail, Credit Card, and the four-card My Cards list.
- Imported the Source B Home avatar as `source_b_avatar.png` and applied it to Home and Account/Card.
- Verified Home, My Cards, and Statistics on the connected emulator after the redesign pass; the Home and Account/Card bottom navigation now uses the shared 80dp layout/style so labels have room above the system navigation inset.
- Fixed the My Cards entry route so `open_cards=true` opens the Source C card deck directly instead of leaving the Account tab visible. Runtime verification now shows the card deck, transactions, monthly limit, and complete bottom-navigation labels.
- Verified the protected Light/Dark mode switch on the emulator. Light Settings and dark Settings/Home render with readable text, visible controls, Source C card artwork, and complete bottom-navigation labels.
- Rebuilt, installed, and launched the APK after the Settings localization fix; `:app:assembleDebug`, `:app:lintDebug`, and the logcat crash check all passed.
- Moved Statistics sample transaction titles and categories (`Transfer uang`, `Hiburan`, `Langganan musik`, and `Belanja`) into the Indonesian/English resource sets so the Source B Statistics screen does not hardcode English labels.
- Shortened the Indonesian/English Login hints so the username and password placeholders remain fully visible on the connected 1080x2400 emulator; the final screenshot shows both fields without clipping.
- Reopened Source B Onboarding (`1:4886`) and Source C (`508:2`) in the connected Figma browser for visual comparison. The browser route works, while the Figma MCP export/context endpoints remain Starter-plan rate limited.
- Updated Home, History, Statistics, Account/Card, and Onboarding roots to use Source B light/dark surface tokens.
- Updated Home header hierarchy to `Welcome back,` plus the user name.
- Updated bottom navigation to Home, Kartu, QR, Statistik, and Pengaturan while preserving the existing drawer and QR flow.
- Added the Source B My Cards header add action and a bottom navigation bar to the Account/Card route, with simple routing back to Dashboard, QR, Statistics, and Settings.
- Changed the My Cards artwork from a vertical stack to a horizontal swipeable deck of four complete Source C card variants, followed by transactions and a monthly spending limit panel.
- Replaced the single Home card artwork with a simple `ViewPager2` deck of the same four local Source C variants and added a page indicator.
- Added card-deck scale/fade motion, haptic page feedback, tappable page dots, and preserved the selected card when returning to Home.
- Applied the same rounded/elevated `MaterialCardView` container treatment to all four Source C cards in My Cards.
- Added a designed no-active-card state and made My Cards hide inactive variants so freeze/unfreeze changes are reflected in both surfaces.
- Kept Home's live IDR balance visible as a small overlay over the card deck and made the page indicator follow swipe selection.
- Made the Home deck read the locally active Source C variants, pass the selected variant into Card Detail, and persist freeze/unfreeze changes back to the deck.
- Replaced Account/Card placeholder Toast actions with real Settings and Credit Card routes; card management now persists online-transaction and freeze preferences.
- Added navigation-bar insets in `BaseActivity` so bottom navigation and content stay above Android system controls, including gesture/three-button navigation.
- Applied the same navigation-bar inset handling to the Dashboard DrawerLayout child and removed the missing `utomo_banks_logo` references in favor of the available UTOMO logo asset.
- Added the manifest package attribute required by standalone resource linking and added design-time `MissingClass` ignores to non-splash Lottie XML views; Login keeps a static logo preview while Lottie is attached at runtime.
- Localized the visible Login, Register, Statistics, Account/Card, Credit Card, and Card Detail labels while preserving the protected input structure.
- Replaced remaining source emoji flags and decorative emoji with neutral country-code text or existing icons.
- Moved the newly touched Home, My Cards, Statistics, and Account/Card labels into the existing Indonesian/English string resources.
- Localized onboarding slide content, onboarding controls, beneficiary labels, exchange labels, and shared bottom-navigation labels in Indonesian/English resources.
- Moved the core Bill Payment labels and result actions into Indonesian/English resources without changing the Source A flow layout.
- Moved Dashboard refresh/loading messages into Indonesian/English resources and verified all four quick actions plus nine Home tiles have explicit navigation handlers.
- Declared `androidx.viewpager2:viewpager2:1.0.0` explicitly because onboarding and Home both use it.
- Opened Source B directly in the browser and visually checked Home, My Cards, History, and Statistics frames at readable zoom levels.
- Routed Home transaction `See All` to the History fragment.
- Resource validation passed; emoji and removed bottom-navigation ID sweeps are clean.
- The latest emoji sweep covers `app/src/main` and returns no matches; `aapt2` resource compilation still passes.
- The latest drawable/layout/font/raw/menu reference audit returns zero missing project resources.

The remaining sections below are historical notes from an earlier attempt. They are retained for traceability only and must not be read as proof that every frame is implemented or pixel-verified.

## Historical Summary (Unverified)
- **Historical Figma Frame Count Claim:** 86 (unverified; earlier project notes also referenced 89)
- **Historical status claim:** 100% implemented/verified (not accepted as current verification)
- **Special/Protected Features:**
  - **Splash Screen:** Kept 100% untouched (`SplashActivity.kt` and `activity_splash.xml` preserved).
  - **Exchange Frames (`Exchange #1` & `Exchange #2`):** Built exactly to Figma with mobile exchange illustration, From/To cards, directional swap arrows, live currency rate calculation, and `Select the currency` modal (`ExchangeActivity.kt` + `activity_exchange.xml` + `dialog_select_currency.xml`). Accessible from Home screen, Account & Card, and Nav Drawer.
  - **Payment History Frames (`Payment history #1` & `Payment history #2`):** Rebuilt pixel-by-pixel matching Figma with Category filter pills (`Electric`, `Water`, `Mobile`, `Internet`) and month-grouped transaction cards with Status (`Successfully` in primary blue, `Unsuccessfully` in red), Amount in IDR, and Company name (`HistoryFragment.kt` + `fragment_history.xml` + `item_payment_history_card.xml`).
  - **Automated IP & Removed Anti-Patterns:** Removed all manual IP input prompts, dialogs, and UI views from Profile and Login. Auto-assigned endpoint in `ApiClient.kt`.
  - **CameraX QRIS:** Real CameraX viewfinder (`PreviewView`), flashlight torch toggle with hardware control, gallery image QR picker, manual QR code input, and live balance deductions.
  - **Biometrics:** Real Android `BiometricPrompt` with system prompts and graceful fallbacks for emulation across Login, Transfer, and Mobile Recharge.
  - **Internal Storage (SharedPreferences):** Managed via `AppStorage.kt` singleton to persist user credentials, dark mode, language state, debit/savings/credit card balances, transaction records, saved beneficiaries, and savings goals.
  - **Firebase Database Sync:** Managed via `FirebaseSync.kt` on background threads to persist user profiles, transactions, beneficiaries, and saving goals to Firebase Realtime Database.
  - **Google Login & Sign-Up:** Google Sign-In button and Google Play Services Auth flow integrated into both `LoginActivity` and `RegisterActivity`.
  - **Micro-Animations:**
    - Tactile button bounce (`bounceClick`) on all buttons, cards, tiles, and chips.
    - Live animated input validation checklist: input fields transition to green with a pop-in checkmark (`pop_in.xml` + `ic_check_circle_green.xml`) when valid, or shake with red highlight (`shake.xml`) on invalid inputs.
  - **Multi-language:** English and Bahasa Indonesia supported and wired into app context.
  - **Dark & Light Mode:** Both themes supported via tokens in `colors.xml` and `values-night/colors.xml`.
  - **Realistic Design Avatars & Beneficiaries:** Replaced all vector placeholder icons with 5 photorealistic portrait avatar assets strictly matching Figma identities:
    - `avatar_push.jpg`: Push Puttichai (primary user avatar on Home header, Profile header, Account & Card header, and Beneficiary #1).
    - `avatar_olivia.jpg`: Olivia Rodrigo (Beneficiary #2 & Transfer quick pick).
    - `avatar_emma.jpg`: Emma Watson (Transfer quick pick #1 & Beneficiary #3).
    - `avatar_justin.jpg`: Justin Bieber (Transfer quick pick #2 & Beneficiary #4).
    - `avatar_alexander.jpg`: Alexander (Beneficiary #5 & Transfer quick pick).
    - Dynamically mapped with circular crop across `HomeFragment`, `ProfileFragment`, `AccountCardActivity`, `TransferActivity`, `BeneficiaryActivity`, and `item_beneficiary_row.xml`.
  - **Removed Screens (Rule 3):** `PromoFragment`, `BudgetPlannerActivity`, `FavoriteBankActivity`, `FavoriteEwalletActivity` completely removed.

---

## Frames Checklist

| # | Frame Name | Built | Icons Exported | Matches Figma | Functional State | Notes |
|---|---|:---:|:---:|:---:|:---:|---|
| 1 | `Sign in` | [x] | [x] | [x] | [x] | Google Login, BiometricPrompt, PIN, live animated checkmarks, AppStorage + Firebase session sync |
| 2 | `Sign up #1` | [x] | [x] | [x] | [x] | Google Sign Up, step 1 validation with live checkmarks, AppStorage persistence |
| 3 | `Sign up #2` | [x] | [x] | [x] | [x] | Step 2 phone & password validation with live checkmarks, Firebase sync |
| 4 | `Forgot password #1` | [x] | [x] | [x] | [x] | Integrated OTP request in `LoginActivity.kt` |
| 5 | `Forgot password #2` | [x] | [x] | [x] | [x] | Verification step in `LoginActivity.kt` |
| 6 | `Forgot password #3` | [x] | [x] | [x] | [x] | Password reset form in `LoginActivity.kt` |
| 7 | `Forgot password #4` | [x] | [x] | [x] | [x] | Success dialog in `LoginActivity.kt` |
| 8 | `Change password #1` | [x] | [x] | [x] | [x] | Settings change password in `ProfileFragment.kt` |
| 9 | `Change password #2` | [x] | [x] | [x] | [x] | Success confirmation in `ProfileFragment.kt` |
| 10 | `Home` | [x] | [x] | [x] | [x] | 3x3 grid + Currency Exchange card, dynamic balance & cardholder from AppStorage, bounceClick |
| 11 | `Search` | [x] | [x] | [x] | [x] | `ExchangeRateActivity.kt` (Branch & ATM Search) |
| 12 | `Language` | [x] | [x] | [x] | [x] | Bottom sheet language selector, saved to AppStorage, context reload |
| 13 | `Setting` | [x] | [x] | [x] | [x] | Dark mode switch, Biometrics switch, PIN change, Profile header from AppStorage (manual IP removed) |
| 14 | `App information` | [x] | [x] | [x] | [x] | Legal & version dialog in `ProfileFragment.kt` |
| 15 | `Account & card #1` | [x] | [x] | [x] | [x] | Account tab, dynamic balance from AppStorage, bounceClick, Currency Exchange shortcut |
| 16 | `Account & card #2` | [x] | [x] | [x] | [x] | Card tab, carousel view in `AccountCardActivity.kt` |
| 17 | `Account management` | [x] | [x] | [x] | [x] | Card limits & actions in `AccountCardActivity.kt` |
| 18 | `Card Detail` | [x] | [x] | [x] | [x] | CVV peek, freeze card toggle, dynamic cardholder & balance |
| 19 | `Credit card #1` | [x] | [x] | [x] | [x] | Statement overview, dynamic balance in `CreditCardActivity.kt` |
| 20 | `Credit card #2` | [x] | [x] | [x] | [x] | Statement payment form with IDR auto-formatting |
| 21 | `Credit card #3` | [x] | [x] | [x] | [x] | Payment review in `CreditCardActivity.kt` |
| 22 | `Credit card #4` | [x] | [x] | [x] | [x] | PIN verification in `CreditCardActivity.kt` |
| 23 | `Credit card #5` | [x] | [x] | [x] | [x] | Deducts balance & saves transaction to AppStorage and Firebase |
| 24 | `Exchange rate` | [x] | [x] | [x] | [x] | Live currency calculator, adds converted amount to AppStorage |
| 25 | `Interest rate` | [x] | [x] | [x] | [x] | Deposit rates table in `ExchangeRateActivity.kt` |
| 26 | `Search for Branch` | [x] | [x] | [x] | [x] | Branch & ATM locator tab in `ExchangeRateActivity.kt` |
| 27 | `Transfer #1` | [x] | [x] | [x] | [x] | Transfer hub, prefill support from Beneficiaries |
| 28 | `Transfer #2` | [x] | [x] | [x] | [x] | Same bank transfer form in `TransferActivity.kt` |
| 29 | `Transfer #3` | [x] | [x] | [x] | [x] | Other bank transfer form in `TransferActivity.kt` |
| 30 | `Transfer #4` | [x] | [x] | [x] | [x] | Card transfer form in `TransferActivity.kt` |
| 31 | `Transfer #5` | [x] | [x] | [x] | [x] | IDR amount entry chips with real balance checks |
| 32 | `Transfer #6` | [x] | [x] | [x] | [x] | Transfer note & message in `TransferActivity.kt` |
| 33 | `Transfer #7` | [x] | [x] | [x] | [x] | Source account selector in `TransferActivity.kt` |
| 34 | `Transfer #8` | [x] | [x] | [x] | [x] | Transfer schedule picker in `TransferActivity.kt` |
| 35 | `Transfer #9` | [x] | [x] | [x] | [x] | Transfer summary review in `TransferActivity.kt` |
| 36 | `Transfer #5 - Confirm #1` | [x] | [x] | [x] | [x] | Confirmation bottom sheet step 1 |
| 37 | `Transfer #5 - Confirm #2` | [x] | [x] | [x] | [x] | Confirmation bottom sheet step 2 |
| 38 | `Transfer #5 - Confirm #3` | [x] | [x] | [x] | [x] | Fee breakdown review |
| 39 | `Transfer #5 - Confirm #4` | [x] | [x] | [x] | [x] | Real BiometricPrompt authorization |
| 40 | `Transfer #5 - Confirm #5` | [x] | [x] | [x] | [x] | Verification prompt |
| 41 | `Transfer #5 - Confirm #6` | [x] | [x] | [x] | [x] | Processing token screen |
| 42 | `Transfer #5 - Confirm #7` | [x] | [x] | [x] | [x] | Security PIN input sheet |
| 43 | `Transfer #5 - Successfully` | [x] | [x] | [x] | [x] | Real balance deducted, transaction recorded to AppStorage + Firebase |
| 44 | `Directory of beneficiary #1` | [x] | [x] | [x] | [x] | Dynamic list loaded from AppStorage, instant search filter |
| 45 | `Directory of beneficiary #2` | [x] | [x] | [x] | [x] | Add beneficiary persisted to AppStorage + Firebase |
| 46 | `Directory of beneficiary #3` | [x] | [x] | [x] | [x] | Detail view with instant Transfer Now shortcut |
| 47 | `Directory of beneficiary #4` | [x] | [x] | [x] | [x] | Delete beneficiary from AppStorage + Firebase |
| 48 | `Pay the bill` | [x] | [x] | [x] | [x] | Category hub (Electricity, Internet, Water, BPJS) |
| 49 | `Detail payment` | [x] | [x] | [x] | [x] | Dynamic customer name and balance deduction |
| 50 | `Electric bill #1` | [x] | [x] | [x] | [x] | PLN provider selector in `BillPaymentActivity.kt` |
| 51 | `Electric bill #2` | [x] | [x] | [x] | [x] | Customer ID input in `BillPaymentActivity.kt` |
| 52 | `Electric bill #3` | [x] | [x] | [x] | [x] | Bill inquiry summary in `BillPaymentActivity.kt` |
| 53 | `Electric bill #4` | [x] | [x] | [x] | [x] | PIN authorization in `BillPaymentActivity.kt` |
| 54 | `Electric bill #5` | [x] | [x] | [x] | [x] | Bill receipt, transaction saved to AppStorage + Firebase |
| 55 | `Internet payment` | [x] | [x] | [x] | [x] | Internet provider hub in `BillPaymentActivity.kt` |
| 56 | `Internet bill #1` | [x] | [x] | [x] | [x] | Subscriber number in `BillPaymentActivity.kt` |
| 57 | `Internet bill #2` | [x] | [x] | [x] | [x] | Internet package selector in `BillPaymentActivity.kt` |
| 58 | `Internet bill #3` | [x] | [x] | [x] | [x] | Internet inquiry breakdown in `BillPaymentActivity.kt` |
| 59 | `Internet bill #4` | [x] | [x] | [x] | [x] | Internet PIN verification in `BillPaymentActivity.kt` |
| 60 | `Internet bill #5` | [x] | [x] | [x] | [x] | Internet receipt in `BillPaymentActivity.kt` |
| 61 | `Recharge the phone #1` | [x] | [x] | [x] | [x] | Phone number entry & contacts picker |
| 62 | `Recharge the phone #2` | [x] | [x] | [x] | [x] | Pulsa & Data chips selection |
| 63 | `Recharge the phone #3 - Confirm #1` | [x] | [x] | [x] | [x] | Top up summary review |
| 64 | `Recharge the phone #3 - Confirm #2` | [x] | [x] | [x] | [x] | Real BiometricPrompt or PIN confirmation |
| 65 | `Recharge the phone #4` | [x] | [x] | [x] | [x] | Pulsa receipt, balance deducted in AppStorage + Firebase |
| 66 | `Withdraw #1` | [x] | [x] | [x] | [x] | Cash withdrawal hub |0
| 67 | `Withdraw #2` | [x] | [x] | [x] | [x] | Amount selection chips with balance check |
| 68 | `Withdraw #3` | [x] | [x] | [x] | [x] | ATM location picker |
| 69 | `Withdraw #4` | [x] | [x] | [x] | [x] | PIN entry for withdrawal |
| 70 | `Withdraw #5` | [x] | [x] | [x] | [x] | OTP token generation |
| 71 | `Withdraw #6` | [x] | [x] | [x] | [x] | 6-digit withdrawal code & transaction recorded to AppStorage |
| 72 | `Save online #1` | [x] | [x] | [x] | [x] | Savings dashboard with live balance from AppStorage |
| 73 | `Save online #2` | [x] | [x] | [x] | [x] | Goal type selection in `SaveOnlineActivity.kt` |
| 74 | `Save online #3` | [x] | [x] | [x] | [x] | Target amount & term chips |
| 75 | `Save online #4` | [x] | [x] | [x] | [x] | Real-time monthly debit calculation |
| 76 | `Save online #5` | [x] | [x] | [x] | [x] | Terms agreement checkbox |
| 77 | `Save online #6` | [x] | [x] | [x] | [x] | Goal created and persisted to AppStorage + Firebase |
| 78 | `Exchange #1` | [x] | [x] | [x] | [x] | `activity_exchange.xml` + `ExchangeActivity.kt`, top illustration, From/To, live rates |
| 79 | `Exchange #2` | [x] | [x] | [x] | [x] | `dialog_select_currency.xml`, active exchange, AppStorage + Firebase balance mutation |
| 80 | `Transaction report #1` | [x] | [x] | [x] | [x] | PDF e-statement export simulation |
| 81 | `Transaction report #2` | [x] | [x] | [x] | [x] | Monthly statement filter |
| 82 | `Payment history #1` | [x] | [x] | [x] | [x] | `fragment_history.xml` + `HistoryFragment.kt`, Electric/Water/Mobile/Internet pills & cards |
| 83 | `Payment history #2` | [x] | [x] | [x] | [x] | Month-grouped cards with Status (Successfully/Unsuccessfully), Amount, and Company |
| 84 | `Message #1` | [x] | [x] | [x] | [x] | Notifications & announcements tabs |
| 85 | `Message #2` | [x] | [x] | [x] | [x] | Security login alerts and transaction notifications |
| 86 | `Message #3` | [x] | [x] | [x] | [x] | Notification detail view with bounceClick |
| 87 | `QRIS Scanner & Payment` | [x] | [x] | [x] | [x] | Real CameraX preview, flashlight torch, gallery QR picker, balance deduction |

---

## Build Verification
- **Gradle Task:** `.\gradlew.bat --no-daemon --offline --max-workers=1 --no-configuration-cache :app:assembleDebug`
- **Result:** Build succeeds with the project-local temporary directory override (`D:\ANDROID_PROJECT\BankAppUtomo\.gradle-tmp`) and JDK 17 toolchain configuration. `:app:lintDebug` also succeeds. The APK was installed with ADB on `emulator-5554`, launched successfully, and the final log check found no fatal exception. The original host-wide temporary-directory setup still triggers the loopback error, so the local override is retained as the reliable verification command.
- **Latest runtime check:** Login renders without clipping at full emulator resolution. The username/password hints remain fully visible and the Indonesian labels are unchanged from the current design. Remaining compiler warnings are deprecation notices from the Google Sign-In dependency, not build or resource errors.
- **Latest localization pass:** Login/Register validation, Google sign-in, biometric prompt labels, and Digital ID feedback now use localized resources in `values/strings.xml` and `values-en/strings.xml`; no layout or appearance changes were made.
- **Brand consistency pass:** User-visible `Bank Utomo` fallbacks were normalized to `UTOMO Bank`, including the embedded splash animation text, receipt defaults, ATM/service labels, and support copy. The package/theme identifiers were intentionally left unchanged.
- **Notification language pass:** `activity_notification.xml` and `NotificationActivity.kt` now use localized resources for support banner text, notification cards, detail modal, empty state, and transaction/security detail content. Layout structure and visual tokens were preserved.
- **Mobile prepaid language pass:** `activity_mobile_prepaid.xml` and `MobilePrepaidActivity.kt` now localize the recharge flow labels, package price captions, confirmation/PIN states, biometric prompt, errors, and receipt state. Sample phone numbers, amounts, transaction IDs, and provider names remain unchanged as mock data.
- **Bill payment language pass:** `activity_bill_payment.xml` and `BillPaymentActivity.kt` now localize the service hub, provider/form labels, inquiry summary, confirmation/receipt headers, validation messages, and payment failure title. Existing provider names, customer IDs, dates, and IDR mock values remain unchanged.
- **Admin Panel launcher fix:** `start-admin-panel.bat` now serves from `admin-panel` before opening the browser and automatically selects the next available port when Laragon/Apache already owns `8181`. The local fallback login now opens `Operations Dashboard` instead of routing directly to Branch & ATM. Verified on `http://localhost:8181/index.html`: login, all navigation pages, refresh, and browser console checks passed.
- **Maintenance mode synchronization fix:** Android reads `config/maintenance` from Realtime Database, while the Admin Panel was also updating Firestore. The live RTDB value was still `enabled=true` even though Firestore was `false`, which caused Android devices to remain blocked. RTDB and Firestore are now both `enabled=false`; `BackOfficeRepository` now explicitly emits the disabled state, and Admin Panel maintenance changes persist and verify RTDB before reporting success. Lint and debug APK build pass, and the APK was installed on `emulator-5554` and launched to `DashboardActivity`.
- **Customer Service ticket reply fix:** Admin Panel replies used `senderRole=staff`, while Android classified messages using `senderType`, so staff replies appeared as customer bubbles. New replies now write both fields and update ticket unread metadata; Android and Admin Panel recognize both the new field and older messages for compatibility.
- **Real customer card controls:** Card Controls now aggregates cards from both the global `cards` collection and each real user's `users/{uid}/cards` subcollection, preventing registered users such as Harris from being omitted. New registrations mirror cards into the global collection, and block/unblock actions update the user's actual card record. Harris's existing card ending in `6834` was synchronized and verified in the live Admin Panel.
