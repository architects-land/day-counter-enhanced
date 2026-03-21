package world.anhgelus.architectsland.daycounterenhanced.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.util.ARGB;
import world.anhgelus.architectsland.daycounterenhanced.Config;
import world.anhgelus.architectsland.daycounterenhanced.DayCounterEnhanced;

public class DayCounterEnhancedClient implements ClientModInitializer {
    private long connectedAt = -1;
    private long timeAlreadyPassed = 0;
    private boolean firstUpdateDone = false;

    public final Identifier HUD_ID = Identifier.fromNamespaceAndPath(DayCounterEnhanced.MOD_ID, "hud");

    @Override
    public void onInitializeClient() {
        final var playTimeStat = Stats.CUSTOM.get(Stats.PLAY_TIME);

        HudElementRegistry.addLast(HUD_ID, (context, _) -> {
            final var client = Minecraft.getInstance();
            if (client.options.hideGui || !Config.enabled) return;
            if (!Config.displayWhenF3 && client.getDebugOverlay().showDebugScreen()) return;
            final var player = client.player;
            final var world = client.level;
            if (world == null || player == null) return;

            final long time;
            // server
            if (!client.isLocalServer()) {
                if (!firstUpdateDone) {
                    timeAlreadyPassed = player.getStats().getValue(playTimeStat);
                    firstUpdateDone = true;
                }
                time = timeConnected();
                // client
            } else {
                // get server player entity
                final var server = client.getSingleplayerServer();
                if (server == null) return;
                final var serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
                assert serverPlayer != null;
                // get and show time
                serverPlayer.getStats().markAllDirty();
                time = serverPlayer.getStats().getValue(playTimeStat);
            }

            draw(context, client.font, Math.floorDiv(time,24000)+1);
        });

        ClientPlayConnectionEvents.JOIN.register((_, sender, client) -> {
            if (client.isLocalServer()) return;
            timeAlreadyPassed = 0;
            DayCounterEnhanced.LOGGER.info("Fetching time passed...");
            sender.sendPacket(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            connectedAt = System.currentTimeMillis();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((_, client) -> {
            if (client.isLocalServer()) return;
            if (connectedAt == -1) DayCounterEnhanced.LOGGER.warn("Connected at was not set");
            // reset
            connectedAt = -1;
            timeAlreadyPassed = 0;
            firstUpdateDone = false;
        });
    }

    private void draw(GuiGraphicsExtractor context, Font font, long day) {
        int x = 5, y = 5;
        int xDecal = 33 + (int) Math.floor(Math.log10(day))*5;
        switch (Config.position) {
            case TOP_RIGHT -> x = context.guiWidth() - xDecal;
            case BOTTOM_LEFT -> y = context.guiHeight() - 15;
            case BOTTOM_RIGHT -> {
                x = context.guiWidth() - xDecal;
                y = context.guiHeight() - 15;
            }
        }
        context.textWithBackdrop(font, Component.literal("Day " + day), x, y, 16, ARGB.color(255, 255, 255));
    }

    private long timeConnected() {
        return Math.floorDiv(System.currentTimeMillis() - connectedAt, 50) + timeAlreadyPassed; // counts each tick
    }
}
