
package com.example.villagertradereroller.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class EnchantmentTarget {

    private static Enchantment targetEnchantment;
    private static int targetLevel = 1;

    private EnchantmentTarget() {
    }

    /**
     * 保存目标附魔和等级。
     * 示例 ID：minecraft:sharpness
     */
    public static boolean setTarget(String enchantmentId, int level) {
        ResourceLocation id = ResourceLocation.tryParse(enchantmentId);

        if (id == null) {
            return false;
        }

        Optional<Enchantment> result =
                BuiltInRegistries.ENCHANTMENT.getOptional(id);

        if (result.isEmpty()) {
            return false;
        }

        Enchantment enchantment = result.get();

        if (level < 1 || level > enchantment.getMaxLevel()) {
            return false;
        }

        targetEnchantment = enchantment;
        targetLevel = level;
        return true;
    }

    public static Enchantment getTargetEnchantment() {
        return targetEnchantment;
    }

    public static int getTargetLevel() {
        return targetLevel;
    }

    public static boolean hasTarget() {
        return targetEnchantment != null;
    }

    /**
     * 获取游戏当前注册的全部附魔。
     * 包括其他模组注册的附魔。
     */
    public static List<EnchantmentEntry> getAllEnchantments() {
        List<EnchantmentEntry> entries = new ArrayList<>();

        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            ResourceLocation id =
                    BuiltInRegistries.ENCHANTMENT.getKey(enchantment);

            if (id == null) {
                continue;
            }

            String localizedName =
                    Component.translatable(
                            enchantment.getDescriptionId()
                    ).getString();

            entries.add(new EnchantmentEntry(
                    id.toString(),
                    localizedName,
                    enchantment.getMaxLevel()
            ));
        }

        entries.sort((a, b) ->
                a.localizedName().compareToIgnoreCase(b.localizedName()));

        return entries;
    }

    /**
     * 按附魔本地化名称或注册 ID 搜索。
     */
    public static List<EnchantmentEntry> search(String query) {
        String keyword = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        List<EnchantmentEntry> results = new ArrayList<>();

        for (EnchantmentEntry entry : getAllEnchantments()) {
            if (entry.localizedName().toLowerCase(Locale.ROOT)
                    .contains(keyword)
                    || entry.id().toLowerCase(Locale.ROOT)
                    .contains(keyword)) {
                results.add(entry);
            }
        }

        return results;
    }

    public record EnchantmentEntry(
            String id,
            String localizedName,
            int maxLevel
    ) {
    }
}
