# Civic Nations Currency Alpha 5

Target: Minecraft 1.20.1 / NeoForge 47.x / Java 17

## National currency

A nation currency has a name and a 2-5 character code. Separate currency symbols are no longer required.
All nations use the same fixed physical denominations:

- Copper Coin: 1
- Iron Coin: 5
- Silver Coin: 10
- Gold Coin: 25
- Diamond-Plated Gold Coin: 50
- Hexagonal Diamond-Plated Gold Coin: 100

Issued physical coins end with the currency code, for example `Copper Coin [EX]`.
The item data also stores the issuing nation and unique currency ID, so different nations' coins cannot stack together accidentally. Minted coins do not use the enchanted-item glint.

## Coin Press

The Coin Press is a seamless two-block-tall industrial machine. Either half opens the same inventory.
It manufactures blank coins and does not require nation membership.

Put the recipe materials into the two Materials slots and press `Start Machine`. Each operation takes 60 game ticks, or about three seconds at normal server speed. The machine continues processing while it has a valid recipe and output space. It stops automatically when the materials run out or the output becomes blocked.

Recipes:

- 1 Copper Ingot produces 8 Blank Copper Coins
- 1 Iron Ingot produces 8 Blank Iron Coins
- 1 `forge:ingots/silver` ingot produces 8 Blank Silver Coins
- 1 Gold Ingot produces 8 Blank Gold Coins
- 1 Gold Ingot + 1 Diamond produces 4 Blank Diamond-Plated Gold Coins
- 1 Gold Ingot + 2 Diamonds produces 2 Blank Hexagonal Diamond-Plated Gold Coins

Geolosys silver is supported through the shared silver-ingot tag, with a direct `geolosys:silver_ingot` fallback.

## Coin Mint

The Coin Mint is a flush two-block-wide secured stamping machine. Dedicated left and right textures form one continuous cabinet without overlapping model faces. Either half opens the same GUI.
Normal physical currency is created only by stamping the matching blank coin at a complete Coin Mint.
Both halves must be inside territory claimed by the operator's nation. The operator must have the mint-currency permission.

Examples:

- Blank Copper Coin becomes Copper Coin [EX], value 1 EX
- Blank Silver Coin becomes Silver Coin [EX], value 10 EX
- Blank Hexagonal Diamond-Plated Gold Coin becomes Hexagonal Diamond-Plated Gold Coin [EX], value 100 EX

## Commands

Create a player nation's currency:

```
/nation currency create <code> <name>
```

The OP-only testing override remains available:

```
/civicnations admin givecurrency <nation> <player> <denomination> <count>
```

The override does not consume blanks and is recorded separately as a testing override.
