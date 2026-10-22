
package com.example.villagertradereroller.mixin;

import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantMenu.class)
public interface MerchantMenuInvoker {
    @Accessor("trader")
    Merchant villagerTradeReroller$getTrader();
}
