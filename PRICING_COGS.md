# Overage Rate COGS Validation (Risk 3b / task #12)

**Status:** recommendation — rate card NOT yet ratified. **Date:** 2026-06-07.
**Scope:** validate the two proposed overage rates against real marginal cost.

No dollar rates are hardcoded in `src/` — the code collects only counts/bytes
(`UsageSnapshot`; premium generations via the append-only `PremiumPackageEvent`
ledger, read through `PremiumPackageEventRepository` / `PremiumOverageGuard`),
and a billing layer applies rates. `$0.18`/`$0.40` below are proposed rate-card
numbers.

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
- **Premium rail — FIXED (per-period flow), delete-proof.** Each billable
  premium generation is recorded as an immutable `PremiumPackageEvent` (the
  append-only ledger), so deletes can't lower a counter. The billing meter counts
  events **generated within the period** (half-open `[start, end)` on
  `generatedAt`): `UsageSnapshot.premiumPackagesGenerated` is populated by
  `UsageAggregationService.capture` via `PremiumOverageGuard.countGeneratedInPeriod`.
  Billing sums these across the window, so a one-time generation is billed once
  and never re-billed; a tenant with no new generations this period is billed $0
  for premium (tested). The **spend-cap guard** (`PremiumOverageGuard`) counts the
  ledger for the **current billing period (calendar month)** too, so the included
  bundle resets per period (a tenant is not capped forever).

The included-bundle / overage-cap **defaults in code are provisional**
(`PremiumOverageGuard` 122-138, `TenancyApiImpl.defaultStorageOverageBytes`
120-123) — ratify under COGS sign-off; keep rates in the billing layer.

## 4. Resolved: package-generation bytes are NOT billable storage (task #15)

**Decision: NO.** A generated AIP/DIP artifact does **not** count toward the
tenant's billable storage; the **$0.40/package** generation fee is all-in and
covers the artifact's whole lifecycle, including its storage.

Rationale:

- **No double-charge.** Billing both $0.40/pkg *and* $0.18/GB-month for the same
  bytes would charge the same object twice. The generation fee already carries a
  ~71,000× margin over marginal cost, so the artifact's storage COGS is absorbed.
- **Negligible COGS.** Generated artifacts are NOARK 5.5 / E-ARK **metadata
  manifests** (the serialized package descriptor), typically KBs–low MBs. At
  $0.18/GB-month a 10 MB manifest is ~$0.0018/month — not worth a second meter or
  the customer confusion of a storage line item appearing after a generation.
- **No duplication of content.** The archived *content* (the actual documents)
  are separate `Document` uploads that **are** already metered. The manifest is
  not a copy of that content, so it is not "missing" revenue.
- **Clean rails.** Storage metering (`SUM(Document.fileSize)` over billable
  documents) stays "user-uploaded content"; premium generation is its own
  per-event rail. The two do not overlap.

Mechanically this needs no billing change: `AbstractPackageGenerator.generate`
uploads the artifact via `CloudStorageService.uploadBytes` and creates **no**
`Document` row, so the bytes never enter the storage meter. That is intentional
and is noted at the upload seam so it is not "fixed" by accident.

If a future plan wants to recover artifact storage COGS, prefer raising the
per-package fee over adding a second meter on the same bytes.
