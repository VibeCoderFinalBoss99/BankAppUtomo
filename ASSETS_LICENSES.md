# Asset and Figma Provenance

This file records the provenance status of visual assets used by the current Android project.

## Project-local assets

The existing files under `app/src/main/res/drawable`, `app/src/main/res/mipmap-*`, and `app/src/main/res/raw` are project-local assets already present in the workspace. They are not silently replaced or removed during the v3 audit.

## Figma assets

The following assets were exported from the connected Figma Community files during this audit. The source URLs are recorded for traceability. Figma Community ownership/commercial-use terms were not independently verified in the current session, so these entries must be checked before a public release.

| Asset | Used in | Source URL | Author | License/status |
|---|---|---|---|---|
| `source_b_avatar.png` | Home and Account/Card header | [Source B](https://www.figma.com/design/H4wccE9fcW38Vs1koo9kLH/Free-Banking-Mobile-App-Ui-Kit-With-light---Dark-Mode-High-Quality-Ui-43--Screen-template--Community-) | Community file author not exposed in the connected browser | Figma Community asset; permission/commercial use requires verification |
| `figma_card_01.png` | Home and card detail | [Source C](https://www.figma.com/design/erNxQ1vJWq1e4GoZgwsDSI/Banking-Cards-Design--Community-) | Community file author not exposed in the connected browser | Figma Community asset; permission/commercial use requires verification |
| `figma_card_variant_02.png` | Card deck variant 02 | [Source C](https://www.figma.com/design/erNxQ1vJWq1e4GoZgwsDSI/Banking-Cards-Design--Community-) | Community file author not exposed in the connected browser | Figma Community asset; permission/commercial use requires verification |
| `figma_card_variant_03.png` | Card deck variant 03 | [Source C](https://www.figma.com/design/erNxQ1vJWq1e4GoZgwsDSI/Banking-Cards-Design--Community-) | Community file author not exposed in the connected browser | Figma Community asset; permission/commercial use requires verification |
| `figma_card_variant_04.png` | Card deck variant 04 | [Source C](https://www.figma.com/design/erNxQ1vJWq1e4GoZgwsDSI/Banking-Cards-Design--Community-) | Community file author not exposed in the connected browser | Figma Community asset; permission/commercial use requires verification |
| `figma_card_pattern_01.png` | Shared card surfaces | [Source C](https://www.figma.com/design/erNxQ1vJWq1e4GoZgwsDSI/Banking-Cards-Design--Community-) | Community file author not exposed in the connected browser | Figma Community asset; permission/commercial use requires verification |

## Admin Panel SVG Icons

All icons utilized within the web admin panel (`admin-panel/icons.js`) originate from the Feather Icons / Lucide icon libraries.
- **License**: MIT License (Permissive commercial use)
- **Source**: [Feather Icons](https://feathericons.com) / [Lucide](https://lucide.dev)
- **Copyright**: (c) 2013-2024 Cole Bemis, Lucide Contributors
- **Included Icons**: `logo`, `dashboard`, `tickets`, `customers`, `cards`, `transactions`, `maintenance`, `staff`, `canned`, `audit`, `search`, `filter`, `refresh`, `plus`, `lock`, `unlock`, `check`, `alertTriangle`, `close`, `user`, `messageSquare`, `send`, `logout`, `flag`.
- **Emoji policy**: Zero emojis are utilized across the entire portal in accordance with enterprise banking back-office standards.

## Circular Flag Icons (Language Selector)

All circular flag vector drawables utilized in `LanguageActivity` (`ic_flag_us_round.xml`, `ic_flag_au_round.xml`, `ic_flag_fr_round.xml`, `ic_flag_es_round.xml`, `ic_flag_am_round.xml`, `ic_flag_vi_round.xml`, `ic_flag_id_round.xml`, `ic_flag_jp_round.xml`, `ic_flag_gb_round.xml`, `ic_check_circle_blue.xml`) are custom, clean vector drawables matching the Figma Community specification.
- **License**: MIT / Public Domain / Project-local
- **Format**: Android VectorDrawable XML
- **Styling**: Standardized 48x48 viewport with circular clip path and clean geometric paths.
- **Emoji policy**: Replaced all raw text/initials and emojis with crisp circular vector flag icons.

## Required follow-up before release

Before release, export the remaining approved Source A/B/C assets into the Android `drawable` or `mipmap` folders, record the originating Figma file/node and license/ownership decision here, and run the visual verification pass described in `figma-recreate-prompt-v3.md`.
