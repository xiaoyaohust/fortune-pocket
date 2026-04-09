# Fortune Pocket — Bazi Engine Notes

This document describes the current offline Bazi engine used by both iOS and Android.

## Scope

- Goal: provide a locally computed four-pillar chart with explainable rules
- Platforms: iOS and Android implement the same semantics natively
- Shared inputs: `shared-content/data/bazi/*`
- Shared verification: `shared-content/data/bazi/test_vectors.json`

## Input Model

Logical input fields:

- `birthYear`, `birthMonth`, `birthDay`
- `birthHour`, `birthMinute`
- `city`
- `gender`
- `useTrueSolarTime`
- `distinguishLateZiHour`

Precision tiers exposed in UI:

1. Date only
   - Computes year, month, and day pillars
   - Hour pillar remains unknown
   - Best treated as day-level guidance
2. Civil time
   - Uses local clock time plus city time-zone rules
   - Supports minute-level hour pillar
3. True solar time
   - Starts from city civil time
   - Adds longitude correction and equation of time
   - Intended for users who want a more traditional solar-time-based chart

## Location Strategy

City records live in `shared-content/data/bazi/cities.json`.

Each city contains:

- `longitude_east`
- `time_zone_id`
- `utc_offset_hours`

Rules:

- `time_zone_id` is an IANA time zone and is the source of truth for DST behavior
- `utc_offset_hours` is a standard legal-offset reference used for display/debugging and to avoid historical local-mean-time drift in old Chinese dates
- When a city is selected, the engine resolves legal birth time using:
  - the city's standard legal offset
  - plus DST derived from IANA zone rules at the birth instant
- When no city is selected, the UI makes the fallback explicit:
  - `Asia/Shanghai / UTC+8`
  - no DST
  - true solar time disabled

This fallback is acceptable for common mainland-China use, but should not be treated as exact for overseas births or DST-sensitive cases.

## Pillar Calculation

### Year Pillar

- The year pillar is determined by the exact `立春` transition
- Dates before the exact `立春` instant still belong to the previous Bazi year
- This is not based on January 1

### Month Pillar

- Month branch is determined by solar-term boundaries, not Gregorian month numbers
- Month stem follows the standard `寅月起干` rule from the year stem

### Day Pillar

- Day pillar uses a Julian Day Number anchor
- Reference:
  - `2000-01-07 = 甲子日`
  - `JDN = 2451551`
- Formula:
  - `dayIndex = (JDN - 2451551) mod 60`
  - `stem = dayIndex mod 10`
  - `branch = dayIndex mod 12`

### Hour Pillar

- Hour branch is mapped from local birth hour
- If `distinguishLateZiHour = true`, then `23:00–23:59` is treated as the next day for day-pillar purposes
- Hour stem is derived from the day stem and hour branch

## True Solar Time

When enabled, the engine converts local civil time into local apparent solar time.

Formula:

`true solar time = UTC + longitude * 4 minutes + equation of time`

Implementation notes:

- Longitude correction aligns civil time with local meridian offset
- Equation of time uses a NOAA-style approximation
- Solar-term solving and true-solar-time correction are independent:
  - solar terms are solved from astronomical longitude
  - true solar time affects local day/hour interpretation

## Major Cycles

- Direction is derived from year-stem polarity and gender
- Start age is based on the time distance to the nearest relevant solar term
- Approximation used:
  - `start age = days to relevant term / 3`

## Shared Data Files

Main files:

- `shared-content/data/bazi/cities.json`
- `shared-content/data/bazi/hidden_stems.json`
- `shared-content/data/bazi/stems.json`
- `shared-content/data/bazi/branches.json`
- `shared-content/data/bazi/personality-templates.json`
- `shared-content/data/bazi/test_vectors.json`

## Automated Verification

Android:

- Tests: `android/core/core-content/src/test/java/com/fortunepocket/core/content/bazi/BaziCalculatorTest.kt`
- Gradle resource wiring points test resources at `shared-content/data/bazi`

iOS:

- Tests: `ios/PocketOracleTests/BaziCalculatorTests.swift`
- Xcode test target reads the same shared JSON vectors

Current automated checks cover:

- shared vector pillar expectations
- IANA/DST resolution
- true-solar-time correction beyond longitude-only adjustment
- late-Zi day rollover behavior

## Current Limitations

- City selection is curated, not full geocoding
- No latitude-based refinements are used
- Interpretation text remains productized and simplified; the charting engine is stricter than the prose layer
- The no-city fallback is explicit but still approximate

## Practical Product Guidance

- If the user knows only the birth date, keep the result framed as broad reference
- If the user knows exact time, strongly encourage city selection
- If the user is near:
  - `立春`
  - solar-term boundaries
  - `23:00–23:59`
  - DST transitions
  then city and minute precision matter much more
