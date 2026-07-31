package com.austin.civicnations.data;

/**
 * Server-authoritative permissions used by nation roles.
 *
 * Treasury and diplomacy permissions are defined now so later systems can use
 * the same permission model without migrating saved nation roles.
 */
public enum NationPermission {
    INVITE_MEMBERS,
    REMOVE_MEMBERS,
    ASSIGN_ROLES,
    CLAIM_LAND,
    UNCLAIM_LAND,
    SPEND_TREASURY,
    MINT_CURRENCY,
    MANAGE_DIPLOMACY,
    TRANSFER_LEADERSHIP,
    DISBAND_NATION
}
