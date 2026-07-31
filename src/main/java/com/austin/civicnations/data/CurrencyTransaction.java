package com.austin.civicnations.data;

import net.minecraft.nbt.CompoundTag;

public record CurrencyTransaction(long timestamp, String type, String actor, long amount, String note) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Timestamp", timestamp);
        tag.putString("Type", type == null ? "activity" : type);
        tag.putString("Actor", actor == null ? "System" : actor);
        tag.putLong("Amount", amount);
        tag.putString("Note", note == null ? "" : note);
        return tag;
    }

    public static CurrencyTransaction load(CompoundTag tag) {
        return new CurrencyTransaction(
                tag.getLong("Timestamp"),
                tag.getString("Type"),
                tag.getString("Actor"),
                tag.getLong("Amount"),
                tag.getString("Note")
        );
    }
}
