# Civic Nations currency alpha.1 test plan

Use a backup or disposable world.

## Upgrade safety
1. Replace the old Civic Nations JAR with the newly built JAR.
2. Open an existing world.
3. Confirm the existing nation, members, banner, role, and claims remain.
4. Restart the world once and repeat `/nation info`.

## Banner designer
1. Disband or use a fresh test world, then clear cooldown as OP.
2. Open the Nation Ledger and choose Create Banner.
3. Cycle the base color.
4. Add two or more different pattern layers with different dye colors.
5. Select layers and test Up, Down, and Remove.
6. Cancel and verify the previous design returns.
7. Reopen, finish the design, and confirm the preview matches the created nation.
8. Restart the world and verify the banner still displays.

## Ledger tabs
1. Open Overview and verify owner, role, citizens, and claims.
2. Open Citizens and select a member.
3. Change roles and reopen the Ledger after it closes.
4. Click Remove or Transfer once and verify it asks for confirmation; click again to execute.
5. Open History and verify founding and role events appear newest first.

## Server-owned solo role test
1. Leave/disband the current nation and clear cooldown.
2. Run `/civicnations admin createservernation "Role Test" ROLE`.
3. Confirm the Ledger says Owner: Server and your role is Citizen.
4. As Citizen, verify claiming is rejected.
5. Run `/civicnations admin setrole @s official` and verify claiming is allowed.
6. Run `/civicnations admin setrole @s treasurer` and verify the role updates.
7. Return to Citizen and verify the history records each change.
8. Unclaim all chunks, then disband with the server-nation admin command.
