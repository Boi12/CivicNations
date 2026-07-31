# Binary resource archive

`civicnations-binary-assets.zip.b64` is a Base64-encoded ZIP containing the PNG resources from the imported Alpha 5 source tree. Gradle decodes and unpacks it into generated resources before packaging the mod.

This temporary bootstrap format keeps every imported texture byte-for-byte intact while allowing the repository to be populated through a text-oriented integration. It should eventually be replaced by ordinary tracked PNG files at the same runtime paths.

Decoded ZIP SHA-256: `d916c55749ab62a1b18c01ad21cc2c786e733b303fb182c222322983fda1cc23`
