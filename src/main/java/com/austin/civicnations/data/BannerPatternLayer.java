package com.austin.civicnations.data;

/**
 * One vanilla banner pattern layer. Pattern ids are the compact ids stored in
 * banner BlockEntityTag NBT (for example "cre" for creeper charge).
 */
public record BannerPatternLayer(String patternId, int colorId) {
}
