package com.austin.civicnations.data;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum NationRole {
    LEADER(4, EnumSet.allOf(NationPermission.class)),
    TREASURER(3, EnumSet.of(NationPermission.SPEND_TREASURY, NationPermission.MINT_CURRENCY)),
    OFFICIAL(2, EnumSet.of(
            NationPermission.INVITE_MEMBERS,
            NationPermission.REMOVE_MEMBERS,
            NationPermission.CLAIM_LAND,
            NationPermission.UNCLAIM_LAND,
            NationPermission.MANAGE_DIPLOMACY
    )),
    CITIZEN(1, EnumSet.noneOf(NationPermission.class));

    private final int authorityLevel;
    private final Set<NationPermission> permissions;

    NationRole(int authorityLevel, Set<NationPermission> permissions) {
        this.authorityLevel = authorityLevel;
        this.permissions = Set.copyOf(permissions);
    }

    public int authorityLevel() {
        return authorityLevel;
    }

    public boolean has(NationPermission permission) {
        return permissions.contains(permission);
    }

    public Set<NationPermission> permissions() {
        return permissions;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        String lower = serializedName();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public boolean canBeAssignedDirectly() {
        return this != LEADER;
    }

    public static Optional<NationRole> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(role -> role.serializedName().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
