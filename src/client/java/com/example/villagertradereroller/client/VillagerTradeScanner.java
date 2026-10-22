
package com.example.villagertradereroller.client;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;

public final class VillagerTradeScanner {

    private VillagerTradeScanner() {
    }

    /**
     * 检查当前村民交易列表中是否存在指定附魔书和等级。
     */
    public static boolean containsTarget(
            MerchantScreen screen,
            Enchantment targetEnchantment,
            int targetLevel
    ) {
        List<MerchantOffer> offers =
                screen.getMenu().getOffers();

        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();

            if (VillagerTradeHelper.matchesEnchantedBook(
                    result,
                    targetEnchantment,
                    targetLevel
            )) {
                return true;
            }
        }

        return false;
    }
}
