# Civic Nations

Civic Nations is an Eco-inspired nation, economy, currency, market, banking, and land-management mod for Minecraft 1.20.1.

- Platform: NeoForge 47.x
- Java: 17
- Build: Gradle 8.1.1
- Integrations: FTB Teams and FTB Chunks
- Permanent output: `civicnations-neoforge-1.20.1.jar`

The repository currently contains the imported Currency Alpha 5 source. Existing nation, role, ledger, banner, treasury, history, claim-limit, Coin Press, Coin Mint, and OP testing systems are being preserved while the project is compiled and expanded toward Civic Nations 1.0.

See `docs/ROADMAP.md` for the complete locked scope and `README-CURRENCY.md` for the current currency behavior.

## Build

On Windows, run `BUILD_CIVIC_NATIONS_NEOFORGE.bat`.

On any system with Java 17 and Gradle 8.1.1:

```bash
gradle --no-daemon build
```

The output must be:

```text
build/libs/civicnations-neoforge-1.20.1.jar
```
