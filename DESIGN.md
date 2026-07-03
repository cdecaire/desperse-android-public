---
version: alpha
name: Desperse
description: >-
  Content-first design system for a Web3 social platform on Solana.
  Minimal. Focused. Sharp. Dark by default. Authored in OKLCH for
  perceptually-uniform ramps, locked-hue palettes, and dark-mode vibrancy.
  Three post-type tones (Standard, Collectible, Edition) encode value
  and rarity at a glance. Tokens are authored once and applied across
  web, responsive web, iOS, and Android.

colors:
  # ═══════════════════════════════════════════════════════════════════════
  # Base palettes — OKLCH ramps, locked hue per palette, perceptually-even
  # lightness, sRGB-capped chroma. Each 600 step clears WCAG AA (4.5:1) on
  # white except flush-orange-600 (AA-Large only — use 700 for body text).
  # ═══════════════════════════════════════════════════════════════════════

  # Zinc — Hue 264°, near-zero chroma (cool neutral)
  zinc-50:  "oklch(97.2% 0.002 264)"
  zinc-100: "oklch(94.0% 0.003 264)"
  zinc-200: "oklch(88.0% 0.004 264)"
  zinc-300: "oklch(79.0% 0.004 264)"
  zinc-400: "oklch(68.0% 0.004 264)"
  zinc-500: "oklch(58.0% 0.005 264)"
  zinc-600: "oklch(48.0% 0.005 264)"
  zinc-700: "oklch(39.0% 0.004 264)"
  zinc-800: "oklch(30.0% 0.004 264)"
  zinc-900: "oklch(23.0% 0.003 264)"
  zinc-950: "oklch(15.0% 0.003 264)"

  # Torch Red — Hue 22° (destructive)
  torch-red-50:  "oklch(97.2% 0.013 22)"
  torch-red-100: "oklch(94.0% 0.028 22)"
  torch-red-200: "oklch(88.0% 0.060 22)"
  torch-red-300: "oklch(79.0% 0.118 22)"
  torch-red-400: "oklch(68.0% 0.190 22)"
  torch-red-500: "oklch(60.0% 0.235 22)"
  torch-red-600: "oklch(53.0% 0.225 22)"
  torch-red-700: "oklch(45.0% 0.190 22)"
  torch-red-800: "oklch(36.0% 0.150 22)"
  torch-red-900: "oklch(28.0% 0.110 22)"
  torch-red-950: "oklch(18.0% 0.075 22)"

  # Blue Gem — Hue 285° (collectible, violet-blue)
  blue-gem-50:  "oklch(97.2% 0.014 285)"
  blue-gem-100: "oklch(94.0% 0.030 285)"
  blue-gem-200: "oklch(88.0% 0.065 285)"
  blue-gem-300: "oklch(79.0% 0.130 285)"
  blue-gem-400: "oklch(68.0% 0.205 285)"
  blue-gem-500: "oklch(58.0% 0.265 285)"
  blue-gem-600: "oklch(48.0% 0.265 285)"
  blue-gem-700: "oklch(40.0% 0.225 285)"
  blue-gem-800: "oklch(31.0% 0.180 285)"
  blue-gem-900: "oklch(23.0% 0.135 285)"
  blue-gem-950: "oklch(15.0% 0.090 285)"

  # Purple Heart — Hue 309° (edition / brand accent, magenta-purple)
  purple-heart-50:  "oklch(97.2% 0.014 309)"
  purple-heart-100: "oklch(94.0% 0.030 309)"
  purple-heart-200: "oklch(88.0% 0.065 309)"
  purple-heart-300: "oklch(79.0% 0.135 309)"
  purple-heart-400: "oklch(68.0% 0.215 309)"
  purple-heart-500: "oklch(60.0% 0.275 309)"
  purple-heart-600: "oklch(52.0% 0.275 309)"
  purple-heart-700: "oklch(43.0% 0.230 309)"
  purple-heart-800: "oklch(33.0% 0.180 309)"
  purple-heart-900: "oklch(24.0% 0.135 309)"
  purple-heart-950: "oklch(15.0% 0.090 309)"

  # Caribbean Green — Hue 173° (standard / success, teal-green)
  caribbean-green-50:  "oklch(97.2% 0.018 173)"
  caribbean-green-100: "oklch(94.0% 0.035 173)"
  caribbean-green-200: "oklch(88.0% 0.070 173)"
  caribbean-green-300: "oklch(79.0% 0.115 173)"
  caribbean-green-400: "oklch(68.0% 0.140 173)"
  caribbean-green-500: "oklch(58.0% 0.135 173)"
  caribbean-green-600: "oklch(45.0% 0.118 173)"
  caribbean-green-700: "oklch(38.0% 0.100 173)"
  caribbean-green-800: "oklch(30.0% 0.080 173)"
  caribbean-green-900: "oklch(23.0% 0.060 173)"
  caribbean-green-950: "oklch(15.0% 0.040 173)"

  # Flush Orange — Hue 52° (warning, amber-orange)
  flush-orange-50:  "oklch(97.2% 0.018 52)"
  flush-orange-100: "oklch(94.0% 0.038 52)"
  flush-orange-200: "oklch(88.0% 0.075 52)"
  flush-orange-300: "oklch(79.0% 0.130 52)"
  flush-orange-400: "oklch(72.0% 0.170 52)"
  flush-orange-500: "oklch(68.0% 0.180 52)"
  flush-orange-600: "oklch(54.0% 0.155 52)"
  flush-orange-700: "oklch(45.0% 0.130 52)"
  flush-orange-800: "oklch(36.0% 0.105 52)"
  flush-orange-900: "oklch(28.0% 0.080 52)"
  flush-orange-950: "oklch(18.0% 0.055 52)"

  # Azure Radiance — Hue 250° (info, sky-blue)
  azure-radiance-50:  "oklch(97.2% 0.014 250)"
  azure-radiance-100: "oklch(94.0% 0.030 250)"
  azure-radiance-200: "oklch(88.0% 0.060 250)"
  azure-radiance-300: "oklch(79.0% 0.115 250)"
  azure-radiance-400: "oklch(68.0% 0.175 250)"
  azure-radiance-500: "oklch(60.0% 0.215 250)"
  azure-radiance-600: "oklch(52.0% 0.225 250)"
  azure-radiance-700: "oklch(43.0% 0.195 250)"
  azure-radiance-800: "oklch(34.0% 0.155 250)"
  azure-radiance-900: "oklch(25.0% 0.115 250)"
  azure-radiance-950: "oklch(16.0% 0.075 250)"

  # ═══════════════════════════════════════════════════════════════════════
  # Dark-mode-tuned tone overrides — boosted chroma for vibrancy on the
  # dark canvas. Activated via .dark class. Edition leans pinker by design.
  # ═══════════════════════════════════════════════════════════════════════

  tone-edition-dark:     "oklch(62.0% 0.295 318)"
  tone-collectible-dark: "oklch(72.0% 0.225 285)"
  tone-standard-dark:    "oklch(78.0% 0.205 168)"
  tone-info-dark:        "oklch(76.0% 0.165 235)"
  tone-warning-dark:     "oklch(82.0% 0.165 65)"
  highlight-dark:        "oklch(70.0% 0.285 318)"
  destructive-dark:      "oklch(68.0% 0.225 25)"

  # ═══════════════════════════════════════════════════════════════════════
  # Semantic tokens — light mode (counterpart)
  # All tone-* tokens were re-anchored from 500/600 to 600/700 so each
  # passes WCAG AA (4.5:1) on white as text or icon color.
  # ═══════════════════════════════════════════════════════════════════════

  background-light: "#ffffff"
  foreground-light: "{colors.zinc-950}"
  card-light: "#ffffff"
  card-foreground-light: "{colors.zinc-950}"
  popover-light: "#ffffff"
  popover-foreground-light: "{colors.zinc-950}"
  primary-light: "{colors.zinc-950}"
  primary-foreground-light: "{colors.zinc-50}"
  secondary-light: "{colors.zinc-100}"
  secondary-foreground-light: "{colors.zinc-950}"
  muted-light: "{colors.zinc-100}"
  muted-foreground-light: "{colors.zinc-600}"
  accent-light: "{colors.zinc-100}"
  accent-foreground-light: "{colors.zinc-950}"
  destructive-light: "{colors.torch-red-600}"
  destructive-foreground-light: "#ffffff"
  border-light: "{colors.zinc-200}"
  input-light: "{colors.zinc-200}"
  ring-light: "{colors.zinc-400}"
  tone-standard-light: "{colors.caribbean-green-600}"
  tone-collectible-light: "{colors.blue-gem-600}"
  tone-edition-light: "{colors.purple-heart-700}"
  tone-warning-light: "{colors.flush-orange-700}"
  tone-info-light: "{colors.azure-radiance-600}"
  highlight-light: "{colors.purple-heart-700}"

  # ═══════════════════════════════════════════════════════════════════════
  # Semantic tokens — dark mode (brand default)
  # ═══════════════════════════════════════════════════════════════════════

  background: "{colors.zinc-950}"
  foreground: "{colors.zinc-50}"
  card: "{colors.zinc-900}"
  card-foreground: "{colors.zinc-50}"
  popover: "{colors.zinc-900}"
  popover-foreground: "{colors.zinc-50}"
  primary: "{colors.zinc-50}"
  primary-foreground: "{colors.zinc-950}"
  secondary: "{colors.zinc-800}"
  secondary-foreground: "{colors.zinc-50}"
  muted: "{colors.zinc-800}"
  muted-foreground: "{colors.zinc-400}"
  accent: "{colors.zinc-800}"
  accent-foreground: "{colors.zinc-50}"
  destructive: "{colors.destructive-dark}"
  destructive-foreground: "#ffffff"
  border: "{colors.zinc-700}"
  input: "{colors.zinc-700}"
  ring: "{colors.zinc-500}"
  sidebar: "{colors.zinc-950}"
  sidebar-foreground: "{colors.zinc-50}"
  sidebar-primary: "{colors.zinc-50}"
  sidebar-primary-foreground: "{colors.zinc-950}"
  sidebar-accent: "{colors.zinc-800}"
  sidebar-accent-foreground: "{colors.zinc-50}"
  sidebar-border: "{colors.zinc-900}"
  sidebar-ring: "{colors.zinc-500}"
  tone-standard: "{colors.tone-standard-dark}"
  tone-collectible: "{colors.tone-collectible-dark}"
  tone-edition: "{colors.tone-edition-dark}"
  tone-warning: "{colors.tone-warning-dark}"
  tone-info: "{colors.tone-info-dark}"
  highlight: "{colors.highlight-dark}"

