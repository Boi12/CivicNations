CIVIC NATIONS - NEOFORGE 1.20.1
================================

Target:
- Minecraft 1.20.1
- NeoForge 47.x
- Java 17
- FTB Library, FTB Teams, and FTB Chunks

BUILD
-----
Double-click BUILD_CIVIC_NATIONS_NEOFORGE.bat.
The built mod is always:

build\libs\civicnations-neoforge-1.20.1.jar

Replace the older JAR in the profile mods folder. Never keep two Civic Nations
JARs installed together.

NEW IN THIS UPDATE
------------------
- Added the physical Coin Mint block and craftable Blank Coins.
- Normal national currency issuance now requires a Coin Mint inside the nation's claimed territory.
- Leaders and Treasurers can operate the mint; other roles are rejected server-side.
- Every minted coin consumes one Blank Coin from the operator's inventory.
- The OP-only givecurrency command remains available as a testing override.
- Coin Mint production and OP testing overrides are labeled separately in financial history.

SERVER-OWNED TEST COMMANDS
--------------------------
Names containing spaces must be quoted.

/civicnations admin createservernation "Test Nation" TEST
/civicnations admin addmember "Test Nation" <player> citizen
/civicnations admin setrole <player> official
/civicnations admin setrole <player> treasurer
/civicnations admin setrole <player> citizen
/civicnations admin removemember <player>
/civicnations admin disbandservernation "Test Nation"
/civicnations clearcooldown <player>

A server-owned nation shows Owner: Server. The player who runs the creation
command is the technical FTB Team owner but begins as a Civic Nations Citizen,
allowing the Civic role permissions to be tested.
