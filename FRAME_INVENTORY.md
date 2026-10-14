# Figma Frame Inventory

This inventory records the verified state of the v3 redesign execution.

## Source Totals

| Source | Total | Build rule | State |
|---|---:|---|---|
| A - BankEase Digital Banking UI Kit | 89 reported by the existing project audit | Build every Source A frame not replaced by Source B | Partially verified |
| B - Free Banking Mobile App UI Kit | 46 top-level frames in linked canvas | Source B replaces Onboarding, Home, History, Statistics and My Cards | Accessible; selected frames inspected |
| C - Banking Cards Design | 6 top-level sections; 50 numbered card variants identified in metadata | Source C supplies every bank-card visual | Accessible; variants 01-04 exported and applied to the main card surfaces |

The revised Source A, B, and C links are recorded in `figma-recreate-prompt-v3.md`. Source B light Home (`1:2940`) and dark Home (`1:5104`) were inspected, Source B My Cards (`1:2298`) was identified, and Source C card variants were rechecked in the connected browser at 50% zoom. The full visual rollout still needs to be completed frame by frame.

## Verified Source A Mapping

| Source | Frame name | Node ID | App route | Status | Light | Dark | English | Indonesian | Functional | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| A | Home | 2:22909 | Dashboard / HomeFragment | PARTIAL | yes | yes | partial | partial | yes | Source B should replace this screen; Source A layout was inspected and current project already has the 9 menu items. |
| A | Sign in | 54:21278 | LoginActivity | PARTIAL | yes | yes | partial | partial | yes | Existing input structure and validation are protected. |
| A | Sign up #1 | 54:21277 | RegisterActivity | PARTIAL | yes | yes | partial | partial | yes | Existing input structure and validation are protected. |
| A | Transfer #2 | 118:1520 | TransferActivity | PARTIAL | yes | yes | partial | partial | simulated | Source A context and screenshot were inspected. |
| A | Transfer #5 - Confirm #2 | 587:28733 | Transfer confirmation | PARTIAL | yes | yes | partial | partial | simulated | Source A context and screenshot were inspected. |
| A | Pay the bill | 285:40011 | BillPaymentActivity | PARTIAL | yes | yes | partial | partial | simulated | Source A context and screenshot were inspected. |
| A | Mobile prepaid | 571:4239 | MobilePrepaidActivity | PARTIAL | yes | yes | partial | partial | simulated | Source A context and screenshot were inspected. |
| A | Payment history #1 | 291:469 | HistoryFragment | PARTIAL | yes | yes | partial | partial | yes | Source B should replace this screen. |
| A | Transaction report #1 | 122:3520 | TransactionReportActivity | PARTIAL | yes | yes | partial | partial | simulated | Source A context and screenshot were inspected. |
| A | Directory of beneficiary #1 | 149:663 | BeneficiaryActivity | PARTIAL | yes | yes | partial | partial | simulated | Source A context and screenshot were inspected. |
| A | Setting | 104:3705 | ProfileFragment | PARTIAL | yes | yes | partial | verified | yes | Language and dark mode retained; Indonesian emulator check now shows `Ubah PIN Transaksi`, `Ubah Kata Sandi`, `Login Biometrik`, and `Mode Gelap`. |
| A | Account and card #1 | 290:4931 | AccountCardActivity | PARTIAL | yes | yes | partial | partial | simulated | Source A context and screenshot were inspected. |
| A | Credit card #1 | 2:22620 | CreditCardActivity | PARTIAL | yes | yes | partial | partial | simulated | Source C should replace card visuals. |

## Source B / C Current Rollout

| Source | Frame/variant | Status | Notes |
|---|---|---|---|
| B | Home light `1:2940` | PARTIAL | Context and screenshot inspected in the browser; Light runtime was verified on the emulator. Home keeps the required Source A actions and now uses a swipeable four-card Source C deck, so the intentional nine-menu fusion remains the only visual difference from the four-action source frame. |
| B | Home dark `1:5104` | PARTIAL | Context and screenshot inspected in the browser; Dark runtime was verified on the emulator with readable tokens and Source C cards. The intentional nine-menu fusion remains the only assigned-scope difference. |
| B | My Cards `1:2298` | PARTIAL | Screen inspected directly in the browser and emulator; the Account/Card route now opens the Source B-style My Cards state directly with a horizontally swipeable local Source C card deck, transactions, a monthly spending limit panel, add-card action, and complete bottom navigation. |
| B | History `1:3773` / `1:4517` | PARTIAL | Screen inspected directly in the browser; search, filters, summary, categories, and transaction states are present locally. |
| B | Statistics `1:2604` / `1:6153` | PARTIAL | Screen inspected directly in the browser; chart, balance, spending limit, and transaction sections are present locally. |
| B | Onboarding `1:4886` and related states | PARTIAL | Metadata available; existing local onboarding is retained until visual rollout. |
| C | Card variants `01-04` | PARTIAL | Complete local artwork is applied to Home, Card Detail, Credit Card, and four choices in My Cards. The remaining numbered Figma variants still need individual export if they are required as separate product types. |
| C | Remaining numbered variants | BLOCKED FOR IMPLEMENTATION | Metadata is available, but individual exports for the remaining numbered variants are still limited by the Figma Starter MCP quota. |

