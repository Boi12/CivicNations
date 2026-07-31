# Civic Nations first-cycle design

## Authority model

Civic Nations is authoritative for:

- Nation identity
- Nation membership
- Civic government role
- Joining cooldown
- Invitations
- Permission checks

FTB Teams is the synchronized party representation used by FTB Chunks. Players should not use the ordinary FTB party workflow to join, leave, transfer ownership, or create parties.

## Default roles

| Role | FTB rank | Civic authority |
|---|---|---|
| Leader | Owner | Complete nation control |
| Treasurer | Officer | Treasury spending |
| Official | Officer | Invitations, removals, claims, diplomacy |
| Citizen | Member | No government authority |

Treasurer and Official intentionally share the FTB Officer rank because FTB Teams has fewer ranks than Civic Nations. Fine-grained permissions stay in Civic data.

## Membership lifecycle

1. An authorized official sends a Civic invitation.
2. The target accepts through Civic Nations.
3. The service joins the target to the linked FTB party.
4. Civic membership is saved only after the FTB operation succeeds.
5. Leaving/removal starts a 72-hour real-world cooldown.
6. External FTB changes are rejected or reconciled.

## Territory model

- One nation links to one FTB party.
- The linked party owns FTB Chunks claims.
- Every nation starts with a hard limit of 25 claims.
- Force-loading is always zero.
- Claim/unclaim authorization comes from Civic roles.
- FTB Chunks supplies ordinary block and interaction protection.
- Private plots will later add a second ownership layer inside nation territory.

## Future services

The permissions for treasury spending and diplomacy already exist so future modules can reuse the same role checks. Currency, plots, elections, and laws should be added behind new services instead of placing business logic directly inside screens or commands.
