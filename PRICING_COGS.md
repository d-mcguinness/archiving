# Overage Rate COGS Validation (Risk 3b / task #12)

**Status:** recommendation — rate card NOT yet ratified. **Date:** 2026-06-07.
**Scope:** validate the two proposed overage rates against real marginal cost.

No dollar rates are hardcoded in `src/` — the code collects only counts/bytes
(`UsageSnapshot`, `PremiumPackageUsageRepository`), and a billing layer applies
rates. `$0.18`/`$0.40` below are proposed rate-card numbers.

## 1. Marginal COGS (us-east-1, June 2026)

| Item | Marginal COGS | Basis |
|---|---|---|
| 1 GB-month — S3 Standard (multi-AZ) | **$0.023** | first-50 TB tier ($0.022 / $0.021 above) |
| 1 GB-month — S3 Standard-IA (multi-AZ, cold) | **$0.0125** | + ~$0.01/GB retrieval, 30-day min, 128 KB min object |
| 1 GB-month — S3 One Zone-IA | $0.01 | single-AZ — NOT a sole copy of irreplaceable archives |
| One premium-package generation | **~$5.6e-6** | one S3 PUT ($5e-6, ~90%) + sub-second compute (~$6e-7) |

Premium path = one in-memory serialize (Jackson/StAX) + one `uploadBytes` PUT
(`AbstractPackageGenerator.generate`); no multipart/transcoding.

> Provenance caveat: the $0.023/$0.022/$0.021 tiers were not read off the
> JS-rendered AWS page; they come from third-party pages (costimizer.ai,
> cloudzero.com, May-2026) + model knowledge. **Confirm $0.023 via the live AWS
> table or `aws pricing get-products` before locking the card** — everything
> cascades off it. COGS is storage-only; all-in must load replication, backups,
> index/metadata, retrieval egress, support.

## 2. Rate verdicts

**Storage overage — $0.18/GB-month: KEEP** (anchored to multi-AZ S3 Standard).
~7.8x raw Standard ($0.023) ≈ 87% gross margin — a sane single-digit-x SaaS
multiple, not punitive, once replication/backup/metadata/egress/support/margin
are loaded. Correct unit: storage is metered as a monthly stock
(`SUM(fileSize) WHERE billable=true`), matching $/GB-**month**.
- **State the storage class on the card** — defensibility swings ~8x (Standard)
  → ~14x (Standard-IA) → 50–180x (Glacier).
- **Recommended add:** a Cold/Archive tier on S3 Standard-IA ($0.0125 COGS)
  priced ~$0.10–$0.12/GB-month to win latency-tolerant preservation volume at
  similar margin (keep multi-AZ; never One Zone-IA as the sole copy).

**Premium package — $0.40/package: KEEP, strictly value-based.** ~71,000x
marginal cost — there is no cost-plus story; price on the compliance value of
Noark 5 / E-ARK conformance and **never present it as cost-recovery**.

## 3. Blocking metering gap before billing

- **Storage rail — OK.** Point-in-time monthly stock; bill $0.18/GB-month on the
  snapshot. *Refinement (not a blocker):* derive time-weighted GB-month by
  averaging the daily `UsageSnapshot` rows for mid-month churn fairness.
- **Premium rail — FIXED (per-period flow).** The billing meter now counts
  premium packages **generated within the period** (half-open `[start, end)` on
  `createdAt`): `UsageSnapshot.premiumPackagesGenerated` is populated by
  `UsageAggregationService.capture` via
  `Aip/DipService.countByTenantAndStandardsGeneratedIn`. Billing sums these
  across the window, so a one-time generation is billed once and never
  re-billed. A tenant with no new generations this period is billed $0 for
  premium (tested). The **cumulative** count
  (`countByTenantIdAndStandardInAndBillableTrue`) remains, but only drives the
  **lifetime spend-cap guard** (`PremiumOverageGuard`), not the bill.

The included-bundle / overage-cap **defaults in code are provisional**
(`PremiumOverageGuard` 122-138, `TenancyApiImpl.defaultStorageOverageBytes`
120-123) — ratify under COGS sign-off; keep rates in the billing layer.

## 4. Open question

Confirm whether package-generation bytes **also** count toward billable storage.
If yes, a tenant pays $0.40 to generate **and** $0.18/GB-month to store the same
bytes — verify that is intended, not double-counting (relates to task #15).
