package com.austin.civicnations.service;

import net.minecraft.network.chat.Component;

public record NationActionResult(boolean success, Component message) {
    public static NationActionResult success(Component message) {
        return new NationActionResult(true, message);
    }

    public static NationActionResult failure(Component message) {
        return new NationActionResult(false, message);
    }
}
