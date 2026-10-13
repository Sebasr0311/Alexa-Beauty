# Production readiness evidence — 2026-10-13

## Decision

Application hardening is implemented locally. Public launch remains blocked on environment configuration and staging evidence; no deployment, production database change, commit, or push was performed. There is **no payment gateway**. Staff independently verifies cash/transfers and records the exact payment through the protected admin panel.

## Implemented controls

- Public catalog, product details and admin views use real API data, not demonstration records on failure. Persisted carts contain real variant UUIDs; empty carts stay empty.
- Manual payments reject zero/partial amounts, elapsed reservations, inconsistent reserved stock and invalid order states. Row locks and verified-payment replay prevent duplicate stock consumption.
- Checkout records a database-backed idempotency key and request fingerprint (additive Flyway V4). Replays recover the same tracking token; different requests using the same key are rejected. A stable settings-row lock serializes checkout/reference allocation across replicas.
- Browser API calls use the same-origin Next.js rewrite `/api/backend/*`. Admin login is in AdminShell, not a separate `/admin/login` route. HttpOnly session cookies replace browser token storage. Production uses Secure/Lax cookies and strict exact-origin/non-simple-header checks for writes.
- Production startup rejects missing/placeholder credentials, non-HTTPS origins, demonstration WhatsApp numbers, insecure database URLs and origin trailing slashes. First production bootstrap disables the seeded development administrator; subsequent restarts do not reset the configured account.
- Image uploads are decoded/re-encoded JPEG/PNG with byte/dimension limits. Actuator exposes health only. Production Swagger is disabled.
- Production instance-wide login/checkout budgets are bounded without untrusted forwarded-IP interpretation. Trusted edge per-client/distributed protection is still an operational requirement.
- Order confirmation no longer supplies a demonstration WhatsApp fallback or claims successful reservation before a real tracking response.

## Executed evidence

| Check | Result | Scope |
|---|---|---|
| Java 21 / Maven `test` | 61 tests, zero failures/errors/skips | H2 PostgreSQL compatibility mode; not PostgreSQL |
| Maven `package` | Successful; executable Spring Boot JAR produced, all 61 tests repeated successfully | Java 21, local build; no container or cloud release |
| Next.js `npm run build` | Successful, including TypeScript checking; 14 routes | Isolated `.next-production-check`, synthetic HTTPS backend target; not an external connectivity proof |
| API lifecycle regression | PASS | Catalog → HTTP checkout → identical retry → conflicting retry → manual EFECTIVO confirmation → inventory → tracking → XLSX containing the same order reference |
| Manual payment regression | PASS | Partial amount, elapsed reservation before worker, inconsistent reserved stock, duplicate and concurrent confirmation |
| Cookie-auth integration | PASS | HttpOnly cookie authenticates `/auth/me`, untrusted-origin logout rejected, trusted logout clears cookie |
| Actual Next rewrite smoke | PASS | Isolated H2 backend 8089 / Next dev 3009: proxied health 200, anonymous admin 401, foreign-origin login 403 |
| Runtime successful proxy login | PASS locally | Chromium desktop/mobile through Next same-origin rewrite, isolated PostgreSQL synthetic account; HttpOnly/Lax cookie, no browser token storage, logout clears cookie. Secure flag requires HTTPS staging |
| PostgreSQL concurrency/migrations | PASS locally | PostgreSQL 14.24 native isolated home cluster on 127.0.0.1:55439. Maven postgresql-verification profile: 61 tests, zero failures/errors/skips, repeated on a fresh database; Flyway V1-V4 successful |
| Desktop/mobile browser and workbook reconciliation | PASS locally | Chromium 1440x1000 / 390x844: real UUID checkout, reload persistence, empty cart, manual cash/Nequi confirmation, stock 5 to 4 and reserved 0, XLSX download, API 503 error state. Both downloaded workbooks have five sheets and matching order references |
| Local PostgreSQL backup/restore | PASS | pg_dump custom format and pg_restore to a distinct isolated database; all 19 public tables match row counts and sorted row-content digests (snapshot: 34 orders, 17 payments). Synthetic fixtures include deliberately inconsistent regression records; this is exact restore proof, not a clean catalog certificate |
| Production deployment/TLS/storage | NOT PROVEN | No hosting credentials or external staging deployment used; local HTTP cannot prove production Secure cookies or external storage |