typography:
  # Display tier — marketing only. Display 4XL is fluid via clamp() for
  # editorial wordmarks; the rest are fixed scale.
  display-4xl:
    fontFamily: Figtree
    fontSize: clamp(4.5rem, 12vw, 12rem)
    fontWeight: 800
    lineHeight: 0.9
    letterSpacing: -0.05em
  display-3xl:
    fontFamily: Figtree
    fontSize: 6rem
    fontWeight: 700
    lineHeight: 0.95
    letterSpacing: -0.045em
  display-2xl:
    fontFamily: Figtree
    fontSize: 4.5rem
    fontWeight: 700
    lineHeight: 1.0
    letterSpacing: -0.04em
  display-xl:
    fontFamily: Figtree
    fontSize: 3.75rem
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: -0.035em
  display-lg:
    fontFamily: Figtree
    fontSize: 3rem
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: -0.03em

  # Heading tier — semantic h1–h4. One heading-1 per route.
  heading-1:
    fontFamily: Figtree
    fontSize: 2.25rem
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: -0.025em
  heading-2:
    fontFamily: Figtree
    fontSize: 1.75rem
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: -0.02em
  heading-3:
    fontFamily: Figtree
    fontSize: 1.375rem
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: -0.015em
  heading-4:
    fontFamily: Figtree
    fontSize: 1.125rem
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: -0.01em

  # Title tier — repeating UI primitives (card titles, modal headers).
  title-lg:
    fontFamily: Figtree
    fontSize: 1rem
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: -0.01em
  title-sm:
    fontFamily: Figtree
    fontSize: 0.875rem
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: -0.005em

  # Body tier — reading text.
  body-lg:
    fontFamily: Figtree
    fontSize: 1.0625rem
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: -0.005em
  body-md:
    fontFamily: Figtree
    fontSize: 0.9375rem
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: -0.005em
  body-sm:
    fontFamily: Figtree
    fontSize: 0.8125rem
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0

  # Label tier — functional UI text. Tighter than body, never wraps to a
  # third line. Caption is the only non-semibold label.
  label-lg:
    fontFamily: Figtree
    fontSize: 0.875rem
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: -0.005em
  label-md:
    fontFamily: Figtree
    fontSize: 0.75rem
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: 0
  label-xs:
    fontFamily: Figtree
    fontSize: 0.6875rem
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: 0.06em
    textTransform: uppercase
  caption:
    fontFamily: Figtree
    fontSize: 0.75rem
    fontWeight: 400
    lineHeight: 1.4
    letterSpacing: 0

  # Mono tier — for on-chain data, IDs, code. Always tabular figures.
  mono-md:
    fontFamily: DM Mono
    fontSize: 0.8125rem
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0
    fontVariant: tabular-nums
  mono-sm:
    fontFamily: DM Mono
    fontSize: 0.6875rem
    fontWeight: 500
    lineHeight: 1.2
    letterSpacing: 0.02em
    fontVariant: tabular-nums

rounded:
  none: 0px
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 20px
  full: 9999px

