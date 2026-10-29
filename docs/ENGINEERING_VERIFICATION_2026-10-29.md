# EliteRetail POS — Engineering Verification (October 29, 2026)

**Project:** University/portfolio JavaFX point-of-sale demonstration
**Repository:** `raghd-dev/EliteRetail-POS-System`
**Development branch at verification:** `fix/checkout-integrity`
**Java target:** 21

## Engineering changes

| Commit | Change | Scope |
|---|---|---|
| `864aeaa` | Discount usage accounting | Count an applied promotion once per completed sale and integrate usage updates into checkout transaction |
| `502afde` | Checkout validation and bill-number allocation | Validate checkout inputs and serialize final bill-number assignment with a MySQL named lock |
| `7724df8` | Monetary precision | Apply cent-based `BigDecimal` arithmetic at important calculation and persistence boundaries |
| `124069a` | Authentication and role checks | Remove legacy plaintext verification; reset state after failed sign-in; add service-layer role checks |
| Subsequent development-branch commit | Java CI (Maven) workflow | Java 21 automated Maven build and unit tests |

## Tests and evidence

- **Developer-provided CMD transcript:** `mvn clean test` completed with **26 tests run, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS` on October 29, 2026.
- **Developer confirmation:** GitHub Actions Java CI was green for the development branch. Exact run ID and commit SHA were not independently inspected here.
- **Developer confirmation:** Local demo application login, checkout, discount and rollback checks behaved as expected. No raw MySQL trace is attached.
- Source code tests cover selected domain calculations, password verification, role guard behavior, discount usage and checkout validation.

## Remaining limitations

- Simultaneous multi-cashier live MySQL checkout/load testing remains **unverified**. Unit tests and sequential manual sales do not establish concurrency safety in every deployment.
- The desktop application connects to MySQL using configured local credentials. Java service-layer authorization alone does not prevent direct database access by someone with those credentials.
- The application is an educational project, not a real payment-processing system or independently certified production POS.
- The monetary fix improves critical cent-based calculations, but JavaFX-bound model values have not all been converted to `BigDecimal`.
- No external penetration test or independent full database transaction audit was conducted.

## Reproducible commands

```bash
mvn clean test
mvn javafx:run
```

Use a local MySQL demo database. The sample `database/pos_db.sql` **drops and recreates tables**, so do not run it against any database containing data you need to retain.

## Portfolio interpretation

The documented fixes and tests demonstrate practical work with JavaFX, layered design, transaction handling, validation, password hashing, automated tests, and CI. They should be described as an educational software-engineering exercise rather than production deployment evidence.
