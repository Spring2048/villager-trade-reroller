 package com.example.villagertradereroller;

import com.example.villagertradereroller.VillagerTradeReroller;
import com.example.villagertradereroller.mixin.AbstractVillagerTradeInvoker;
import com.example.villagertradereroller.mixin.MerchantMenuInvoker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TradeRefreshNetworking {
    private static final int REFRESH_INTERVAL_TICKS = 1;
    private static final int MAX_ATTEMPTS = 999;

    private static final Map<UUID, RefreshTask> TASKS = new HashMap<>();

    private TradeRefreshNetworking() {
    }

    private static final class RefreshTask {
        private final UUID playerId;
        private final Villager villager;
        private final int containerId;
        private final Enchantment enchantment;
        private final int targetLevel;

        private int ticksRemaining = REFRESH_INTERVAL_TICKS;
        private int attempts = 0;

        private RefreshTask(
                ServerPlayer player,
                Villager villager,
                int containerId,
                Enchantment enchantment,
                int targetLevel
        ) {
            this.playerId = player.getUUID();
            this.villager = villager;
            this.containerId = containerId;
            this.enchantment = enchantment;
            this.targetLevel = targetLevel;
        }
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                VillagerTradeReroller.REFRESH_TRADES,
                (server, player, handler, buf, responseSender) -> {
                    ResourceLocation enchantmentId =
                            buf.readResourceLocation();
                    int targetLevel = buf.readVarInt();

                    server.execute(() -> {
                        Enchantment enchantment =
                                BuiltInRegistries.ENCHANTMENT.get(enchantmentId);

                        if (enchantment == null
                                || targetLevel < 1
                                || targetLevel > enchantment.getMaxLevel()) {
                            player.sendSystemMessage(
                                    Component.literal("目标附魔或等级无效。")
                            );
                            return;
                        }

                        if (!(player.containerMenu instanceof MerchantMenu menu)) {
                            player.sendSystemMessage(
                                    Component.literal("请先打开村民交易界面。")
                            );
                            return;
                        }

                        Merchant merchant =
                                ((MerchantMenuInvoker) menu)
                                        .villagerTradeReroller$getTrader();

                        if (!(merchant instanceof Villager villager)) {
                            player.sendSystemMessage(
                                    Component.literal("当前交易对象不是普通村民。")
                            );
                            return;
                        }

                        if (!isEligible(villager)) {
                            player.sendSystemMessage(
                                    Component.literal(
                                            "只能刷新尚未交易的新手图书管理员。"
                                    )
                            );
                            return;
                        }

                        if (matchesTarget(
                                villager.getOffers(),
                                enchantment,
                                targetLevel
                        )) {
                            player.sendSystemMessage(
                                    Component.literal("当前交易已经符合目标，无需刷新。")
                            );
                            return;
                        }

                        // 同一玩家的新任务替换旧任务。
                        TASKS.put(
                                player.getUUID(),
                                new RefreshTask(
                                        player,
                                        villager,
                                        menu.containerId,
                                        enchantment,
                                        targetLevel
                                )
                        );

                        player.sendSystemMessage(
                                Component.literal(
                                        "开始自动刷新交易，目标："
                                                + enchantmentId
                                                + "，等级 "
                                                + targetLevel
                                                + "。"
                                )
                        );
                    });
                }
        );

        ServerTickEvents.END_SERVER_TICK.register(
                TradeRefreshNetworking::tick
        );
    }

    private static void tick(MinecraftServer server) {
        var iterator = TASKS.entrySet().iterator();

        while (iterator.hasNext()) {
            RefreshTask task = iterator.next().getValue();

            ServerPlayer player =
                    server.getPlayerList().getPlayer(task.playerId);

            if (player == null) {
                iterator.remove();
                continue;
            }

            if (!(player.containerMenu instanceof MerchantMenu menu)
                    || menu.containerId != task.containerId
                    || !task.villager.isAlive()
                    || !task.villager.level().equals(player.level())
                    || ((MerchantMenuInvoker) menu)
                    .villagerTradeReroller$getTrader() != task.villager) {
                player.sendSystemMessage(
                        Component.literal("已停止自动刷新：交易界面已关闭或交易对象已改变。")
                );
                iterator.remove();
                continue;
            }

            if (!isEligible(task.villager)) {
                player.sendSystemMessage(
                        Component.literal("已停止自动刷新：村民已不符合安全条件。")
                );
                iterator.remove();
                continue;
            }

            if (matchesTarget(
                    task.villager.getOffers(),
                    task.enchantment,
                    task.targetLevel
            )) {
                player.sendSystemMessage(
                        Component.literal(
                                "找到目标附魔书！已刷新 "
                                        + task.attempts
                                        + " 次。"
                        )
                );
                iterator.remove();
                continue;
            }

            if (task.attempts >= MAX_ATTEMPTS) {
                player.sendSystemMessage(
                        Component.literal(
                                "已达到最大刷新次数 "
                                        + MAX_ATTEMPTS
                                        + "，自动刷新停止。"
                        )
                );
                iterator.remove();
                continue;
            }

            if (--task.ticksRemaining > 0) {
                continue;
            }

            task.ticksRemaining = REFRESH_INTERVAL_TICKS;

            MerchantOffers oldOffers = task.villager.getOffers();
            AbstractVillagerTradeInvoker invoker =
                    (AbstractVillagerTradeInvoker) task.villager;

            try {
                // 直接替换村民内部的交易列表。
                MerchantOffers replacementOffers = new MerchantOffers();
                invoker.villagerTradeReroller$setOffers(replacementOffers);

                // 原版方法会向当前 offers 列表添加交易。
                invoker.villagerTradeReroller$updateTrades();

                MerchantOffers newOffers = task.villager.getOffers();

                // 必须确认使用的是新列表，而且成功生成了交易。
                if (newOffers != replacementOffers || newOffers.isEmpty()) {
                    invoker.villagerTradeReroller$setOffers(oldOffers);

                    player.sendSystemMessage(
                            Component.literal("交易列表替换失败，已恢复旧交易；自动刷新已停止。")
                    );

                    iterator.remove();
                    continue;
                }

                // 将新交易发送给客户端。
                player.sendMerchantOffers(
                        menu.containerId,
                        newOffers,
                        task.villager.getVillagerData().getLevel(),
                        task.villager.getVillagerXp(),
                        task.villager.showProgressBar(),
                        task.villager.canRestock()
                );

                task.attempts++;
            } catch (RuntimeException exception) {
                invoker.villagerTradeReroller$setOffers(oldOffers);

                VillagerTradeReroller.LOGGER.error(
                        "Failed to reroll villager trades",
                        exception
                );

                player.sendSystemMessage(
                        Component.literal("刷新失败，已尝试恢复旧交易；自动刷新已停止。")
                );

                iterator.remove();
            }
        }
    }

    private static boolean isEligible(Villager villager) {
        return villager.getVillagerData().getProfession()
                == VillagerProfession.LIBRARIAN
                && villager.getVillagerData().getLevel() == 1
                && villager.getVillagerXp() == 0
                && villager.getOffers().stream()
                .noneMatch(offer -> offer.getUses() > 0);
    }

    private static boolean matchesTarget(
            MerchantOffers offers,
            Enchantment targetEnchantment,
            int targetLevel
    ) {
        for (var offer : offers) {
            ItemStack result = offer.getResult();

            if (!result.is(Items.ENCHANTED_BOOK)) {
                continue;
            }

            int actualLevel = EnchantmentHelper
                    .getEnchantments(result)
                    .getOrDefault(targetEnchantment, 0);

            if (actualLevel == targetLevel) {
                return true;
            }
        }

        return false;
    }
}