spacing:
  base: 8px
  px: 1px
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 24px
  2xl: 32px
  3xl: 48px
  4xl: 64px
  5xl: 96px
  # Layout constants
  nav-height: 56px
  sidebar-width: 256px
  feed-max: 1024px
  wide-max: 1536px
  ultrawide-max: 1800px
  body-measure: 65ch
  # Platform-specific minimum touch targets
  touch-ios: 44px
  touch-android: 48px

components:
  # ─── Buttons ─────────────────────────────────────────────────────────────
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.primary-foreground}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.full}"
    height: 40px
    padding: 0 16px
  button-primary-hover:
    backgroundColor: "{colors.zinc-200}"
  button-primary-md:
    height: 32px
  button-secondary:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.secondary-foreground}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.full}"
    height: 40px
    padding: 0 16px
  button-secondary-hover:
    backgroundColor: "{colors.zinc-800}"
  button-destructive:
    backgroundColor: "{colors.destructive}"
    textColor: "{colors.destructive-foreground}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.full}"
    height: 40px
    padding: 0 16px
  button-ghost:
    backgroundColor: transparent
    textColor: "{colors.foreground}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.full}"
    height: 40px
    padding: 0 12px
  button-ghost-hover:
    backgroundColor: "{colors.zinc-800}"
  button-outline:
    backgroundColor: transparent
    textColor: "{colors.foreground}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.full}"
    height: 40px
    padding: 0 16px
  button-icon:
    backgroundColor: transparent
    textColor: "{colors.foreground}"
    rounded: "{rounded.full}"
    height: 40px
    width: 40px

  # ─── Card ────────────────────────────────────────────────────────────────
  card:
    backgroundColor: "{colors.card}"
    textColor: "{colors.card-foreground}"
    rounded: "{rounded.lg}"
    padding: 24px
  card-flat:
    backgroundColor: transparent
    textColor: "{colors.foreground}"
    rounded: "{rounded.lg}"
    padding: 24px

  # ─── Inputs ──────────────────────────────────────────────────────────────
  input-field:
    backgroundColor: "{colors.card}"
    textColor: "{colors.foreground}"
    typography: "{typography.body-md}"
    rounded: "{rounded.sm}"
    height: 40px
    padding: 0 12px
  input-field-invalid:
    backgroundColor: "{colors.card}"
  textarea:
    backgroundColor: "{colors.card}"
    textColor: "{colors.foreground}"
    typography: "{typography.body-md}"
    rounded: "{rounded.sm}"
    padding: 12px

  # ─── Badges ──────────────────────────────────────────────────────────────
  badge-default:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.secondary-foreground}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"
    padding: 2px 10px
  badge-destructive:
    backgroundColor: "{colors.destructive}"
    textColor: "{colors.destructive-foreground}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"
  badge-success:
    backgroundColor: "{colors.tone-standard}"
    textColor: "{colors.zinc-950}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"
  badge-warning:
    backgroundColor: "{colors.tone-warning}"
    textColor: "{colors.zinc-950}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"
  badge-outline:
    backgroundColor: transparent
    textColor: "{colors.foreground}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"

  # ─── Pills ───────────────────────────────────────────────────────────────
  pill-display:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.secondary-foreground}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"
    padding: 2px 8px
  pill-interactive:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.secondary-foreground}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"
    padding: 2px 8px
  pill-interactive-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.primary-foreground}"

  # ─── Media pills (overlays on imagery) ───────────────────────────────────
  media-pill-dark:
    backgroundColor: "rgba(9, 9, 11, 0.85)"
    textColor: "#ffffff"
    typography: "{typography.mono-sm}"
    rounded: "{rounded.full}"
    height: 24px
    padding: 0 10px
  media-pill-tone-edition:
    backgroundColor: "{colors.tone-edition}"
    textColor: "{colors.zinc-950}"
    typography: "{typography.mono-sm}"
    rounded: "{rounded.full}"
    height: 24px
  media-pill-tone-collectible:
    backgroundColor: "{colors.tone-collectible}"
    textColor: "{colors.zinc-950}"
    typography: "{typography.mono-sm}"
    rounded: "{rounded.full}"
    height: 24px
  media-pill-tone-standard:
    backgroundColor: "{colors.tone-standard}"
    textColor: "{colors.zinc-950}"
    typography: "{typography.mono-sm}"
    rounded: "{rounded.full}"
    height: 24px

  # ─── Navigation ──────────────────────────────────────────────────────────
  nav-sidebar-item:
    backgroundColor: transparent
    textColor: "{colors.sidebar-foreground}"
    typography: "{typography.body-md}"
    rounded: "{rounded.full}"
    padding: 8px 12px
  nav-sidebar-item-hover:
    backgroundColor: "{colors.zinc-800}"
  nav-sidebar-item-active:
    backgroundColor: "{colors.sidebar-accent}"
    textColor: "{colors.foreground}"
  nav-bottom-item:
    backgroundColor: transparent
    textColor: "{colors.muted-foreground}"
    typography: "{typography.label-md}"
    height: 56px
  nav-bottom-item-active:
    textColor: "{colors.foreground}"
  nav-topbar:
    backgroundColor: "{colors.background}"
    textColor: "{colors.foreground}"
    height: 56px

  # ─── Notification badge ──────────────────────────────────────────────────
  notification-badge:
    backgroundColor: "{colors.destructive}"
    textColor: "{colors.destructive-foreground}"
    typography: "{typography.label-md}"
    rounded: "{rounded.full}"
    height: 20px

  # ─── Surfaces ────────────────────────────────────────────────────────────
  dialog:
    backgroundColor: "{colors.card}"
    textColor: "{colors.card-foreground}"
    rounded: "{rounded.lg}"
    padding: 24px
  sheet:
    backgroundColor: "{colors.card}"
    textColor: "{colors.card-foreground}"
    rounded: "{rounded.lg}"
  tooltip:
    backgroundColor: "{colors.zinc-900}"
    textColor: "{colors.foreground}"
    typography: "{typography.label-md}"
    rounded: "{rounded.sm}"
    padding: 6px 10px
  toast:
    backgroundColor: "{colors.card}"
    textColor: "{colors.card-foreground}"
    rounded: "{rounded.md}"
    padding: 12px 16px

  # ─── Post card (composite) ───────────────────────────────────────────────
  post-card:
    backgroundColor: "{colors.card}"
    textColor: "{colors.card-foreground}"
    rounded: "{rounded.lg}"
    padding: 16px
  post-action-button:
    backgroundColor: transparent
    textColor: "{colors.foreground}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.full}"
    height: 40px
  post-action-button-hover:
    backgroundColor: "{colors.zinc-900}"

---

# Desperse Design System

Desperse is a Web3 social platform for creators on Solana. This document is
the single source of truth for its visual identity. The YAML tokens above
define the atoms; the prose below explains **how and why** to apply them
across web, responsive web, iOS, and Android.

## Where the system lives

This document is one of three artifacts that together form the design
system. Treat them as one thing in three layers:

