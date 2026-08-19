package lol.duckyyy.client;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelJoinEvent;
import com.github.twitch4j.chat.events.channel.ChannelLeaveEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.RaidEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import com.github.twitch4j.eventsub.domain.Reward;
import com.github.twitch4j.eventsub.events.CustomRewardRedemptionAddEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.ChannelInformation;
import com.github.twitch4j.helix.domain.Chatter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import lol.duckyyy.CoduhLink;
import lol.duckyyy.api.ClientboundRenameAnimalPayload;
import lol.duckyyy.api.ServerboundRaidPayload;
import lol.duckyyy.client.api.ApiResponse;
import lol.duckyyy.api.ServerboundRewardRedemptionPayload;
import lol.duckyyy.client.api.SessionResponse;
import lol.duckyyy.client.screen.KeybindHelpScreen;
import lol.duckyyy.client.screen.RenameAnimalConfirmScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CoduhLinkClient implements ClientModInitializer {
    public static String ACCESS_TOKEN = "";
    public static String USER_ID = "";
    public static String USER_NAME = "";
    public String ERROR = "";
    public boolean JOIN_NOTIFIED = false;
    Set<String> POSSIBLE_ACTIONS = new HashSet<String>();
    public static KeyMapping HELP_KEYBIND;

    public static void showToast(String title, String body) {
        try {
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().font != null) {
                    SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal(title), Component.literal(body));
                } else {
                    CoduhLink.LOGGER.warn("Failed to show toast.");

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            CoduhLink.LOGGER.warn("Failed to show toast.");
        }
    }

    private void summonEntity(String username, String entityName, EntityType<?> type, Minecraft instance, Reward reward) {
        Entity entity = type.spawn(instance.getSingleplayerServer().getLevel(Level.OVERWORLD), instance.player.blockPosition(), EntitySpawnReason.COMMAND);
        assert entity != null;
        if(!(entity instanceof TamableAnimal)) entity.setCustomName(Component.literal(entityName));
        instance.player.sendOverlayMessage(Component.literal(String.format("%s spawned by %s!", entity.getType().toShortString().replace(String.valueOf(entity.getType().toShortString().charAt(0)), String.valueOf(entity.getType().toShortString().charAt(0)).toUpperCase(Locale.ROOT)), username)).withColor(CommonColors.GREEN));
    }


    public boolean isModerator(Set<CommandPermission> permissions) {
        boolean toReturn = permissions.contains(CommandPermission.MODERATOR);
        if(permissions.contains(CommandPermission.BROADCASTER)) toReturn = true;

        return toReturn;
    }

    public boolean isVIPOrMod(Set<CommandPermission> permissions) {
        boolean toReturn = permissions.contains(CommandPermission.VIP);
        if(permissions.contains(CommandPermission.MODERATOR)) toReturn = true;
        if(permissions.contains(CommandPermission.BROADCASTER)) toReturn = true;

        return toReturn;
    }

    public boolean isSubscriberOrMod(Set<CommandPermission> permissions) {
        boolean toReturn = permissions.contains(CommandPermission.SUBSCRIBER);
        if(permissions.contains(CommandPermission.MODERATOR)) toReturn = true;
        if(permissions.contains(CommandPermission.BROADCASTER)) toReturn = true;

        return toReturn;
    }

    @Override
    public void onInitializeClient() {
        CoduhLink.LOGGER.info("STARTING CODUHLINK");
        POSSIBLE_ACTIONS.add("time-day");
        POSSIBLE_ACTIONS.add("time-night");

        KeyMapping.Category KEYMAP_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CoduhLink.MOD_ID, "keybinds"));
        this.HELP_KEYBIND = KeyMappingHelper.registerKeyMapping(new KeyMapping(String.format("key.%s.help",CoduhLink.MOD_ID), InputConstants.Type.KEYSYM, InputConstants.KEY_PERIOD, KEYMAP_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(this.HELP_KEYBIND.consumeClick() && client.hasControlDown()) {
                if(client.player != null && client.hasSingleplayerServer()) {
                    CoduhLink.LOGGER.info("help key pressed");
                    if(!(client.gui.screen() instanceof KeybindHelpScreen)) {
                        client.gui.setScreen(new KeybindHelpScreen());
                    } else client.gui.setScreen(null);
                } else {
                    CoduhLink.LOGGER.info("Key pressed but can not show screen");
                }
            }
        });

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format("%s?key=%s", CoduhLink.CONFIG.api_url, CoduhLink.CONFIG.api_key))).GET().build();

            CoduhLink.LOGGER.info(String.format("GET %s", String.format("%s?key=%s", CoduhLink.CONFIG.api_url, CoduhLink.CONFIG.api_key)));

            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonString = res.body();

            if (res.statusCode() != 200 || jsonString.equalsIgnoreCase("null")) {
                ACCESS_TOKEN = "";
                ERROR = String.format("The server responded with a status code %s", res.statusCode());
            }

            Gson gson = new GsonBuilder().create();


            Type responseType = new TypeToken<ApiResponse<SessionResponse.AsUser>>() {
            }.getType();
            ApiResponse<SessionResponse.AsUser> response;

            try {
                response = gson.fromJson(jsonString, responseType);
            } catch (Exception e) {
                response = null;
                CoduhLink.LOGGER.error("Failed to fetch session details");
            }

            if (response == null) {
                ACCESS_TOKEN = "";
                ERROR = "Failed to fetch session details";
            }

            if (response != null) CoduhLink.LOGGER.info("JSON\n" + jsonString);

            if (response != null && response.data.access_token != null) {
                ACCESS_TOKEN = response.data.access_token;
                USER_ID = response.data.user.id;
                USER_NAME = response.data.user.login;
            }


            if (response != null)
                CoduhLink.LOGGER.info(String.format("TOKEN (FROM JSON): %s", response.data.access_token));
            CoduhLink.LOGGER.info(String.format("TOKEN: %s", ACCESS_TOKEN));
            CoduhLink.LOGGER.info(String.format("USER ID: %s", USER_ID));
            CoduhLink.LOGGER.info(String.format("USERNAME: %s", USER_NAME));
            CoduhLink.LOGGER.info(String.format("ERROR: %s", ERROR));

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        if (ACCESS_TOKEN != null && ACCESS_TOKEN.equalsIgnoreCase("")) {
            showToast("Twitch Authorization Failed", "An invalid access token was provided. Twitch features have been disabled.");
            if (!ERROR.equalsIgnoreCase("")) showToast("Twitch Authorization Failed", ERROR);
            CoduhLink.TWITCH_ENABLED = false;
        } else {
            CoduhLink.twitchClient = TwitchClientBuilder.builder().withEnableChat(true).withEnableEventSocket(true).withEnableHelix(true).withDefaultAuthToken(new OAuth2Credential("twitch", ACCESS_TOKEN)).build();

            CoduhLink.twitchClient.getEventSocket().connect();
            CoduhLink.twitchClient.getChat().connect();
            CoduhLink.twitchClient.getChat().joinChannel(USER_NAME);

            ClientPlayNetworking.registerGlobalReceiver(ClientboundRenameAnimalPayload.TYPE, (p, context) -> {
                CoduhLink.LOGGER.info("Packet Received - Animal Tamed");
                ClientboundRenameAnimalPayload payload = (ClientboundRenameAnimalPayload) p;
                if (Minecraft.getInstance().level != null) {
                    Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
                    if (entity != null) {
                        if (entity instanceof TamableAnimal) {

                            RenameAnimalConfirmScreen screen = new RenameAnimalConfirmScreen(Component.literal(String.format("Renaming %s", entity.getName().getString())).withColor(TextColor.GREEN).withStyle(ChatFormatting.BOLD), Component.literal("The first person to run !name in chat will name this animal."), Component.empty(), entity.getId());

                            Minecraft.getInstance().gui.setScreen(screen);
                        }
                    }
                }
            });

            CoduhLink.twitchClient.getEventManager().onEvent(ChannelJoinEvent.class, ev -> {
                CoduhLink.LOGGER.info(String.format("Successfully joined #%s!", ev.getChannel().getName()));
                if (!JOIN_NOTIFIED) {
                    showToast("Twitch Chat Joined", String.format("Successfully joined #%s!", ev.getChannel().getName()));
                    JOIN_NOTIFIED = true;
                }
            });

            CoduhLink.twitchClient.getEventManager().onEvent(ChannelLeaveEvent.class, ev -> {
                if (JOIN_NOTIFIED) {
                    showToast("Twitch Chat Disconnected", String.format("Parted from #%s", ev.getChannel().getName()));
                    JOIN_NOTIFIED = false;
                }
            });

            CoduhLink.twitchClient.getEventManager().onEvent(EventSocketSubscriptionFailureEvent.class, ev -> {
                CoduhLink.LOGGER.warn("Error occurred subscribing to EventSub " + ev.getError().getMessage());
            });

            CoduhLink.twitchClient.getEventManager().onEvent(EventSocketSubscriptionSuccessEvent.class, ev -> {
                CoduhLink.LOGGER.info("Subscribed to EventSub " + ev.getSubscription().getType().getName());

                showToast("EventSub Subscription", String.format("Subscribed to event \"%s\"", ev.getSubscription().getType().getName()));
            });

            CoduhLink.twitchClient.getEventManager().onEvent(RaidEvent.class, ev -> {
                ChannelInformation channel = CoduhLink.twitchClient.getHelix().getChannelInformation(ACCESS_TOKEN, List.of(ev.getRaider().getId())).execute().getChannels().getFirst();
                if(channel == null) return;
                ServerboundRaidPayload payload = new ServerboundRaidPayload(channel.getBroadcasterName(), ev.getViewers(), channel.getGameName());
                ClientPlayNetworking.send(payload);
                CoduhLink.LOGGER.info(String.format("%s raided with %s viewers playing %s", channel.getBroadcasterName(), ev.getViewers(), channel.getGameName()));
            });

            CoduhLink.twitchClient.getEventSocket().register(SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD.prepareSubscription(b -> b.broadcasterUserId(USER_ID).build(), null));

            try {
                CoduhLink.twitchClient.getEventManager().onEvent(CustomRewardRedemptionAddEvent.class, ev -> {
                    String customRewardId = ev.getReward().getId();
                    CoduhLink.LOGGER.info(customRewardId);
                    Reward reward = ev.getReward();

                    ServerboundRewardRedemptionPayload payload = new ServerboundRewardRedemptionPayload(customRewardId, ev.getUserName(), reward.getTitle(), ev.getUserInput(), ev.getReward().getCost());
                    ClientPlayNetworking.send(payload);

                    Minecraft.getInstance().execute(() -> {
                        if (!Minecraft.getInstance().hasSingleplayerServer()) return;

                        if (CoduhLink.CONFIG.summon_rewards.containsKey(customRewardId)) {
                            String entityId = CoduhLink.CONFIG.summon_rewards.get(customRewardId);
                            String[] split = entityId.split(":");
                            String key = split[0];
                            String id = split[1];
                            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entityId));
                            List<Chatter> chatters = CoduhLink.twitchClient.getHelix().getChatters(ACCESS_TOKEN, USER_ID, USER_ID, 100, null).execute().getChatters().stream().filter(c -> !CoduhLink.BOT_USER_NAMES.contains(c.getUserName().toLowerCase()) && !c.getUserName().equalsIgnoreCase(USER_NAME)).toList();

                            String entityName;

                            try {
                                int random = new Random().nextInt(chatters.size());
                                Chatter chatter = chatters.get(random);
                                if(chatter == null) chatter = chatters.getFirst();

                                entityName = chatter.getUserName();
                            } catch(Exception e) {
                                entityName = ev.getUserName();
                            }
                            if (!ev.getUserInput().equalsIgnoreCase("")) entityName = ev.getUserInput().trim();


                            summonEntity(ev.getUserName(), entityName, entityType, Minecraft.getInstance(), reward);
                        }
                    });
                });
            } catch (Exception e) {
                CoduhLink.LOGGER.error(e.getMessage());
            }

            CoduhLink.twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, ev -> {

                if (ev.getMessage().toLowerCase().startsWith("!quakepro") && isModerator(ev.getPermissions())) {
                    int oldFov = Minecraft.getInstance().options.fov().get();
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().options.fov().set(110);
                    });

                    String[] split = ev.getMessage().split(" ");
                    String providedAmount = "10";
                    if(split.length > 1) providedAmount = split[1];
                    int modifiedSeconds = Integer.parseInt(providedAmount.trim());
                    if(modifiedSeconds < 10) modifiedSeconds = 10;


                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.sendOverlayMessage(Component.literal(String.format("QUAKE PRO! FOV Modified for %s seconds by %s", modifiedSeconds, ev.getUser().getName())).withColor(TextColor.YELLOW));

                        CompletableFuture.delayedExecutor(modifiedSeconds - 3, TimeUnit.SECONDS).execute(() -> {
                            player.sendOverlayMessage(Component.literal("FOV resets in 3 seconds...").withColor(TextColor.YELLOW));
                        });

                        CompletableFuture.delayedExecutor(modifiedSeconds - 2, TimeUnit.SECONDS).execute(() -> {
                            player.sendOverlayMessage(Component.literal("FOV resets in 2 seconds...").withColor(TextColor.YELLOW));
                        });

                        CompletableFuture.delayedExecutor(modifiedSeconds - 1, TimeUnit.SECONDS).execute(() -> {
                            player.sendOverlayMessage(Component.literal("FOV resets in 1 second...").withColor(TextColor.YELLOW));
                        });

                        CompletableFuture.delayedExecutor(modifiedSeconds, TimeUnit.SECONDS).execute(() -> {
                            Minecraft.getInstance().execute(() -> {
                                Minecraft.getInstance().options.fov().set(oldFov);
                                player.sendOverlayMessage(Component.literal(String.format("FOV Reset to %s...", oldFov)).withColor(TextColor.YELLOW));

                            });
                        });
                    }
                }

//                if(!ev.getMessage().startsWith("!") && !ev.getUser().getName().equalsIgnoreCase("shortbotduh")) {
//                    Optional<String> hex = ev.getMessageEvent().getUserChatColor();
//                    String hexCode = (hex.orElse("#FFFFFF")).replaceAll("#", "0x");
//                    int color = Integer.decode(hexCode);
//                    ClientPlayNetworking.send(new ServerboundChatMessagePayload(ev.getUser().getName(), ev.getMessage(), color));
//                }


                if (ev.getMessage().toLowerCase().startsWith("!name")) {
                    String[] split = ev.getMessage().split(" ");
                    if (split.length <= 1) return;
                    String name = String.join(" ", Arrays.copyOfRange(split, 1, split.length));

                    if (CoduhLink.RENAMING_ANIMAL != -1) {
                        CoduhLink.LOGGER.info("!name:{}({})", ev.getMessage(), name);
                        CoduhLink.ANIMAL_NAMES.put(CoduhLink.RENAMING_ANIMAL, name);
                        CoduhLink.PLAYER_RENAMED.put(CoduhLink.RENAMING_ANIMAL, ev.getUser().getName());
                    }

                }
            });
        }
    }
}