Commands were executed with process-only `JAVA_HOME=C:/Users/JUAN/.jdks/ms-21.0.11` and Maven 3.9.9. Test XML evidence is in `backend/target/surefire-reports/` (generated, ignored). Local ignored logs retain bounded execution evidence. Earlier smoke servers were stopped; the latest validation used separately isolated PostgreSQL/backend/frontend processes. No existing PostgreSQL services on 5432/5433 were changed.

## TDD evidence and limitations

- Earlier worker's payment reproducer reported HTTP 200 for partial/expired payments before its fix; the corresponding regression tests now pass. This is inherited evidence, not a newly repeated old-code run.
- Rate-limit test referenced the missing filter and compiled RED (`cannot find symbol PublicWriteRateLimitFilter`); minimal implementation made it GREEN.
- Exact-origin validation test executed RED (`Expecting code to raise a throwable`) for an HTTPS origin with trailing slash; rejecting nonempty URI paths made it GREEN.
- Confirmation contact source check executed RED on the hardcoded demonstration phone number; configured contact lookup removes that literal. This is a structural regression, not a browser interaction test.
- No coverage instrumentation was configured or executed; **80% coverage is not claimed**. The later local browser journey is recorded above; no live cloud rehearsal is claimed. New lifecycle/cookie tests provide additional regression coverage over implemented behavior, not fabricated historical RED evidence.
- No external dependency vulnerability scan was run; require an approved dependency/advisory check before release.
- No checkpoint commits were made because this work was explicitly kept uncommitted for independent inspection.

## Scoped expiration correction after independent inspection

Independent inspection identified an actual concurrency blocker: expiration loaded pending order state before variant locks, without an order lock. Two expirers could both release the same quantity, stealing another order's reservation; a waiting expiry could overwrite a paid order.

- RED: `concurrentExpirationReleasesOnlyItsOwnActiveReservation` executed against the old code with a shared variant lock held. Expected two units still reserved for the other order; actual zero. The first attempt had an oversized fixture SKU, corrected before recording the valid business failure.
- RED: `expirationDoesNotReleaseFutureReservation` showed a not-yet-due order incorrectly changed to EXPIRADO.
- Correction: discovery now returns scalar IDs only. A separate Spring `OrderExpirationWorker.expire` uses REQUIRES_NEW per ID, obtains a fresh PESSIMISTIC_WRITE order lock before sorted variant locks, and checks current status, deadline, active reservation totals and sufficient reserved inventory. It releases exact active quantities without clamping; failures roll back only that order's independent transaction. The caller no longer relies on self-invoked transactional methods or previously loaded entities.
- GREEN: 19 targeted checkout/payment tests pass, including both reproducers and `expirationWaitingForPaymentCommitCannotOverwritePaidOrder`. That test holds a manual payment transaction open while expiry starts; after payment commits, expiry preserves PAGADO, consumed reservations and stock reduced exactly once.
- Existing expiry fixtures now set an actual past deadline before explicitly requesting expiration. History evidence uses an accurate nonfixed-duration description.
- These races originally used H2. They now also pass on PostgreSQL 14.24. The payment/expiry test assigns a deadline two seconds ahead, holds the successful payment transaction open until the deadline has elapsed, starts expiration, then releases the payment commit and checks PAGADO and exactly-once stock. This proves the real elapsed-deadline locking case, not equality to a frozen injected clock.

## Required launch gates

1. Inject real production variables documented in `.env.example` and the deployment guide; configure final exact HTTPS origin and real WhatsApp number.
2. Verify the configured administrator logs in through the deployed same-origin rewrite and logs out; validate Secure cookie flags in HTTPS staging.
3. Repeat the now-passing PostgreSQL migration/concurrency rehearsal against the exact PostgreSQL version and deployment configuration chosen for staging; local verification used version 14.24, not the final hosting environment.
4. Repeat the now-passing desktop/mobile browser journey against HTTPS staging. Empty cart and API outage are covered locally; externally hosted empty-inventory behavior, storage upload permissions and full production routing remain to be checked.
5. Populate approved catalog/prices/stock and agree delivery costs: current order shipping fee is zero. Coordinate any uncharged shipping separately rather than representing it as paid.
6. Validate Supabase upload permissions, TLS, edge rate limits, health alerts, backup restoration, previous release rollback and operational ownership before inviting customers.

The historical `INFORME_FINAL_VERIFICACION.md` is superseded as release evidence; its PASS claims do not prove these outstanding gates.