| Layer | Artifact | Purpose |
|---|---|---|
| **Spec** | `DESIGN.md` (this file) | Tokens + rationale. Read before any visual work. Follows the Google `design.md` format. |
| **Implementation** | `src/styles.css` | CSS custom properties for the OKLCH palette, semantic tokens (light + dark), dark-tuned tone overrides, paired typography utilities, radii, shadows. |
| **Living demo** | `/dev/typography-test` | Standalone route in the app. Renders every token, palette ramp, dark-tuned tone, vertical rhythm, and pattern from this spec, with a working theme switcher. |

**The relationship.** The YAML tokens below mirror the CSS custom properties
in `src/styles.css`. The Markdown prose explains the same rules the
`/dev/typography-test` page demonstrates visually. If the three ever
disagree, this document is right and the others are drifting — fix them.

**When making visual changes:** read the relevant section here, use existing
tokens via Tailwind classes (`bg-card`, `text-muted-foreground`,
`bg-tone-edition`, `text-heading-1`) or palette steps via
`text-(--purple-heart-500)` syntax, and verify against the rendered demo
page. If the system genuinely lacks what you need, propose an addition here
first, then add it to `src/styles.css`, then use it.

## Overview

Desperse is **minimal, focused, and sharp**. It is a content-first platform:
the creator's work is the interface, and the UI recedes so art advances.
Confidence comes from restraint, not decoration.

**Audience.** Crypto-native creators who already live in Web3 and want a
polished, powerful creative tool. They expect speed, precision, and respect
for their time. The interface should feel like a sharp instrument — fast to
learn, faster to use, never in the way of the content.

**Emotional response.** Quiet confidence. The app should feel like a gallery
at night — dark walls, lit artwork, nothing superfluous on the walls.
Premium, legible, fast.

**References.** Zora and Foundation (art-forward, Web3-native), SuperRare
and Exchange.Art (premium collectible feel), Linear and Vercel
(developer-grade polish and speed), Instagram and TikTok (proven
content-first social patterns).

**Anti-patterns.** Overly playful UI, excessive animation, neon glow
effects, glassmorphism as decoration, busy dashboards, "Web3-for-Web3's-sake"
aesthetics (wallet jargon, hex addresses in faces, blockchain-first UX).

**Cross-surface intent.** A single token set drives all four platforms.
Native shells generate platform-specific theme files at build time.

- **Web (desktop ≥ 1024px).** Sidebar navigation (256px), max feed width
  1024px, hover states, keyboard focus.
- **Responsive web (< 1024px).** Fixed top bar (56px) with safe-area inset,
  bottom tab bar (56px), full-bleed cards on mobile.
- **iOS (PWA + native shell).** 44pt minimum touch target. Haptic feedback
  on destructive and collect actions.
- **Android (PWA + native shell).** 48dp minimum touch target. Material
  ripple replaced with the Desperse `zinc-800` hover state for cross-platform
  consistency.

## Colors

The palette is **authored in OKLCH**: every color is described as
lightness, chroma, hue. This gives us perceptually-uniform ramps (each step
visually feels equally separated from the next), locked hue across a palette
(no accidental hue drift between 50 and 950), and gamut-aware chroma (the
brightest in-gamut saturation at every step). It also unlocks a feature
unique to dark mode: dedicated **dark-tuned tone overrides** with chroma
boosted past what a single ramp can offer, so the dark surface feels
electric instead of conservative.

### Palette construction

Seven palettes:

| Palette | Hue | Role |
|---|---|---|
| zinc | 264° | Neutral. Background, text, borders. |
| purple-heart | 309° | Edition tone / brand accent. Magenta-purple. |
| blue-gem | 285° | Collectible tone. Violet-blue. |
| caribbean-green | 173° | Standard tone / success. Teal-green. |
| azure-radiance | 250° | Info. Sky-blue. |
| flush-orange | 52° | Warning. Amber-orange. |
| torch-red | 22° | Destructive. |

Each palette is 11 stops (50 → 950). Lightness steps from ~97% to ~15%,
roughly even in OKLCH L, with deliberate adjustments where AA contrast
required them:

- `caribbean-green-600` is darkened to L=45% so it clears AA on white
  (teal sRGB volume is narrow at 48%).
- `torch-red-600` is darkened to L=53% so destructive-on-white passes AA.
- `flush-orange-600` is the **one exception**: orange at this hue cannot
  pass AA body-text on white in sRGB without going muddy brown. Use
  `flush-orange-700` for body text on white; `600` is fine for icons,
  large text, and dark-mode surfaces.

### The dark-tuned tones

Dark mode does not reuse the standard ramp. It uses dedicated values with
**~10–25% higher chroma** than the equivalent light-mode tones, because
dark backgrounds tolerate (and benefit from) more saturation.

| Token | OKLCH | Notes |
|---|---|---|
| `tone-edition-dark` | `oklch(62% 0.295 318)` | Electric magenta. Hue pulled to 318° because pure 309° caps at lower chroma in sRGB and reads more lavender than neon. |
| `tone-collectible-dark` | `oklch(72% 0.225 285)` | Saturated violet, the cooler sibling. |
| `tone-standard-dark` | `oklch(78% 0.205 168)` | Vivid mint — alive, not forest. |
| `tone-info-dark` | `oklch(76% 0.165 235)` | Clean cyan-blue, distinctly not violet. |
| `tone-warning-dark` | `oklch(82% 0.165 65)` | Warm amber, never yellow. |
| `highlight-dark` | `oklch(70% 0.285 318)` | Edition family, alpha-tuned for fills and overlays. |
| `destructive-dark` | `oklch(68% 0.225 25)` | Threads both contrast walls — 5.1:1 on canvas, 3.1:1 reverse. |

`tone-edition-dark` and `highlight-dark` are sRGB-clamped at the magenta
boundary near C=0.295. In Display-P3 they reach C≈0.36 — a future
`@media (color-gamut: p3)` override pass would unlock genuinely fluorescent
magenta on capable displays.

### Semantic tokens

Components reference **semantic tokens**, never palette steps directly.
The semantic layer (`background`, `foreground`, `card`, `muted-foreground`,
`primary`, `destructive`, `tone-*`, etc.) is what reads in component code;
the palette layer is the implementation.

In dark mode, `primary` is `zinc-50` and `primary-foreground` is `zinc-950`
— intentional inversion, so a "primary button" in dark mode is a white pill,
and in light mode it is a near-black pill. The brand never relies on purple
for primary CTAs.

### Tone system (the taxonomy)

Desperse posts have three economic modes, each with a tone that appears
consistently wherever the post is represented — the action button, the media
overlay pill, and the indicator on the compose tray.

- **Standard.** Free-form posts. `tone-standard` (caribbean-green family).
- **Collectible.** Free-to-mint NFTs. `tone-collectible` (blue-gem family).
- **Edition.** Paid NFT editions. `tone-edition` (purple-heart family).

Edition and Collectible are intentionally close but distinct: Edition is
redder (warmer, more premium); Collectible is bluer (cooler, more
community). The hue shift is the semantic signal.

