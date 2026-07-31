package com.austin.civicnations.data;

import net.minecraft.nbt.CompoundTag;

public record NationHistoryEntry(long timestamp, String message) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Timestamp", timestamp);
        tag.putString("Message", message);
        return tag;
    }

    public static NationHistoryEntry load(CompoundTag tag) {
        return new NationHistoryEntry(
                tag.getLong("Timestamp"),
                tag.getString("Message")
        );
    }
}
