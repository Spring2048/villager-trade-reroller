
package com.example.villagertradereroller.client.mixin;

import com.example.villagertradereroller.client.EnchantmentSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends Screen {

    protected MerchantScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void villagerTradeReroller$addButton(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        int x = client.getWindow().getGuiScaledWidth() / 2 + 55;
        int y = client.getWindow().getGuiScaledHeight() / 2 - 108;

        Button button = Button.builder(
                Component.translatable(
                        "screen.villager-trade-reroller.button"
                ),
                b -> {
                    Screen currentScreen = (Screen) (Object) this;
                    client.setScreen(
                            new EnchantmentSelectionScreen(currentScreen)
                    );
                }
        ).bounds(x, y, 86, 20).build();

        this.addRenderableWidget(button);
    }
}