### Contrast targets

All foreground/background pairings meet **WCAG AA** (4.5:1 body text,
3:1 large text and UI elements).

| Pair | Mode | Ratio |
|---|---|---|
| `foreground` on `background` | dark | 19 : 1 |
| `foreground` on `card` | dark | 16 : 1 |
| `muted-foreground` on `background` | dark | 7 : 1 |
| `tone-edition` (dark-tuned) on `background` | dark | 5.5 : 1 |
| `destructive` (dark-tuned) on `background` | dark | 5.1 : 1 |
| `foreground-light` on `background-light` | light | 19 : 1 |
| `muted-foreground-light` on `background-light` | light | 7 : 1 |
| `tone-standard-light` (caribbean-green-600) on white | light | 4.6 : 1 |
| `tone-warning-light` (flush-orange-700) on white | light | 5.2 : 1 |
| `destructive-light` (torch-red-600) on white | light | 4.6 : 1 |

### Native-platform mapping

- **iOS.** Map semantic tokens to dynamic color assets in `Assets.xcassets`
  pairing dark/light variants so `UITraitCollection` handles the swap.
  `tone-*` colors live as named colors. Modern iOS supports `oklch()` via
  Core Graphics' wide-color paths; fall back to sRGB conversions on older
  versions.
- **Android.** Map semantic tokens to `MaterialTheme.colorScheme`:
  `background` → `surface`, `card` → `surfaceContainer`, `primary` →
  `onSurface`, `destructive` → `error`. Post-type tones live as extension
  colors. OKLCH values convert at build time to `Color()` literals.

## Typography

**Figtree** is the single family for UI and body. **DM Mono** is reserved
for on-chain data (signatures, hashes, balances, mint supply counts), never
as "technical vibes" decoration.

Figtree was chosen over the reflex defaults (Inter, DM Sans) because it
hits a specific note: geometric construction as clean as Inter, but slightly
softer terminals and open apertures give it warmth — which matters on a
creator-facing product where Inter can read as too much like a developer
tool.

### The six tiers

The system is six tiers — Display, Heading, Title, Body, Label, Mono. Each
utility bundles four properties (size, weight, line-height, letter-spacing)
as a single token. **Compose with color and margin; never override the four
paired properties individually.**

- **Display** — marketing only. `display-2xl` for hero (one per page max).
  `display-3xl` and `display-4xl` for editorial wordmarks, the latter using
  `clamp()` for fluid sizing on landing pages.
- **Heading** — semantic h1–h4. One `heading-1` per route. Anything below
  h4 should be a Title, not a Heading.
- **Title** — repeating UI primitives (card titles, modal headers, list-row
  titles). Not page hierarchy; sits inside Headings.
- **Body** — reading text. `body-lg` for long-form, `body-md` for default
  UI prose, `body-sm` for dense secondary content.
- **Label** — functional UI text. Tighter than body, never wraps to a third
  line. `label-xs` is auto-uppercase with eyebrow tracking.
- **Mono** — DM Mono for code, addresses, hashes, IDs. Always renders with
  tabular figures.

### Scale

A **1.2 (minor third)** modular scale computed from a responsive root:

- Mobile (`< 768px`): root is **16px**.
- Desktop (`≥ 768px`): root is **14px**.

This means `body-md` (≈1rem) is 16px on mobile but 14px on desktop —
matching the ergonomics of each surface (readable at arm's length on
phones, denser at viewing distance on monitors). All sizes inherit from
this root via `rem`, so the entire scale flexes with one variable.

### Weight strategy

- **400 (Regular)** for body text. The new system relies on increased
  line-height + paired tracking to make 400 feel substantial on dark.
- **500 (Medium)** for body in legacy code paths only — the system has
  consolidated on 400 in Body and 600 in Heading/Title/Label.
- **600 (Semibold)** is the only heading weight. All Heading and Title
  utilities, plus most Label utilities. The design does not use 700+ in
  product UI. (Display tier uses 700/800 for marketing only.)
- **400 in DM Mono** for chain data.

A maximum of **two weights on any single screen** is enforced.

### OpenType features

Body text app-wide enables `kern`, `liga`, `calt` via
`font-feature-settings`. `calt` activates Figtree's contextual alternates,
visibly tightening spacing around capitals and certain glyph pairs. Kerning
is enabled explicitly via `font-kerning: normal`.

Mono utilities (`text-mono-md`, `text-mono-sm`, `code`, `.font-mono`) all
set `font-variant-numeric: tabular-nums`. Digits in addresses, hashes, IDs,
and spec values align column-by-column without alignment hacks.

### Vertical rhythm

Margins between content blocks are tied to the **heading tier above**, not
the content below. This gives consistent breathing room regardless of what
follows.

| Element | Top | Bottom | Notes |
|---|---|---|---|
| Display | 0 | mb-6 | Hero owns its own spacing |
| Heading 1 | 0 | mb-4 | One per route, top of page |
| Heading 2 (mid-doc) | mt-12 | mb-3 | First H2 in a section: mt-0 |
| Heading 3 | mt-8 | mb-2 | — |
| Heading 4 | mt-6 | mb-2 | — |
| Title (in card) | 0 | mb-1.5 | Subtitle/body sits tight beneath |
| Paragraph (Body) | 0 | mb-4 | Last child: mb-0 |
| List (ul/ol) | 0 | mb-4 | Items: mb-1 |
| Eyebrow / Label XS | 0 | mb-3 | Above its parent heading |
| Hr / divider | my-8 | — | Major section break |

### Native-platform mapping

| Token | iOS UIFont | Android TextAppearance |
|---|---|---|
| `display-2xl` | `largeTitle` 700 | `displayLarge` |
| `heading-1` | `title1` 600 | `headlineLarge` |
| `heading-2` | `title2` 600 | `headlineMedium` |
| `title-lg` | `title3` 600 | `titleLarge` |
| `body-lg` | `body` 400 | `bodyLarge` |
| `body-md` | `body` 400 | `bodyMedium` |
| `body-sm` | `subheadline` 400 | `bodySmall` |
| `label-lg` | `footnote` 600 | `labelLarge` |
| `label-md` | `caption1` 600 | `labelMedium` |
| `label-xs` | `caption2` 600 | `labelSmall` |

iOS Dynamic Type scaling is respected — the 16px mobile root accommodates
up to "Accessibility Large" without layout break. Android `Configuration`
font scaling honored via `Compose` `MaterialTheme`.

## Layout

Desperse uses a **three-tier responsive shell** that adapts instead of
shrinks. Nothing is amputated on mobile.

### The 8px grid

All spacing is on an 8px base with a 4px half-step for micro-adjustments.
The semantic spacing scale (`xs` = 4, `sm` = 8, `md` = 12, `lg` = 16,
`xl` = 24, `2xl` = 32, `3xl` = 48, `4xl` = 64, `5xl` = 96) is what
components consume. Pixel-named tokens are not exposed — density is
expressed through intent, not math.

