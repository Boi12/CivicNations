# Currency Alpha 5

- Rebuilt the Coin Mint as a seamless two-block-wide cabinet with dedicated Civic Nations textures.
- Rebuilt the Coin Press as a clean two-block-tall frame with aligned geometry and no overlapping boundary faces.
- Removed Coin Press denomination and production-count selectors.
- Added automatic recipe detection from the two Materials slots.
- Added Start Machine and Stop Machine controls.
- Added a three-second timed processing cycle that continues while materials and output space remain available.
- Renamed the Coin Press input heading from Plating to Materials.
- Replaced special arrow glyphs and production wording with plain ASCII text.
- Removed the enchanted glint from minted national coins.
- Preserved the permanent JAR filename `civicnations-neoforge-1.20.1.jar`.

# Currency Alpha 4

- Added six fixed-value blank coin denominations.
- Added a two-block-tall Coin Press with Geolosys-compatible silver input.
- Changed the Coin Mint into a two-block-wide secured stamping machine.
- Made either machine half open the correct shared GUI and breaking either half remove the complete structure.
- Changed minted item names to append the nation currency code, such as `Copper Coin [EX]`.
- Removed the required currency-symbol argument while retaining backward-compatible save data.
- Preserved the OP-only `givecurrency` testing override.
- Fixed physical account deposits being counted twice in the in-progress currency draft.
- Preserved the permanent JAR filename `civicnations-neoforge-1.20.1.jar`.

# Civic Nations Changelog

## 0.3.0-currency-alpha.3-neoforge
- Preserved the physical Coin Mint production system and OP-only givecurrency override.
- Restricted Coin Mint packets to the production-run sizes exposed by the GUI.
- Added explicit server-side denomination validation before Blank Coins are consumed.
- Distinguished physical Coin Mint production from OP testing overrides in currency transactions and nation history.
- Clarified in the Coin Mint screen that Blank Coins are pulled from the operator's inventory and the mint must remain in claimed territory.
- Added a dedicated copper Blank Coin item texture.

## 0.3.0-currency-alpha.2-neoforge
- Added a craftable Coin Mint block.
- Removed normal-player command minting; physical currency must be produced at a Coin Mint.
- Coin Mints only operate inside territory claimed by the operator's nation.
- Added Blank Coins; each minted coin consumes one blank.
- One copper ingot crafts nine Blank Coins.
- Added a Coin Mint GUI for denomination and production-run selection.
- Kept the OP-only givecurrency command as a testing override.

## 0.3.0-currency-alpha.1-neoforge
- Added one permanent national currency definition per nation.
- Added physical currency items with nation, currency, code, symbol, and denomination NBT.
- Added persistent digital player accounts and nation treasuries.
- Added issue-supply and financial transaction tracking.
- Added Currency tab to the Nation Ledger.
- Added Leader/Treasurer mint and treasury commands.
- Added OP-only createcurrency and givecurrency testing commands.
- Preserved backward compatibility with worlds that have no currency data.

## 0.2.0-alpha.9-neoforge
- Replaced the base-color-only banner picker with a layered banner designer.
- Added all standard 1.20.1 banner patterns, selectable dye colors, six layers, preview, remove, and reorder controls.
- Added persistent per-nation history stored in the world SavedData.
- Added Overview, Citizens, and History tabs to the Nation Ledger.
- Added citizen role-management buttons with confirmation for removals and leadership transfers.
- Added OP-only server-owned test nations and commands for assigning Citizen, Official, and Treasurer roles.
- Preserved the permanent built JAR filename: civicnations-neoforge-1.20.1.jar.

## 0.2.0-alpha.8-neoforge
- Removed the player inventory and physical-banner requirement from the Create Nation screen.
- Clicking Choose Banner now opens an in-screen banner designer with all 16 base colors.
- Pressing the inventory key no longer closes the nation-creation screen.
- Banner choice is validated and converted into a real banner item on the server.
- Standardized future built JAR names as civicnations-neoforge-1.20.1.jar.

## 0.2.0-alpha.7-neoforge
- Leaders may disband without transferring leadership when the nation owns 0 chunks.
- Nations with claimed territory must unclaim it before disbanding.
- Improved the leader-facing /nation leave message to explain the correct disband command.
- Replaced Unicode em-dash separators with ASCII hyphens to prevent garbled role text.

## 0.2.0-alpha.6-neoforge
- Reworked the nation-creation status layout.
- Removed the stray vanilla Inventory label.
- Moved the player inventory slots down to prevent overlap.
- Renamed the status text to Force Loading: Disabled.

## 0.2.0-alpha.5-neoforge
- Widened the platform dependency range to accept NeoForge/Forge 47.4.x instead of stopping below 47.2.

## 0.2.0-alpha.4-neoforge
- Switched the build to Gradle 8.1.1 for NeoGradle 6 compatibility.
