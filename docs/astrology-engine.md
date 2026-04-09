# Fortune Pocket — Astrology Engine Notes

This document describes the current offline natal-chart engine used by both iOS and Android.

## Scope

- Goal: generate a real local natal chart from birth date, birth time, and birth city
- Platforms: iOS and Android implement matching semantics natively
- Shared inputs:
  - `shared-content/data/astrology/signs.json`
  - `shared-content/data/bazi/cities.json`
- Shared verification:
  - `shared-content/data/astrology/natal_chart_test_vectors.json`

## Input Model

Logical input fields:

- `birthYear`, `birthMonth`, `birthDay`
- `birthHour`, `birthMinute`
- `city`

Current UI requirements:

- Birth date is required
- Birth time is required to calculate the ascendant and houses
- Birth city is required to resolve the IANA time zone and local sidereal time

## Location Strategy

City records live in `shared-content/data/bazi/cities.json`.

Each city contains:

- `latitude_north`
- `longitude_east`
- `time_zone_id`
- `utc_offset_hours`

Rules:

- `time_zone_id` is an IANA time zone and is the source of truth for DST behavior
- `utc_offset_hours` is kept as a human-readable legal-offset reference for UI/debugging
- Civil birth time is restored with the city's IANA time zone before the chart is calculated

## Core Chart Calculation

### Time Base

- The engine converts local birth date/time plus city time zone into an absolute instant
- Julian Day is then derived from that instant
- All planetary and angular calculations are based on that Julian Day

### Planetary Positions

The engine computes:

- Sun
- Moon
- Mercury
- Venus
- Mars
- Jupiter
- Saturn
- Uranus
- Neptune
- Pluto

Implementation notes:

- Sun and Moon use low-precision astronomical formulae suitable for offline consumer use
- Mercury through Neptune use low-precision heliocentric orbital elements transformed into geocentric longitude
- Pluto uses a lightweight sampled approximation
- Retrograde state is inferred by comparing longitudes around the birth instant

This is intentionally not a Swiss Ephemeris class implementation. It is a fully offline natal-chart engine designed for product use, not for professional observatory-grade ephemeris work.

### Signs

- Sign assignment uses tropical zodiac boundaries
- Each sign spans `30°`
- `0° Aries` is the start of the tropical zodiac

### Ascendant

- The ascendant is computed from:
  - Julian Day
  - local sidereal time
  - obliquity of the ecliptic
  - city latitude

### Houses

- The current house system is `Equal House`
- House 1 begins at the ascendant
- Each subsequent house advances by `30°`

This choice keeps the engine deterministic, easy to verify cross-platform, and lightweight enough for a fully local app.

### Aspects

The engine currently surfaces major aspects only:

- conjunction `0°`
- sextile `60°`
- square `90°`
- trine `120°`
- opposition `180°`

Each aspect has a fixed orb:

- conjunction `8°`
- sextile `4.5°`
- square `6°`
- trine `6°`
- opposition `8°`

The app sorts aspects by:

- whether personal planets are involved
- then by exactness

The result list is capped to the top `6` major aspects for product readability.

## Interpretation Layer

The prose shown to users is not random text.

The interpretation layer reads:

- Sun sign
- Moon sign
- Rising sign
- Venus / Mars / Mercury / Jupiter / Saturn placements
- dominant element balance
- top house focus
- top major aspects

It then generates:

- chart signature
- chart summary
- overall profile
- love / intimacy
- career / direction
- wealth / security
- social / communication
- guidance

This means the product is chart-driven, but the prose remains productized and readable rather than attempting to imitate a full professional astrological consultation.

## Shared Verification

Shared vectors live in `shared-content/data/astrology/natal_chart_test_vectors.json`.

They pin:

- Sun sign
- Moon sign
- Rising sign
- top house-focus order
- all `10` planet placements
- top `6` major aspects

Automated tests:

- Android:
  - `android/core/core-content/src/test/java/com/fortunepocket/core/content/astrology/AstrologyChartCalculatorTest.kt`
- iOS:
  - `ios/PocketOracleTests/AstrologyChartCalculatorTests.swift`

## Current Limitations

- No external ephemeris dependency
- No asteroid, Chiron, lunar node, or vertex calculations
- Equal-house system only
- No transit, synastry, or progressed-chart support yet
- Precision is intentionally “product-grade offline astrology,” not “professional ephemeris + multiple house systems”

## Practical Product Guidance

- This engine is strong enough for an offline natal-chart feature with stable, explainable results
- It should be described as an `offline natal chart` or `local astrology chart`
- It should not be marketed as a substitute for professional astrology software with high-precision ephemerides and multiple house-system options