### Breakpoints

| Name | Width | Behavior |
|---|---|---|
| `sm` | 640px | Large phones; 2-column layouts begin |
| `md` | 768px | Tablets; type root drops 16 → 14px |
| `lg` | 1024px | Desktops; sidebar appears, bottom nav hides |
| `xl` | 1280px | Comfortable desktop; extra side padding |
| `2xl` | 1536px | Admin / settings max width; ultrawide cap engages |

Breakpoints are **content-driven, not generic** — they kick in where the
design actually breaks, not at fixed device widths.

### Container widths

- **Feed and standard routes:** `max-width: 1024px`, centered.
- **Settings / admin / post detail:** `max-width: 1536px`, left-aligned.
- **Documentation pages:** full-bleed with `max-width: 1800px` cap on
  ultrawide so line lengths stay readable.
- **Post detail:** full width; the media is the layout.
- **Prose (post caption, about, changelog):** clamp line length to `65ch`.

### Touch targets

- **iOS:** 44×44pt minimum for any tappable element.
- **Android:** 48×48dp minimum.
- **Web mobile:** 40px is acceptable for secondary actions; primary actions
  and the bottom tab bar hit 48px.

Icon buttons in desktop density (`32px`) exist for power users in settings
and admin tooling only — never in the feed.

### Safe areas (mobile)

Every fixed-position element respects `env(safe-area-inset-*)`. The top
bar adds inset-top to its padding, the bottom tab bar adds inset-bottom,
and any full-screen modal applies both. Landscape insets are honored on
iOS.

### Motion

- **Scroll-driven nav.** Top and bottom bars hide on scroll-down, reappear
  on scroll-up. `transform 200ms ease-out`.
- **Smooth scroll.** Lenis on web for feed browsing.
- **Reduced motion.** All scroll-linked and decorative animations honor
  `prefers-reduced-motion: reduce` by switching to instant transitions.

Default transition: `transition-colors duration-150`. Hover on cards uses
either scale or shadow, never both. Anything longer than 300ms must be
skippable. No bounce, spring, or overshoot in product UI — physical motion
belongs to the marketing surface only.

## Elevation & Depth

Desperse is a **flat-with-tonal-layers** system. Depth is almost never
shadow; it is a step up the zinc ladder.

### Tonal elevation (dark mode)

| Layer | Surface | Token |
|---|---|---|
| 0 — base | `background` | zinc-950 |
| 1 — card / popover | `card` | zinc-900 |
| 2 — hover / secondary | `secondary` | zinc-800 |
| 3 — floating (menu) | `popover` + border | zinc-900 + zinc-700 border |
| 4 — dialog / sheet | `card` + scrim | zinc-900 + 50% black overlay |

### Tonal elevation (light mode)

| Layer | Surface | Token |
|---|---|---|
| 0 — base | `background-light` | white |
| 1 — card | `card-light` | white (subtle shadow distinguishes) |
| 2 — hover / secondary | `secondary-light` | zinc-100 |
| 3 — floating | `popover` + border | white + zinc-200 border |
| 4 — dialog / sheet | `card` + overlay | white + 40% black overlay |

### Shadow tokens

Shadows are whisper-soft. Opacity ranges 3–5%.

- `shadow-sm`: `0 1px 4px 0 rgb(0 0 0 / 0.03)` — default card lift.
- `shadow` / `shadow-md`: `0 2px 8px 0 rgb(0 0 0 / 0.04)`.
- `shadow-lg`: `0 8px 24px -4px rgb(0 0 0 / 0.05)` — floating menus.
- `shadow-xl`: `0 16px 40px -8px rgb(0 0 0 / 0.05)` — dialogs on dark.

In dark mode, shadows are functionally invisible and elevation is carried
entirely by tonal contrast. Shadows are kept for light mode.

### Borders instead of shadow

Components in dark mode define edges with a translucent border:
`border: 1px solid oklch(from var(--zinc-700) l c h / 0.4)`. Crisper than
shadow on dark backgrounds and mirrors the "light refraction edge" of
premium OLED UI.

### Platform mapping

- **iOS.** `UIBlurEffect` only on sticky bars where scrim is required for
  legibility over media. Materials are not a brand element.
- **Android.** Disable Material 3 tonal elevation overlays. Provide
  `Surface(tonalElevation = 0.dp)` and use the tonal ladder above. Compose
  `Modifier.shadow` only on `lg`/`xl` surfaces.

## Shapes

**Tight radii. Pill-shaped actions. Soft-square containers.** No sharp
(0-radius) corners anywhere in product UI. No fully-rounded containers
either — only actions are pills.

### Radius ladder

- `none` (0px) — reserved for media viewports.
- `xs` (4px) — focus outlines, badges on dense rows.
- `sm` (8px) — inputs, small pills, selects, badges.
- `md` (12px) — popovers, toasts, small modals, icon tiles.
- `lg` (16px) — cards, dialog containers, sheets.
- `xl` (20px) — hero elements, featured media containers.
- `full` (9999px) — buttons, avatars, tags, media overlay pills.

### Rules

- **Actions are always `full` radius.** Primary, secondary, ghost,
  destructive, icon buttons — all pills. The single most recognizable
  shape signal in the system.
- **Containers are always `lg`.** Cards, dialogs, sheets — 16px. Media
  carousels inside cards get `md`.
- **Inputs are `sm`.** 8px. Never pills. An input that looks like a button
  is a bad input.
- **Never mix.** A card should not contain a sharp-cornered element, and
  a pill should not contain a square-cornered affordance.

### Platform notes

- **iOS.** Native system controls (`UISwitch`, `UISegmentedControl`) follow
  Apple's default corner radii. Custom controls match the Desperse ladder.
- **Android.** Material 3 `Shapes` theme overrides all corner families:
  `Shapes(small = 8.dp, medium = 12.dp, large = 16.dp)`.

## Components

Component behavior is fully specified in the YAML front matter. Prose here
covers the "why" and cross-platform nuance.

### Buttons

Pill-shaped, label-lg typography, tight letter-spacing. Three visual tiers
loudest to quietest:

1. **Primary.** Solid fill of the inverted neutral (`zinc-50` in dark mode,
   `zinc-950` in light). One per screen, maximum.
2. **Secondary.** `zinc-800` fill (dark) or `zinc-100` (light). The default
   for any non-hero action.
3. **Ghost.** Transparent, fills on hover with `zinc-800` (dark) or
   `zinc-100` (light). For tertiary actions inside a card.

**Destructive** is reserved for the destructive token (torch-red derivative)
and only for irrevocable actions (delete post, remove follower, revoke
access). Never use it for cancel.

**Size ladder.** 40px (mobile, also the CTA height) → 32px (desktop
density). A 44px "cta" size exists on mobile for the primary commerce
action (Buy, Collect) to meet iOS HIG.

### Post card

