# Fortune Pocket — Data Schema Reference

All content data lives in `shared-content/data/`. Both iOS and Android read from the same JSON files.

For engine notes and shared test-vector setup, see:

- `docs/astrology-engine.md`
- `docs/bazi-engine.md`

## Design Principles

- All user-facing text fields exist in both `_zh` (Chinese) and `_en` (English) variants
- Arrays use consistent snake_case keys
- `version` field on root objects enables future migration
- Numeric IDs use string format for cross-platform safety

---

## tarot/cards.json

```json
{
  "version": "1.0",
  "total": 22,
  "cards": [
    {
      "id": "major_00",           // string, unique
      "number": 0,                 // int
      "arcana": "major",           // "major" | "minor"
      "name_zh": "愚者",
      "name_en": "The Fool",
      "symbol": "◦",              // Unicode symbol for programmatic rendering
      "color_primary": "#F5D76E", // hex, card background
      "color_accent": "#82E0AA",  // hex, symbol color
      "keywords_upright_zh": ["新开始", "自由"],
      "keywords_upright_en": ["new beginnings", "freedom"],
      "keywords_reversed_zh": ["鲁莽", "混乱"],
      "keywords_reversed_en": ["recklessness", "chaos"],
      "meaning_upright_zh": "...",
      "meaning_upright_en": "...",
      "meaning_reversed_zh": "...",
      "meaning_reversed_en": "...",
      "energy_upright": "light",   // "light" | "shadow" | "neutral"
      "energy_reversed": "shadow"
    }
  ]
}
```

## tarot/spreads.json

```json
{
  "version": "1.0",
  "spreads": [
    {
      "id": "three_card",
      "name_zh": "三牌阵",
      "name_en": "Three Card Spread",
      "card_count": 3,
      "positions": [
        { "index": 0, "label_zh": "过去", "label_en": "Past", "aspect": "past" },
        { "index": 1, "label_zh": "现在", "label_en": "Present", "aspect": "present" },
        { "index": 2, "label_zh": "未来", "label_en": "Future", "aspect": "future" }
      ]
    }
  ]
}
```

## astrology/signs.json

```json
{
  "version": "1.0",
  "signs": [
    {
      "id": "aries",
      "name_zh": "白羊座",
      "name_en": "Aries",
      "symbol": "♈",
      "element": "fire",          // "fire"|"earth"|"air"|"water"
      "quality": "cardinal",      // "cardinal"|"fixed"|"mutable"
      "ruling_planet_zh": "火星",
      "ruling_planet_en": "Mars",
      "date_range": {
        "start_month": 3, "start_day": 21,
        "end_month": 4, "end_day": 19
      },
      "color_primary": "#E74C3C",
      "color_accent": "#F39C12",
      "traits_zh": ["热情", "勇敢"],
      "traits_en": ["passionate", "brave"],
      "strengths_zh": ["行动力强", "充满热情"],
      "strengths_en": ["action-oriented", "enthusiastic"],
      "weaknesses_zh": ["急躁", "缺乏耐心"],
      "weaknesses_en": ["impatient", "impulsive"],
      "description_zh": "...",
      "description_en": "..."
    }
  ]
}
```

## astrology/daily-templates.json

```json
{
  "version": "1.0",
  "templates": {
    "overall_zh": ["今日能量{quality}，适合{action}。", "..."],
    "overall_en": ["Today's energy is {quality}, perfect for {action}.", "..."],
    "love_zh": ["..."],
    "love_en": ["..."],
    "career_zh": ["..."],
    "career_en": ["..."],
    "wealth_zh": ["..."],
    "wealth_en": ["..."],
    "social_zh": ["..."],
    "social_en": ["..."],
    "advice_zh": ["..."],
    "advice_en": ["..."]
  },
  "variables": {
    "quality_zh": ["平稳流畅", "活力充盈", "灵感迸发", "沉静内敛"],
    "quality_en": ["steady and smooth", "full of vitality", "inspired", "calm and introspective"],
    "action_zh": ["专注手头工作", "与朋友深谈", "整理思路", "休息与充电"],
    "action_en": ["focus on current tasks", "connect with friends", "organize your thoughts", "rest and recharge"]
  }
}
```

Note:

- This file is now a legacy MVP artifact
- The current astrology feature uses the local natal-chart engine instead of the old daily-template generator

## astrology/natal_chart_test_vectors.json

```json
{
  "version": "1.0",
  "cases": [
    {
      "id": "beijing_morning",
      "birth_date": "1992-11-15",
      "birth_time": "08:45",
      "birth_city_id": "beijing",
      "expected": {
        "sun_sign_id": "taurus",
        "moon_sign_id": "cancer",
        "rising_sign_id": "sagittarius",
        "house_focus": [6, 2, 3],
        "placements": [
          {
            "planet_id": "sun",
            "sign_id": "taurus",
            "house": 6,
            "retrograde": false
          }
        ],
        "major_aspects": [
          {
            "first_planet_id": "sun",
            "second_planet_id": "pluto",
            "type": "opposition"
          }
        ]
      }
    }
  ]
}
```

## bazi/stems.json

```json
{
  "version": "1.0",
  "stems": [
    {
      "id": "jia",
      "name_zh": "甲",
      "name_en": "Jiǎ",
      "element": "wood",
      "polarity": "yang",
      "personality_zh": "...",
      "personality_en": "...",
      "love_zh": "...",
      "love_en": "...",
      "career_zh": "...",
      "career_en": "...",
      "wealth_zh": "...",
      "wealth_en": "..."
    }
  ]
}
```

## quotes/daily-quotes.json

```json
{
  "version": "1.0",
  "quotes": [
    {
      "id": "q001",
      "text_zh": "每一次翻牌，都是与自己内心的一次对话。",
      "text_en": "Every card drawn is a conversation with your inner self.",
      "category": "tarot"       // "tarot"|"astrology"|"bazi"|"general"
    }
  ]
}
```

## lucky/colors.json

```json
{
  "version": "1.0",
  "colors": [
    { "id": "c001", "name_zh": "深紫", "name_en": "Deep Purple", "hex": "#6C3483" }
  ]
}
```

---

## ReadingRecord (persisted locally)

Both platforms store history using this logical structure:

```
id          : UUID string
type        : "tarot" | "astrology" | "bazi"
createdAt   : ISO 8601 timestamp
title       : string (localized at write time)
summary     : string (short, 1-2 sentences)
detailJSON  : string (full result serialized as JSON)
schemaVersion : int (currently 1)
isPremium   : bool (reserved for future)
```
