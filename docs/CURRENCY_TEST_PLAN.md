# Currency Update Test Plan

1. Open an existing world and verify the nation, claims, roles, banner, and history still load.
2. Create a currency with `/nation currency create IVC $ Ironvale Crown`.
3. Confirm duplicate creation is rejected.
4. Open the Ledger Currency tab and verify name, code, symbol, denominations, supply, balances, and treasury.
5. As OP, run `/civicnations admin givecurrency "<Nation>" @s 10 3`.
6. Confirm three physical $10 notes appear and Total Issued increases by 30.
7. Run `/nation currency deposit`; confirm the notes disappear and digital balance increases by 30.
8. Run `/nation currency withdraw 5 2`; confirm digital balance falls by 10 and two $5 notes appear.
9. Run `/nation treasury deposit 10`; confirm player balance falls and treasury rises.
10. As Citizen or Official, verify treasury withdrawal is rejected and the Coin Mint refuses access.
11. As Treasurer, verify the Coin Mint and treasury withdrawal work.
12. Restart the world and verify all currency data persists.
13. Verify a non-OP cannot use `/civicnations admin givecurrency`.
14. Verify normal nation disband is blocked while digital accounts or treasury contain funds.

## Coin Mint tests
1. Craft and place a Coin Mint outside claimed territory; verify it refuses to open.
2. Place it inside the nation's claimed territory; verify Leader/Treasurer access.
3. Try minting without Blank Coins; verify no currency or supply is created.
4. Craft Blank Coins from copper and mint 1, 4, 8, 16, 32, and 64 coin runs.
5. Confirm each coin consumes one blank and total issued supply increases by denomination x count.
6. Break the mint while its screen is open; verify further mint packets are rejected.
7. Verify `/civicnations admin givecurrency` still works as an OP-only override.
8. Verify Coin Mint transactions/history say they were minted physically, while OP-spawned currency is labeled as a testing override.
9. With a modified client or packet tool, submit a run size other than 1, 4, 8, 16, 32, or 64; verify the server rejects it without consuming blanks or increasing supply.
