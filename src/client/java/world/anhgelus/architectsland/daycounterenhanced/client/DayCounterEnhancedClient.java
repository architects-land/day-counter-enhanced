package world.anhgelus.architectsland.daycounterenhanced.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.stat.Stats;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import world.anhgelus.architectsland.daycounterenhanced.Config;
import world.anhgelus.architectsland.daycounterenhanced.DayCounterEnhanced;

public class DayCounterEnhancedClient implements ClientModInitializer {
    private long connectedAt = -1;
    private long timeAlreadyPassed = 0;
    private boolean firstUpdateDone = false;

    public final Identifier HUD_ID = Identifier.of(DayCounterEnhanced.MOD_ID, "hud");

    @Override
    public void onInitializeClient() {
        final var playTimeStat = Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME);

        HudElementRegistry.addLast(HUD_ID, (context, tickCounter) -> {
            if (!MinecraftClient.isHudEnabled()) return;
            final var client = MinecraftClient.getInstance();
            if (client.inGameHud != null && client.getDebugHud().shouldShowDebugHud()) return;
            final var player = client.player;
            final var world = client.world;
            if (world == null || player == null) return;

            final long time;
            // server
            if (!client.isIntegratedServerRunning()) {
                if (!firstUpdateDone) {
                    timeAlreadyPassed = client.player.getStatHandler().getStat(playTimeStat);
                    firstUpdateDone = true;
                }
                time = timeConnected();
                // client
            } else {
                // get server player entity
                final var server = client.getServer();
                if (server == null) return;
                final var serverPlayer = server.getPlayerManager().getPlayer(player.getUuid());
                assert serverPlayer != null;
                // get and show time
                serverPlayer.getStatHandler().updateStatSet();
                time = serverPlayer.getStatHandler().getStat(playTimeStat);
            }
            draw(client.textRenderer, context, Math.floorDiv(time,24000)+1);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isIntegratedServerRunning()) return;
            timeAlreadyPassed = 0;
            DayCounterEnhanced.LOGGER.info("Fetching time passed...");
            handler.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
            connectedAt = System.currentTimeMillis();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.isIntegratedServerRunning()) return;
            if (connectedAt == -1) DayCounterEnhanced.LOGGER.warn("Connected at was not set");
            // reset
            connectedAt = -1;
            timeAlreadyPassed = 0;
            firstUpdateDone = false;
        });
    }

    private void draw(TextRenderer textRenderer, DrawContext context, long day) {
        int x = 5, y = 5;
        int xDecal = 33 + (int) Math.floor(Math.log10(day))*5;
        switch (Config.position) {
            case TOP_RIGHT -> x = context.getScaledWindowWidth() - xDecal;
            case BOTTOM_LEFT -> y = context.getScaledWindowHeight() - 15;
            case BOTTOM_RIGHT -> {
                x = context.getScaledWindowWidth() - xDecal;
                y = context.getScaledWindowHeight() - 15;
            }
        }
        context.drawTextWithShadow(textRenderer, "Day " + day, x, y, Colors.WHITE);
    }

    private long timeConnected() {
        return Math.floorDiv(System.currentTimeMillis() - connectedAt, 50) + timeAlreadyPassed; // counts each tick
    }
}
