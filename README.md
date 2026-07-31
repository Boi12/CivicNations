# Civic Nations

Civic Nations is an Eco-inspired nation, economy, currency, market, banking, and land-management mod for Minecraft 1.20.1.

- Platform: NeoForge 47.x
- Java: 17
- Build: Gradle 8.1.1
- Integrations: FTB Teams and FTB Chunks
- Permanent output: `civicnations-neoforge-1.20.1.jar`

The `develop` branch contains an exact, checksummed import of the existing Alpha 5 source. GitHub Actions reconstructs that source, resolves the NeoForge and FTB dependencies, and performs the real build. The temporary bootstrap archive avoids losing binary textures or source files during the initial connector-based import; it will be expanded into ordinary repository files after the baseline build is stable.

Existing nation, role, ledger, banner, treasury, history, claim-limit, Coin Press, Coin Mint, and administrative testing systems are being preserved while the project is compiled and expanded toward Civic Nations 1.0.

See `docs/ROADMAP.md` for the locked release scope.
