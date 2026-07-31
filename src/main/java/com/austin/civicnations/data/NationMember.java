package com.austin.civicnations.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record NationMember(UUID playerId, NationRole role, long joinedAt) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Player", playerId);
        tag.putString("Role", role.serializedName());
        tag.putLong("JoinedAt", joinedAt);
        return tag;
    }

    public static NationMember load(CompoundTag tag) {
        NationRole role = NationRole.parse(tag.getString("Role")).orElse(NationRole.CITIZEN);
        return new NationMember(
                tag.getUUID("Player"),
                role,
                tag.contains("JoinedAt") ? tag.getLong("JoinedAt") : System.currentTimeMillis()
        );
    }
}
