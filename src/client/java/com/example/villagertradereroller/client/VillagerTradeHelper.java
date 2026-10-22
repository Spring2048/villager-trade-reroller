
package com.example.villagertradereroller.client;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Map;

public final class VillagerTradeHelper {

    private VillagerTradeHelper() {
    }

    /**
     * 检查物品是否为附魔书，并且包含指定附魔及等级。
     */
    public static boolean matchesEnchantedBook(
            ItemStack stack,
            Enchantment targetEnchantment,
            int targetLevel
    ) {
        if (stack.isEmpty() || stack.getItem() != Items.ENCHANTED_BOOK) {
            return false;
        }

        Map<Enchantment, Integer> enchantments =
                EnchantmentHelper.getEnchantments(stack);

        return enchantments.getOrDefault(targetEnchantment, 0) == targetLevel;
    }
}
