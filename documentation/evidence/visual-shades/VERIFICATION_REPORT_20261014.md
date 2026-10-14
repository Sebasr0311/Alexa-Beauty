# Visual Shade Selector — Local Verification Report

Date: 2026-10-14 (America/Bogota)
Branch: `feature/visual-shade-selector`
Scope: Local visual shade selector, variant management, inventory/order compatibility, and migration preservation. No production deployment or merge.

## Verified results

| Check | Result |
| --- | --- |
| Backend `mvn -B -Ppostgresql-verification clean test` with Java 21 | PASS — 69 tests, 0 failures, 0 errors, 0 skipped |
| `VisualShadeIntegrationTest` | PASS — 7 tests |
| `VisualShadeMigrationTest` | PASS — V4-to-V5 preserved historical fixtures and existing columns across 18 tables |
| Frontend `npm test` | PASS — 3 tests |
| Frontend `tsc --noEmit --incremental false` | PASS |
| Frontend `npm run build` with an isolated dist directory | PASS — Next.js 15.5.27, 14/14 pages generated |
| Browser acceptance evidence from earlier on 2026-10-14 | PASS recorded in `browser-final-20261014.log` (not rerun in this check) |
| `git diff --check` before temporary-resource cleanup | PASS |

### Isolated database test setup

Tests were rerun against a local PostgreSQL 18 verification instance on `127.0.0.1:55441`, using newly created, empty databases `shade_tests_recheck_20261014` and `shade_migration_recheck_20261014`. Production data and production database migrations were not accessed or modified.

An initial rerun against the *previously populated* test database failed (6 assertions, 4 errors) because test fixtures persist between runs: duplicated SKUs, additional seeded categories/brands, and cumulative sales. This was an environment/test-isolation issue. Repeating the complete test suite against fresh databases passed, without changes to application code.

The test cluster has PostgreSQL 18.6; Flyway warns that this server is newer than the database version on which that Flyway release was tested. Passing local migration tests does not establish compatibility with every production PostgreSQL version.

### Evidence and operational notes

- Passing recheck log: `backend-cleanrecheck-20261014.log`.
- Failed populated-database recheck: `backend-tests-recheck-20261014.log`.
- Earlier completed browser verification: `browser-final-20261014.log`.
- Earlier frontend test and build evidence: `frontend-tests-20261014.log`, `frontend-build-20261014.log`.
- Fresh frontend test, typecheck, and production build also passed during this recheck.
- The automatic `tsconfig.json` include change introduced by the isolated Next.js build was reverted to its precheck state.
- Cleanup of the two recheck databases and `.next-shades-recheck` build folder was attempted but the Codex runtime rejected the command with `blocked by policy`; those temporary verification resources may remain. No alternative deletion route was attempted.
- No commit, push, merge, or production deployment was executed as part of this verification.

## Next production gate

Prior to any authorized deployment, review the V5 database migration and perform a production-like staging smoke test for variant creation/editing, image upload, stock exhaustion, and checkout. Do not apply V5 to live data without a verified backup and explicit release approval.
