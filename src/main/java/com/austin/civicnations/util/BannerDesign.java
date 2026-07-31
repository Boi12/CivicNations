package com.austin.civicnations.util;

import com.austin.civicnations.data.BannerPatternLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BannerDesign {
    public static final int MAX_LAYERS = 6;

    /**
     * Vanilla 1.20.1 banner pattern ids. Special-template patterns are included
     * because this editor is intentionally creative and does not consume items.
     */
    public static final List<PatternChoice> PATTERNS = List.of(
            new PatternChoice("bs", "Bottom Stripe"),
            new PatternChoice("ts", "Top Stripe"),
            new PatternChoice("ls", "Left Stripe"),
            new PatternChoice("rs", "Right Stripe"),
            new PatternChoice("cs", "Center Stripe"),
            new PatternChoice("ms", "Middle Stripe"),
            new PatternChoice("drs", "Down Right Stripe"),
            new PatternChoice("dls", "Down Left Stripe"),
            new PatternChoice("ss", "Small Stripes"),
            new PatternChoice("cr", "Cross"),
            new PatternChoice("sc", "Saltire"),
            new PatternChoice("ld", "Left Diagonal"),
            new PatternChoice("rud", "Right Upside-down Diagonal"),
            new PatternChoice("lud", "Left Upside-down Diagonal"),
            new PatternChoice("rd", "Right Diagonal"),
            new PatternChoice("vh", "Left Half"),
            new PatternChoice("vhr", "Right Half"),
            new PatternChoice("hh", "Top Half"),
            new PatternChoice("hhb", "Bottom Half"),
            new PatternChoice("bl", "Bottom Left Corner"),
            new PatternChoice("br", "Bottom Right Corner"),
            new PatternChoice("tl", "Top Left Corner"),
            new PatternChoice("tr", "Top Right Corner"),
            new PatternChoice("bt", "Bottom Triangle"),
            new PatternChoice("tt", "Top Triangle"),
            new PatternChoice("bts", "Bottom Triangles"),
            new PatternChoice("tts", "Top Triangles"),
            new PatternChoice("mc", "Circle"),
            new PatternChoice("mr", "Rhombus"),
            new PatternChoice("bo", "Border"),
            new PatternChoice("cbo", "Curly Border"),
            new PatternChoice("bri", "Bricks"),
            new PatternChoice("gra", "Top Gradient"),
            new PatternChoice("gru", "Bottom Gradient"),
            new PatternChoice("cre", "Creeper Charge"),
            new PatternChoice("sku", "Skull Charge"),
            new PatternChoice("flo", "Flower Charge"),
            new PatternChoice("moj", "Thing"),
            new PatternChoice("glb", "Globe"),
            new PatternChoice("pig", "Snout")
    );

    private static final Set<String> ALLOWED_IDS = PATTERNS.stream()
            .map(PatternChoice::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static final Map<String, String> DISPLAY_NAMES = PATTERNS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(PatternChoice::id, PatternChoice::displayName));

    private BannerDesign() {}

    public static ItemStack create(DyeColor baseColor, List<BannerPatternLayer> layers) {
        ItemStack stack = BannerItems.create(baseColor);
        List<BannerPatternLayer> sanitized = sanitize(layers);
        if (sanitized.isEmpty()) {
            return stack;
        }

        CompoundTag blockEntityTag = stack.getOrCreateTagElement("BlockEntityTag");
        ListTag patternTags = new ListTag();
        for (BannerPatternLayer layer : sanitized) {
            CompoundTag patternTag = new CompoundTag();
            patternTag.putString("Pattern", layer.patternId());
            patternTag.putInt("Color", layer.colorId());
            patternTags.add(patternTag);
        }
        blockEntityTag.put("Patterns", patternTags);
        return stack;
    }

    public static List<BannerPatternLayer> sanitize(List<BannerPatternLayer> layers) {
        if (layers == null || layers.isEmpty()) {
            return List.of();
        }
        return layers.stream()
                .filter(layer -> layer != null
                        && ALLOWED_IDS.contains(layer.patternId())
                        && layer.colorId() >= 0
                        && layer.colorId() < DyeColor.values().length)
                .limit(MAX_LAYERS)
                .toList();
    }

    public static boolean isAllowedPattern(String id) {
        return ALLOWED_IDS.contains(id);
    }

    public static String displayName(String id) {
        return DISPLAY_NAMES.getOrDefault(id, id);
    }

    public record PatternChoice(String id, String displayName) {
    }
}
