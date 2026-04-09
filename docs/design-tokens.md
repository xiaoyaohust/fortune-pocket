# Fortune Pocket — Design Tokens

Single source of truth for brand visual language. Both iOS and Android should follow these values.

## Color Palette

| Token | Hex | Usage |
|---|---|---|
| `background-deep` | `#0E0A1F` | Primary background (darkest) |
| `background-base` | `#1A1035` | Card & surface background |
| `background-elevated` | `#251848` | Elevated card, sheet |
| `accent-gold` | `#C9A84C` | Primary accent, CTA |
| `accent-gold-light` | `#E8D48B` | Gradient end, highlights |
| `accent-purple` | `#7B5EA7` | Secondary accent |
| `accent-rose` | `#C9748F` | Love / emotion themes |
| `text-primary` | `#F0EBE3` | Main text |
| `text-secondary` | `#9E96B8` | Subtitle, metadata |
| `text-muted` | `#5D5580` | Placeholder, disabled |
| `divider` | `#2E2456` | Separator lines |
| `star-glow` | `#FFE566` | Star/sparkle elements |

## Gradients

```
Gold gradient:    #C9A84C → #E8D48B  (linear, 135°)
Purple gradient:  #1A1035 → #2D1B69  (linear, 180°)
Card shimmer:     #251848 → #1A1035  (linear, 180°)
```

## Typography

### iOS (SF Pro)
- Display: SF Pro Display, weight 300 (Light), 28-34pt
- Title: SF Pro Display, weight 400 (Regular), 20-24pt
- Headline: SF Pro Text, weight 600 (Semibold), 16-18pt
- Body: SF Pro Text, weight 400 (Regular), 15-16pt
- Caption: SF Pro Text, weight 400 (Regular), 12-13pt
- Accent text (card names, quotes): Georgia / Serif fallback

### Android (Material 3 Type Scale)
- displayLarge: 34sp, Light (300)
- headlineMedium: 24sp, Regular (400)
- titleLarge: 20sp, Medium (500)
- bodyLarge: 16sp, Regular (400)
- bodyMedium: 14sp, Regular (400)
- labelSmall: 11sp, Medium (500)

## Spacing (8pt Grid)

| Token | Value |
|---|---|
| `xs` | 4pt/dp |
| `sm` | 8pt/dp |
| `md` | 16pt/dp |
| `lg` | 24pt/dp |
| `xl` | 32pt/dp |
| `xxl` | 48pt/dp |

## Corner Radius

| Token | Value | Usage |
|---|---|---|
| `sm` | 8pt/dp | Tags, chips |
| `md` | 16pt/dp | Cards, buttons |
| `lg` | 24pt/dp | Sheets, large cards |
| `full` | 9999pt/dp | Pills, avatars |

## Iconography

- iOS: SF Symbols 5.0 (system icons)
- Android: Material Symbols Outlined
- Custom: Unicode symbols for tarot (see cards.json `symbol` field)

## Tarot Card Programmatic Design

Each card uses:
- `color_primary`: background fill
- `color_accent`: symbol / ornament color
- `symbol`: text/unicode rendered as large centered element
- Border: 1.5pt stroke, `accent-gold` at 40% opacity
- Corner radius: 12pt/dp
- Aspect ratio: 2:3 (width:height)