The feed atom. Structure: media (full-bleed inside the card), metadata
overlay pill on the media, caption, category pills, action row.

- **Media overlay pill.** Bottom-left of the media. Uses
  `media-pill-tone-*` tokens so the pill color carries the post type.
  Example: an Edition post shows `0.5 SOL` in a magenta pill.
- **Action row.** Like, Comment, Collect/Buy. The action button for
  Collect/Buy inherits the post-type tone; Like and Comment are neutral.
- **Collected state.** "Collected" swaps in place of the action button.
  The media pill remains visible so the post's economic identity is never
  hidden.

### Media pill

Short, mono-tracked label overlaid on media. Three variants: `dark` (the
default for neutral metadata like "3 images"), `muted` (informational state
like "Sold Out"), and `tone` (price and type cues). All pills are 24px tall
— small enough to not dominate media, large enough to be legible on a
phone.

Tone pills use **`zinc-950` text on the tone fill**, not white. Mono pill
type (~10px) on the new dark-tuned magenta/violet would clear AA at white,
but `zinc-950` text reads more authoritative and matches the tonal-system
intent. The rule is consistent across all three post-type tones.

### Inputs

Soft-square (`rounded-sm`), no shadow, 40px tall on mobile / 32px on
desktop. Focus state uses a 2px `ring`-colored ring at 30% opacity. Invalid
state swaps the ring to `destructive`. No validation icons inside the
input — the error message below carries the signal. Placeholder text is
`muted-foreground` weight 400.

### Navigation

- **Sidebar (desktop).** 256px, full-height, sticky. Nav items are
  full-radius pills at 40px tall; hover fills with `zinc-800`, active state
  fills with `sidebar-accent` and swaps the icon to filled.
- **Bottom bar (mobile).** 56px tall. Icon + label-md text. Active state
  changes the icon to its solid Font Awesome variant and raises text to
  `foreground`.
- **Top bar (mobile).** 56px, page title (or logo on home), back button on
  secondary pages, optional trailing action. Solid `background` so
  typography over media stays readable — never a translucent scrim.

### Notifications (unread indicator)

A solid `destructive` dot (4px) or count pill (20px tall). The count caps
at `99+`. Never use purple for unread — that conflates value (purple) with
activity (red).

### Surfaces

- **Dialog.** Centered on desktop (`max-width: 500px`), full-screen sheet
  on mobile. Backdrop is 50% black.
- **Sheet.** Slides from bottom on mobile, from right on desktop.
- **Toast.** Top-right (desktop) / top (mobile). Auto-dismiss 4s; error
  toasts require manual dismiss.
- **Tooltip.** `zinc-900` fill, 8px side padding, 6px vertical. Show after
  400ms hover on pointer devices; long-press equivalent on touch (500ms).

### Icons

**Font Awesome Pro** is the primary icon family — solid for active states
and filled affordances, regular for idle states. Icons inherit text color
— never colored icons except in post-type action buttons where the tone is
the point. Icons are sized via `text-{size}` classes (e.g., `text-base`,
`text-2xl`) — typography utilities (`text-body-md`) must not be applied to
icons; those are paired type tokens, not size scales.

### Focus states

Interactive elements use a 2px `ring`-colored outline with 30% opacity,
offset by 1–2px depending on component density. Buttons and clickable
containers have their browser focus ring suppressed; form inputs,
checkboxes, toggles, and category pills retain rings as the accessibility
floor. Any new interactive element that isn't a `<button>`/`<a>` must opt
into a ring.

## Mobile

Native shells (iOS, Android) and the responsive web all share the same
token set. Follow each platform's HIG for primitives — navigation,
gestures, system controls — and apply Desperse tokens for color,
typography, shape, and motion. **The design adapts; the brand doesn't
bend.**

### Platforms at a glance

**iOS — Apple Human Interface Guidelines**

- 44pt minimum touch target on any tappable element.
- Respect Dynamic Type — root scales with user accessibility settings.
- Use `UIBlurEffect` sparingly; prefer solid Desperse surfaces.
- Edge-swipe back gesture, sheet-style modals, action sheets for
  destructive choices.
- Bundle Figtree as `.ttf` via `UIAppFonts`; San Francisco fallback only
  on bundle failure.

**Android — Material Design (overridden where it conflicts with brand)**

- 48dp minimum touch target (slightly larger than iOS).
- Honor `Configuration.fontScale` for accessibility text sizing.
- Disable Material 3 tonal elevation overlays — we use the tonal ladder
  instead.
- Replace Material ripple with the same `zinc-800` hover the web uses.
- Predictive back (Android 13+), bottom sheets standard + modal,
  edge-to-edge with `WindowInsets`.

### Touch targets

| Platform | Minimum | Use |
|---|---|---|
| iOS | 44 × 44pt | HIG floor for any tappable affordance. |
| Android | 48 × 48dp | Material accessibility minimum. |
| Web (mobile) | 40 × 40px | Acceptable for secondary actions; primary actions and the bottom tab bar hit 48px. |
| Web (desktop) | 32 × 32px | Reserved for power-user surfaces (settings, admin) — never feeds, never primary actions. |

### Safe areas

Every fixed-position element respects `env(safe-area-inset-*)`. Notch,
Dynamic Island, status bar, and home indicator are baked into the layout
— not edge cases.

- Top bars add `env(safe-area-inset-top)` to `padding-top`. Notch and
  Dynamic Island fall inside the inset, never under the bar.
- Bottom tab bars add `env(safe-area-inset-bottom)` to `padding-bottom`.
  Home indicator gets ~6% of the screen below the tabs.
- Full-screen modals apply both insets. Landscape orientation honors
  `safe-area-inset-left`/`right` on iPhone-X-and-after.
- Sticky CTAs over media respect inset-bottom + 12px breathing room above
  the home indicator.
- **Never anchor anything tappable inside the inset zone.** The OS
  reserves those areas for its own gestures.

### Navigation patterns

| Surface | iOS | Android |
|---|---|---|
| Bottom tab bar | `UITabBar`, 56pt + safe-area inset bottom. SF Symbols or custom outline/filled icons. Up to 5 tabs. | `BottomNavigationView`, 56dp + `WindowInsets.navigationBars`. Outline → filled on active. Up to 5 destinations. |
| Top bar | `UINavigationBar`, large title on root, inline title on push. Back button uses chevron-left + previous title. | Material `TopAppBar` (small variant), 56dp + `WindowInsets.statusBars`. Up arrow for back; hamburger only when no parent. |
| Modals | `pageSheet` by default (90% height, drag handle). `formSheet` for compact forms. `fullScreenCover` for immersive flows. | `ModalBottomSheet` for actions; `Dialog` for confirmations; full-screen `Activity` for immersive flows. |
| Action sheets | `UIAlertController` `.actionSheet`. Cancel separated. Destructive in tone-warning role at the top. | `ModalBottomSheet` with list rows. No Cancel — tap-outside or back gesture dismisses. |

### Typography on mobile

