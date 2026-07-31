package com.austin.civicnations.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record NationInvitation(UUID nationId, UUID invitedPlayerId, UUID inviterId,
                               long createdAt, long expiresAt) {
    public boolean isExpired(long now) {
        return expiresAt > 0L && now >= expiresAt;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Nation", nationId);
        tag.putUUID("Player", invitedPlayerId);
        tag.putUUID("Inviter", inviterId);
        tag.putLong("CreatedAt", createdAt);
        tag.putLong("ExpiresAt", expiresAt);
        return tag;
    }

    public static NationInvitation load(CompoundTag tag) {
        return new NationInvitation(
                tag.getUUID("Nation"),
                tag.getUUID("Player"),
                tag.getUUID("Inviter"),
                tag.getLong("CreatedAt"),
                tag.getLong("ExpiresAt")
        );
    }
}
