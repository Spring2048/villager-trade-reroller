
package com.example.villagertradereroller.client;

import com.example.villagertradereroller.VillagerTradeReroller;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.FormattedCharSequence;
import me.towdium.pinin.PinIn;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EnchantmentSelectionScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int PANEL_GAP = 10;
    private static final int ROW_HEIGHT = 25;
    private static final int SEARCH_HEIGHT = 20;
    private static final PinIn PININ = new PinIn();

    private EditBox searchBox;

    private final List<Button> resultButtons = new ArrayList<>();
    private final List<LevelEntry> buttonEntries = new ArrayList<>();

    private List<LevelEntry> visibleResults = new ArrayList<>();

    private final Screen parentScreen;

    private int scrollOffset;
    private int listX;
    private int listWidth;
    private int listTop;
    private int visibleCount;

    private LevelEntry hoveredEntry;

    public EnchantmentSelectionScreen() {
        this(null);
    }

    public EnchantmentSelectionScreen(Screen parentScreen) {
        super(Component.translatable(
                "screen.villager-trade-reroller.title"
        ));
        this.parentScreen = parentScreen;
    }

    private static class LevelEntry {
        private final EnchantmentTarget.EnchantmentEntry enchantment;
        private final int level;
        private final Enchantment enchantmentObject;
        private final ItemStack icon;

        private LevelEntry(
                EnchantmentTarget.EnchantmentEntry enchantment,
                int level,
                Enchantment enchantmentObject
        ) {
            this.enchantment = enchantment;
            this.level = level;
            this.enchantmentObject = enchantmentObject;

            this.icon = new ItemStack(Items.ENCHANTED_BOOK);

            EnchantmentHelper.setEnchantments(
                    Map.of(enchantmentObject, level),
                    this.icon
            );
        }

        private String displayName() {
            return enchantment.localizedName()
                    + " " + toRoman(level);
        }
    }

    @Override
    public void onClose() {
        if (parentScreen != null && minecraft != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        resultButtons.clear();
        buttonEntries.clear();

        listX = MARGIN;
        listWidth = Math.max(
                120,
                Math.min(220, (width - MARGIN * 3) / 2)
        );

        int rightX = listX + listWidth + PANEL_GAP;
        int rightWidth = Math.max(
                80,
                width - rightX - MARGIN
        );

        listTop = 65;

        searchBox = new EditBox(
                font,
                listX,
                37,
                listWidth,
                SEARCH_HEIGHT,
                Component.translatable(
                        "screen.villager-trade-reroller.search"
                )
        );

        searchBox.setHint(Component.translatable(
                "screen.villager-trade-reroller.search_hint"
        ));

        searchBox.setResponder(value -> {
            scrollOffset = 0;
            refreshResults();
        });

        addRenderableWidget(searchBox);

        visibleCount = Math.max(
                1,
                (height - listTop - 18) / ROW_HEIGHT
        );

        refreshResults();
    }

    private void refreshResults() {
        for (Button button : resultButtons) {
            removeWidget(button);
        }

        resultButtons.clear();
        buttonEntries.clear();

        String query = searchBox == null
                ? ""
                : searchBox.getValue()
                .trim()
                .toLowerCase(Locale.ROOT);

        List<EnchantmentTarget.EnchantmentEntry> allEnchantments =
                EnchantmentTarget.search("");

        List<LevelEntry> results = new ArrayList<>();

        for (EnchantmentTarget.EnchantmentEntry entry : allEnchantments) {
            String name = entry.localizedName()
                    .toLowerCase(Locale.ROOT);

            String id = entry.id()
                    .toLowerCase(Locale.ROOT);

            ResourceLocation resourceId =
                    ResourceLocation.tryParse(entry.id());

            if (resourceId == null) {
                continue;
            }

            Enchantment enchantment =
                    BuiltInRegistries.ENCHANTMENT.get(resourceId);

            if (enchantment == null || !enchantment.isTradeable()) {
                continue;
            }

            for (int level = 1; level <= entry.maxLevel(); level++) {
                String roman = toRoman(level)
                        .toLowerCase(Locale.ROOT);

                String numeric = String.valueOf(level);

                boolean pinyinMatches = !query.isEmpty()
                        && PININ.contains(entry.localizedName(), query);

                boolean matches = query.isEmpty()
                        || name.contains(query)
                        || id.contains(query)
                        || roman.equals(query)
                        || numeric.equals(query)
                        || (name + " " + roman).contains(query)
                        || (id + " " + roman).contains(query)
                        || pinyinMatches;

                if (matches) {
                    results.add(
                            new LevelEntry(entry, level, enchantment)
                    );
                }
            }
        }

        visibleResults = results;

        int maxOffset = Math.max(
                0,
                visibleResults.size() - visibleCount
        );

        scrollOffset = Math.max(
                0,
                Math.min(scrollOffset, maxOffset)
        );

        int end = Math.min(
                visibleResults.size(),
                scrollOffset + visibleCount
        );

        int buttonWidth = listWidth;
        int y = listTop;

        for (int i = scrollOffset; i < end; i++) {
            LevelEntry entry = visibleResults.get(i);

            Button button = Button.builder(
                    Component.empty(),
                    b -> selectTarget(entry)
            ).bounds(
                    listX,
                    y,
                    buttonWidth,
                    ROW_HEIGHT - 2
            ).build();

            resultButtons.add(button);
            buttonEntries.add(entry);

            addRenderableWidget(button);

            y += ROW_HEIGHT;
        }
    }

    private void selectTarget(LevelEntry entry) {
        String enchantmentId = entry.enchantment.id();
        int level = entry.level;

        if (!EnchantmentTarget.setTarget(enchantmentId, level)) {
            return;
        }

        Enchantment enchantment =
                EnchantmentTarget.getTargetEnchantment();

        if (enchantment == null
                || minecraft == null
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.getConnection() == null) {
            return;
        }

        ResourceLocation registeredId =
                BuiltInRegistries.ENCHANTMENT.getKey(enchantment);

        if (registeredId == null) {
            return;
        }

        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeResourceLocation(registeredId);
        buf.writeVarInt(level);

        ClientPlayNetworking.send(
                VillagerTradeReroller.REFRESH_TRADES,
                buf
        );

        onClose();
    }

    private Component getEnchantmentDescription(LevelEntry entry) {
        ResourceLocation id = ResourceLocation.tryParse(
                entry.enchantment.id()
        );

        if (id == null) {
            return null;
        }

        String descriptionKey = "enchantment."
                + id.getNamespace()
                + "."
                + id.getPath()
                + ".desc";

        if (I18n.exists(descriptionKey)) {
            return Component.translatable(descriptionKey);
        }

        return null;
    }

    private static String toRoman(int number) {
        String[] numerals = {
                "M", "CM", "D", "CD", "C", "XC",
                "L", "XL", "X", "IX", "V", "IV", "I"
        };

        int[] values = {
                1000, 900, 500, 400, 100, 90,
                50, 40, 10, 9, 5, 4, 1
        };

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                result.append(numerals[i]);
            }
        }

        return result.toString();
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (mouseX < listX || mouseX > listX + listWidth) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        int maxOffset = Math.max(
                0,
                visibleResults.size() - visibleCount
        );

        scrollOffset = Math.max(
                0,
                Math.min(
                        maxOffset,
                        scrollOffset - (int) Math.signum(delta)
                )
        );

        refreshResults();
        return true;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        int rightX = listX + listWidth + PANEL_GAP;
        int rightWidth = width - rightX - MARGIN;
        int panelBottom = height - 12;

        graphics.fill(
                listX - 4, 8,
                listX + listWidth + 4, panelBottom,
                0xAA101010
        );

        graphics.fill(
                rightX - 4, 8,
                rightX + rightWidth + 4, panelBottom,
                0xAA101010
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.villager-trade-reroller.search"
                ),
                listX,
                18,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.villager-trade-reroller.available"
                ),
                listX,
                27,
                0xAAAAAA
        );

        super.render(graphics, mouseX, mouseY, partialTick);

        hoveredEntry = null;

        for (int i = 0; i < resultButtons.size(); i++) {
            Button button = resultButtons.get(i);
            LevelEntry entry = buttonEntries.get(i);

            int x = button.getX();
            int y = button.getY();

            if (button.isHovered()) {
                hoveredEntry = entry;
            }

            graphics.renderItem(entry.icon, x + 3, y + 2);

            graphics.drawString(
                    font,
                    entry.displayName(),
                    x + 24,
                    y + 7,
                    0xFFFFFF
            );

            if (button.isHovered()) {
                graphics.renderTooltip(
                        font,
                        Component.literal(entry.displayName()),
                        mouseX,
                        mouseY
                );
            }
        }

        int rightTextX = rightX + 8;

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.villager-trade-reroller.preview"
                ),
                rightTextX,
                18,
                0xFFFFFF
        );

        LevelEntry preview = hoveredEntry;

        if (preview == null && !visibleResults.isEmpty()) {
            preview = visibleResults.get(
                    Math.min(
                            scrollOffset,
                            visibleResults.size() - 1
                    )
            );
        }

        if (preview != null) {
            int iconX = rightX + Math.max(8, (rightWidth - 32) / 2);
            int iconY = 52;

            graphics.renderItem(preview.icon, iconX, iconY);
            graphics.renderItemDecorations(
                    font,
                    preview.icon,
                    iconX,
                    iconY
            );

            graphics.drawCenteredString(
                    font,
                    preview.displayName(),
                    rightX + rightWidth / 2,
                    94,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    font,
                    preview.enchantment.id(),
                    rightX + rightWidth / 2,
                    109,
                    0xAAAAAA
            );

            graphics.drawCenteredString(
                    font,
                    preview.displayName(),
                    rightX + rightWidth / 2,
                    94,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    font,
                    preview.enchantment.id(),
                    rightX + rightWidth / 2,
                    109,
                    0xAAAAAA
            );

            /*graphics.drawString(
                    font,
                    "Enchantment Description",
                    rightX + 8,
                    130,
                    0xFFFFFF
            );*/

            // 获取本地化描述
            Component description = getEnchantmentDescription(preview);

            int descriptionY = 130;

            if (description != null) {
                // 只有存在描述时，才显示标题
                graphics.drawString(
                        font,
                        Component.translatable(
                                "screen.villager-trade-reroller.description"
                        ),
                        rightX + 8,
                        descriptionY,
                        0xFFFFFF
                );

                descriptionY += 16;

                int descriptionX = rightX + 8;
                int descriptionWidth = Math.max(20, rightWidth - 16);

                List<FormattedCharSequence> lines =
                        font.split(description, descriptionWidth);

                for (FormattedCharSequence line : lines) {
                    graphics.drawString(
                            font,
                            line,
                            descriptionX,
                            descriptionY,
                            0xCCCCCC
                    );

                    descriptionY += 11;
                }
            }

/*            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.villager-trade-reroller.start"
                    ),
                    rightX + rightWidth / 2,
                    Math.min(height - 43, descriptionY + 12),
                    0x55FF55
            );

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.villager-trade-reroller.select_hint"
                    ),
                    rightX + rightWidth / 2,
                    Math.min(height - 29, descriptionY + 26),
                    0xAAAAAA
            );*/

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.villager-trade-reroller.start"
                    ),
                    rightX + rightWidth / 2,
                    Math.min(height - 29, descriptionY + 26),
                    0x55FF55
            );

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.villager-trade-reroller.select_hint"
                    ),
                    rightX + rightWidth / 2,
                    Math.min(height - 43, descriptionY + 12),
                    0xAAAAAA
            );
        } else {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.villager-trade-reroller.no_results"
                    ),
                    rightTextX,
                    55,
                    0xAAAAAA
            );
        }

        int maxOffset = Math.max(
                0,
                visibleResults.size() - visibleCount
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.villager-trade-reroller.results",
                        visibleResults.size()
                ),
                listX,
                height - 22,
                0xAAAAAA
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.villager-trade-reroller.scroll",
                        scrollOffset + 1,
                        Math.max(1, maxOffset + 1)
                ),
                rightTextX,
                height - 22,
                0xAAAAAA
        );
    }
}