The 16px mobile root accommodates iOS Dynamic Type and Android
`fontScale` up to "Accessibility Large" without layout break. Bundle
Figtree on both shells; never rely on the system font for brand
surfaces. The full token-to-platform mapping table lives in the
**Typography** section above.

### Haptics

Haptic feedback is part of the brand on mobile. Used **sparingly to
confirm** — never to entertain. Match the system's vocabulary so the app
feels native rather than custom.

| Event | iOS | Android |
|---|---|---|
| Collect / Buy success | `notificationOccurred(.success)` | `HapticFeedbackConstants.CONFIRM` (R+30) |
| Like toggle | `impactOccurred(.light)` | `GESTURE_END` / `VIRTUAL_KEY` |
| Destructive confirm | `notificationOccurred(.warning)` | `REJECT` (R+30) / `LONG_PRESS` |
| Pull to refresh trigger | `impactOccurred(.medium)` | `GESTURE_END` |
| Error / failed action | `notificationOccurred(.error)` | `REJECT` |

### Gestures

Honor the OS's gesture language. Disabling system gestures is the
fastest way to make a native shell feel like a wrapped webview.

- **Edge-swipe back (iOS).** Always available on push navigation.
  Disabling `interactivePopGestureRecognizer.isEnabled` is the
  most-noticed broken-feel signal on iOS.
- **Predictive back (Android).** Opt in via
  `android:enableOnBackInvokedCallback="true"` on Android 13+. Required
  for the modern look-and-feel; without it the OS-level back animation
  looks legacy.
- **Pull to refresh (both).** Standard primitive on feed and notification
  surfaces. Trigger refetch + medium haptic. Don't reinvent the spinner.
- **Long-press (both).** Reserved for tooltip-equivalent on touch
  (~500ms) and contextual actions (post card → share/copy/report sheet).
  Never the only path to a destructive action.

### Platform overrides

A small set of deliberate deviations from each platform's defaults that
keep Desperse looking like itself across iOS, Android, and web.

- **Android.** Disable Material 3 tonal elevation overlays. Use
  `Surface(tonalElevation = 0.dp)` and rely on the zinc tonal ladder.
  Material's automatic surface lightening collides with our flat layered
  system.
- **Android.** Replace the Material ripple with the same `zinc-800`
  hover/press fill the web uses. Cross-platform consistency wins over
  the Material wave animation.
- **iOS.** Use `UIBlurEffect` only on sticky bars when scrim is required
  for legibility over media. Materials are not a brand element; solid
  `background` beats a blur.
- **iOS.** Native system controls (`UISwitch`, `UISegmentedControl`)
  follow Apple's default radii. Custom controls match the Desperse
  ladder. Don't rebuild the system control if the default works.
- **Both.** Status bar content matches the active theme. Dark mode →
  light status bar content; light mode → dark. Never fix it to one and
  let it look wrong in the other.
- **Both.** Bundle Figtree as a font asset. Roboto / San Francisco
  fallbacks only on bundle failure — never as the primary.

### Mobile do's and don'ts

**Do**

- Match the platform's touch-target minimum (44pt iOS / 48dp Android).
- Respect `env(safe-area-inset-*)` on every fixed surface.
- Honor Dynamic Type and `Configuration.fontScale`.
- Fire haptics on success, error, and destructive confirms — never on
  hover or scroll.
- Use the platform's modal language (sheets, bottom sheets, action
  sheets).
- Test landscape on iPhone — left/right insets matter on
  iPhone-X-and-after.

**Don't**

- Disable edge-swipe back on iOS. Most-noticed broken-feel signal.
- Skip predictive back on Android 13+. Modern UX expectation.
- Use Material ripple. Replace with `zinc-800` fill for cross-platform
  consistency.
- Block tap-targets behind the home indicator or notch.
- Lock orientation to portrait without a content reason. Tablets and
  landscape phones exist.
- Implement features that only work on desktop. Every capability needs
  a mobile path.

## Do's and Don'ts

### Do

- Do treat the post-type tone as semantic. If you introduce a new color
  near a post, ask: does this belong on the standard / collectible /
  edition axis, or is it truly new information?
- Do default to **dark** when making a new component. Build light as a
  counterpart, not a co-equal.
- Do use `primary` for exactly one action per screen. If you can't pick
  one, the hierarchy is wrong.
- Do reach for the **dark-tuned tone tokens** (`tone-edition-dark`, etc.)
  rather than picking a brighter step from the ramp. They are tuned to be
  vibrant on dark canvas.
- Do respect `env(safe-area-inset-*)` on every fixed-position surface.
- Do ship Figtree as a bundled font on native shells. Fall back to
  `-apple-system` / Roboto only if bundling fails.
- Do cap prose at `65ch` line length. Feeds scroll; paragraphs read.
- Do use the 8px grid. Anything off-grid must be a deliberate optical
  correction, not a drift.
- Do match touch-target minimums per platform (44pt iOS / 48dp Android).
- Do honor `prefers-reduced-motion` for all non-essential animation.
- Do use `flush-orange-700` for body text on white. `flush-orange-600`
  is the one exception in the palette that fails AA at body-text scale.

### Don't

- Don't write `oklch()` values inline in components. The seven palettes
  and their tone tokens cover every case — extend the system if you need
  a new value.
- Don't use **pure black** (`#000`) or **pure white** (`#fff`) anywhere
  in product UI. Always use `zinc-950` and `zinc-50` (which are subtly
  tinted in OKLCH).
- Don't use **gradient text**. Use weight or color for emphasis.
- Don't use purple for primary CTAs. Purple is for Editions and
  selection only.
- Don't use side-stripe borders (`border-left: 3px solid accent`) on
  cards, callouts, or list items. The most recognizable AI-design tell.
- Don't nest cards inside cards. Flatten the hierarchy.
- Don't mix radii inside one component. Pill buttons should never sit
  inside a sharp-cornered frame.
- Don't override the four paired typography properties (size, weight,
  line-height, tracking) individually. Pick a utility, use it as-is.
- Don't use more than **two font weights** on a single screen (400 body
  + 600 heading is the normal pair; DM Mono 400 for chain data is the
  rare third).
- Don't use `text-uppercase` on body copy. Caps are for `label-xs` only,
  which auto-applies them.
- Don't apply typography utilities (`text-body-md`) to icons. Use
  `text-{size}` (`text-base`, `text-2xl`) for icon sizing.
- Don't ship icons in a color that isn't the active text color — except
  post-type action buttons, where tone is the point.
- Don't use Material ripple on Android. Replace with the same hover state
  the web uses (`zinc-800` fill) for cross-platform consistency.
- Don't implement features that only work on desktop. Every capability
  must have a mobile path; adapt, don't amputate.
- Don't call `primary-foreground` "white" or `primary` "black" — they
  invert with theme.
- Don't introduce a new external font. Figtree and DM Mono are the only
  families. If a new need emerges, propose a change to this document
  first.
