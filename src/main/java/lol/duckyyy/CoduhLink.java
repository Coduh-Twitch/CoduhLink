package lol.duckyyy;

import com.github.twitch4j.TwitchClient;
import lol.duckyyy.api.ClientboundRenameAnimalPayload;
import lol.duckyyy.api.ServerboundChatMessagePayload;
import lol.duckyyy.api.ServerboundRewardRedemptionPayload;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.driver.unix.aix.perfstat.PerfstatNetInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CoduhLink implements ModInitializer {

    private int ticks = 0;
    public static final String MOD_ID = "coduhlink";
    public static TwitchClient twitchClient;
    public static ConfigModel CONFIG;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean TWITCH_ENABLED = true;
    public static Map<Integer, Boolean> ANIMALS;
    public static Map<Integer, String> ANIMAL_NAMES;
    public static Map<Integer, String> PLAYER_RENAMED;
    public static int RENAMING_ANIMAL = -1;
    public static Component CHAT_PREFIX = Component.empty().append(Component.literal("EVENTS ").withColor(TextColor.DARK_PURPLE).withStyle(ChatFormatting.BOLD)).append(Component.literal("» ").withColor(TextColor.DARK_GRAY)).append(Component.empty());

    private void giveItem(String username, String itemName, Item item, int amount, MinecraftServer instance, String rewardTitle, int rewardCost) {
        ItemStack stack = new ItemStack(item, amount);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));

        for (ServerPlayer player : instance.getPlayerList().getPlayers()) {
            PlayerInventoryStorage inventory = PlayerInventoryStorage.of(player.getInventory());

            boolean dropped = player.getInventory().getFreeSlot() == -1;
            boolean success = false;
            if (!dropped) {
                inventory.insert(ItemVariant.of(stack.getItem()), stack.count(), Transaction.openOuter());
                success = true;
            } else {
                ItemEntity entity = EntityTypes.ITEM.create(player.level(), EntitySpawnReason.COMMAND);

                if (entity != null) {
                    entity.spawnAtLocation(player.level(), stack, player.position().add(0, 1, 0));
                    success = true;

                }

            }

            String message = String.format("%s %s you x%s %s! (%spt%s)", username, dropped ? "dropped" : "gave", amount, item.getName(new ItemStack(item)).getString(), rewardCost, rewardCost == 1 ? "" : "s");

            if (success) {
                player.sendOverlayMessage(Component.literal(message).withColor(CommonColors.GREEN));
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.format("Item%s %s!", amount == 1 ? "" : "s", dropped ? "Dropped" : "Received")).withColor(TextColor.GREEN)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(message).withColor(TextColor.YELLOW)));
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.UI, 0.5F, 1.3F);
            }
        }


    }


    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
        AutoConfig.register(ConfigModel.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(ConfigModel.class).getConfig();
        ANIMALS = new HashMap<Integer, Boolean>();
        ANIMAL_NAMES = new HashMap<Integer, String>();
        PLAYER_RENAMED = new HashMap<Integer, String>();

        PayloadTypeRegistry.serverboundPlay().register(ServerboundRewardRedemptionPayload.TYPE, ServerboundRewardRedemptionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ServerboundChatMessagePayload.TYPE, ServerboundChatMessagePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundRenameAnimalPayload.TYPE, ClientboundRenameAnimalPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ServerboundChatMessagePayload.TYPE, (p, context) -> {
            ServerboundChatMessagePayload payload = (ServerboundChatMessagePayload) p;

            if (TWITCH_ENABLED) {
                int color = payload.color();
                LOGGER.info("Color " + color);

                for (ServerPlayer player : PlayerLookup.all(context.server())) {
                    player.sendSystemMessage(Component.empty().append(Component.literal(payload.username()).withColor(color).withStyle(ChatFormatting.BOLD)).append(Component.empty().append(Component.literal(String.format(": %s", payload.content())).withColor(TextColor.WHITE))));
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ServerboundRewardRedemptionPayload.TYPE, (p, context) -> {
            LOGGER.info("Packet Received");
            LOGGER.info(p.toString());
            ServerboundRewardRedemptionPayload payload = (ServerboundRewardRedemptionPayload) p;

            if (TWITCH_ENABLED) {
                if (CONFIG.potion_effect_rewards.containsKey(payload.id())) {
                    ConfigModel.PotionEffectReward reward = CONFIG.potion_effect_rewards.get(payload.id());
                    MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(reward.effect));
                    int secondsToAdd = reward.seconds;
                    int seconds = reward.seconds;
                    int strength = reward.strength;

                    boolean added = false;

                    if (context.player().hasEffect(Holder.direct(effect))) {
                        seconds = Math.round(context.player().getEffect(Holder.direct(effect)).getDuration() / 20) + seconds;
                        added = true;
                    }

                    MobEffectInstance effectInstance = new MobEffectInstance(Holder.direct(effect), seconds * 20, strength - 1);

                    context.player().addEffect(effectInstance);

                    if (added) {
                        context.player().sendSystemMessage(CHAT_PREFIX.copy().append(Component.literal(String.format("%s added %s seconds to your %s %s!", payload.username(), secondsToAdd, effect.getDisplayName().getString(), strength)).withColor(TextColor.GRAY).withStyle(ChatFormatting.ITALIC)));
                        context.player().sendOverlayMessage(Component.literal(String.format("%s added %s seconds to your %s %s!", payload.username(), secondsToAdd, effect.getDisplayName().getString(), strength)).withColor(TextColor.GREEN));
                    } else {
                        context.player().sendSystemMessage(CHAT_PREFIX.copy().append(Component.literal(String.format("%s gave you %s %s for %s seconds!", payload.username(), effect.getDisplayName().getString(), strength, seconds)).withColor(TextColor.GRAY).withStyle(ChatFormatting.ITALIC)));
                        context.player().sendOverlayMessage(Component.literal(String.format("%s gave you %s %s for %s channel points!", payload.username(), effect.getDisplayName().getString(), strength, payload.cost())).withColor(TextColor.GREEN));

                    }
                }


                if (CONFIG.give_item_rewards.containsKey(payload.id())) {
                    ConfigModel.GiveItemReward itemReward = CONFIG.give_item_rewards.get(payload.id());
                    Item itemType = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemReward.item));
                    int amount = itemReward.amount;
                    String input = payload.input();

                    if (input.equalsIgnoreCase("")) input = itemType.getName(new ItemStack(itemType)).getString();
                    giveItem(payload.username(), input, itemType, amount, context.server(), payload.title(), payload.cost());
                }

                if (CoduhLink.CONFIG.action_rewards.containsKey(payload.id())) {
                    String action = CoduhLink.CONFIG.action_rewards.get(payload.id());

                    if (action.equalsIgnoreCase("repair-all")) {
                        ServerPlayer player = context.player();

                        int repaired = 0;

                        for (ItemStack stack : player.inventoryMenu.getItems()) {
                            if (!stack.getItem().equals(Items.AIR)) {
                                if (stack.has(DataComponents.DAMAGE) && (stack.get(DataComponents.DAMAGE)) > 0) {

                                    LOGGER.info("FOUND x" + stack.count() + " " + stack.getItemName().getString() + " to repair");
                                    stack.setDamageValue(0);
                                    repaired += 1;
                                }
                            }
                        }

                        if (repaired > 0) {
                            context.player().sendSystemMessage(CHAT_PREFIX.copy().append(Component.literal(String.format("%s repaired %s pieces of equipment for %s channel points.", payload.username(), repaired, payload.cost())).withColor(TextColor.GRAY).withStyle(ChatFormatting.ITALIC)));
                            context.player().sendOverlayMessage(Component.literal(String.format("%s repaired %s pieces of equipment", payload.username(), repaired)).withColor(TextColor.GREEN));

                            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Equipment Repaired!")));
                            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(String.format("%s repaired %s pieces of equipment for %s channel points.", payload.username(), repaired, payload.cost()))));
                        } else {
                            context.player().sendSystemMessage(CHAT_PREFIX.copy().append(Component.literal(String.format("%s attempted to repair your equipment for %s channel points, but you have not used any durability. This user should be refunded.", payload.username(), payload.cost())).withColor(TextColor.GRAY).withStyle(ChatFormatting.ITALIC)));
                            context.player().sendOverlayMessage(Component.literal("Failed to find equipment in need of repair.").withColor(TextColor.RED));
                        }
                    }

                    if (action.equalsIgnoreCase("time-day")) {
                        ServerLevel level = context.player().level();
                        Holder<WorldClock> clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
                        level.clockManager().setTotalTicks(clock, 0L);

                        LOGGER.info("Setting time to day...");
                        context.player().sendSystemMessage(CHAT_PREFIX.copy().append(Component.literal(String.format("%s has set the time to DAY for %s channel points.", payload.username(), payload.cost())).withColor(TextColor.GRAY).withStyle(ChatFormatting.ITALIC)));
                        context.player().sendOverlayMessage(Component.literal(String.format("%s set the time to DAY!", payload.username())).withColor(TextColor.GREEN));
                    }
                    if (action.equalsIgnoreCase("time-night")) {
                        ServerLevel level = context.player().level();
                        Holder<WorldClock> clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
                        level.clockManager().setTotalTicks(clock, 13000L);

                        LOGGER.info("Setting time to night...");
                        context.player().sendSystemMessage(CHAT_PREFIX.copy().append(Component.literal(String.format("%s has set the time to NIGHT for %s channel points.", payload.username(), payload.cost())).withColor(TextColor.GRAY).withStyle(ChatFormatting.ITALIC)));
                        context.player().sendOverlayMessage(Component.literal(String.format("%s set the time to NIGHT!", payload.username())).withColor(TextColor.GREEN));
                    }
                }
            }
        });


        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ticks += 1;
            for (int entityId : ANIMALS.keySet()) {
                boolean checked = ANIMALS.get(entityId);
                LOGGER.info("Entity #{} checked? {}", entityId, checked ? "Yes" : "No");
                if (checked) ANIMALS.remove(entityId);
            }

            if (RENAMING_ANIMAL != -1) {
                LOGGER.info("Renaming entity #{}", RENAMING_ANIMAL);
            }

            if (ticks == 10) {
                ticks = 0;
                if (!ANIMALS.isEmpty()) {
                    int entityId = ANIMALS.keySet().iterator().next();
                    if (ANIMAL_NAMES.containsKey(entityId)) {
                        String name = ANIMAL_NAMES.get(entityId);
                        Entity entity = server.getLevel(Level.OVERWORLD).getEntity(entityId);
                        if (entity != null && !entity.hasCustomName()) {
                            String entityName = entity.getName().getString();
                            for (ServerPlayer p : PlayerLookup.all(server)) {
                                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.format("%s Named!", entity.getName().getString()))));
                                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(String.format("%s named the %s \"%s\"", PLAYER_RENAMED.get(entityId), entity.getName().getString(), name))));
                                p.sendSystemMessage(CHAT_PREFIX.copy().append(Component.literal(String.format("%s named the %s \"%s\"", PLAYER_RENAMED.get(entityId), entityName, name)).withColor(TextColor.GRAY).withStyle(ChatFormatting.ITALIC)));
                            }

                            entity.setCustomName(Component.literal(name));
                            ANIMALS.put(entityId, true);
                            server.tickRateManager().setFrozen(false);
                            RENAMING_ANIMAL = -1;


                        }
                    }


                }
            }
        });

    }


}