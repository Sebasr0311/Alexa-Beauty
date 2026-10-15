# Product administration fixes — 2026-10-15

## Scope and behavior

- **Photos:** upload failures appear inside the active dialog and preserve the selected file for retry. Product creation no longer replaces a failed-photo warning with a false complete-success message; retry targets the already-created product ID rather than creating another product. Local `/uploads/` media is proxied to the backend, not resolved on the frontend origin. Existing MIME/content validation remains enabled. Every photo mutation (upload, delete, cover change) acquires the product lock before image lookup. Metadata/status writes acquire the same parent-first lock as terminal deletion, preventing stale writes from resurrecting a deleted product.
- **Brands and metadata:** an administrator can create a named brand in the create/edit form and select any stored brand. A product metadata editor persists name, brand, category and description through the existing protected endpoint. Costs and sale prices are edited per presentation in the existing variant manager.
- **Purchase cost:** `referenceCost` is reused, distinct from `salePrice`, including the default product variant. Zero is a confirmed zero cost; omitted/cleared cost is unknown. An explicit `clearReferenceCost` flag permits clearing current reference cost without treating omitted legacy input as an instruction to erase it. Public product responses continue omitting costs.
- **Gross profit:** analytics and Excel expose merchandise revenue, revenue covered by frozen known costs, known cost total, covered gross profit, covered gross margin and units without historical cost. Order discounts are allocated proportionally; shipping is excluded. Costs come exclusively from order-item snapshots, never the current catalog price. Unknown historical costs are marked unknown, not zero or retroactively reconstructed. This is not net profit and excludes operating expenses.
- **Deletion:** forward migration V7 adds `products.deleted_at`. Delete is terminal and distinct from reversible suspension. Operational catalog/detail/variant/media/inventory-selection/checkout paths reject or exclude deleted products; sold orders, stock receipts, inventory movements and their foreign keys remain readable. Stock quantities are never erased. Products with reserved units must have pending orders finalized/canceled before deletion. Old inactive rows are not mass-converted: past suspension and deletion attempts cannot safely be distinguished. Historical SKUs remain reserved by their existing unique identities.

## Verification

- Regression RED: default purchase cost was discarded; deleted stocked products reappeared in admin results (two failing tests before implementation).
- Full backend suite: 92 tests, zero failures/errors/skips, H2 PostgreSQL compatibility profile, including all seven migrations and new cost/privacy, metadata/brand, photo persistence, deletion, reservation and historical order/receipt/Kardex cases.
- Frontend behavioral tests: 8 passed; photo failure/retained-file retry/reopen, partial creation without duplicate product, arbitrary brand error/retry, independent cost/zero/unknown and metadata editing, plus existing shade helpers.
- TypeScript check and isolated Next production build pass (14 generated pages). Build uses a deliberately invalid verification backend origin; no production writes. Generated tsconfig/next-env changes are restored.
- `git diff --check` passes.

## Limits and release checks

- No commit, push, merge, deployment, production database migration or production API write was performed.
- The previously mentioned isolated PostgreSQL service at 127.0.0.1:55441 was unavailable. H2 migration/runtime proof is not a substitute for PostgreSQL deployment rehearsal; run V7 and locking tests on an isolated PostgreSQL instance before release.
- The production-specific storage failure cannot be attributed to a bucket, policy or credential without a sanitized failed-request/server-log trace. The concrete UI error-loss and local media routing defects are corrected and persistence is verified locally; production storage configuration is not claimed verified.
- npm audit currently reports nine advisories in existing Next/Tailwind/PostCSS-related dependency chains. Newly added pinned React Testing Library/user-event/jsdom packages are development-only. Framework major-version changes are deliberately outside this product-fix scope.

## Bounded review corrections

- Reproduced a real delete/edit race with independent H2 transactions: the stale metadata writer returned HTTP 200 after deletion rather than rejecting the terminal product. Metadata and status writes now share a locked operational-product lookup; the regression covers both paths and requires HTTP 404 after the delete commits.
- Added media lock-order/tombstone regressions (both failed before correction). Delete-image and set-cover now acquire the same parent lock as upload before selecting images, preventing competing cover choices and deletion races.
- Replaced reflection-only profit proof with public service tests: a 20% order discount is allocated proportionally, shipping is excluded, current catalog cost changes do not reconstruct missing historical costs, and the real Excel workbook distinguishes numeric zero from unknown text while matching the discounted covered profit.

## Rollout

Back up the database; apply immutable V1–V6 plus forward V7 with normal Flyway validation. Deploy backend before frontend. Verify a disposable product photo, custom brand, two prices, deletion visibility and historical readback in staging. Never drop historical data or rewrite snapshots to make legacy profit appear complete.
