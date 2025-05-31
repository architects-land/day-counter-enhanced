package world.anhgelus.architectsland.daycounterenhanced.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import world.anhgelus.architectsland.daycounterenhanced.DayCounterEnhanced;

public class DayCounterEnhancedClient implements ClientModInitializer {
    private long connectedAt = -1;
    private long lastTimeConnected = 0;
    private boolean firstUpdateDone = false;

    public final Identifier HUD_ID = Identifier.of(DayCounterEnhanced.MOD_ID, "hud");;

    @Override
    public void onInitializeClient() {
        HudLayerRegistrationCallback.EVENT.register((drawContext) -> {
            drawContext.attachLayerAfter(
                    IdentifiedLayer.OVERLAY_MESSAGE,
                    HUD_ID,
                    (context, tickCounter) -> {
                        if (!MinecraftClient.isHudEnabled()) return;
                        final var client = MinecraftClient.getInstance();
                        if (client.inGameHud != null && client.getDebugHud().shouldShowDebugHud()) return;
                        final var player = client.player;
                        final var world = client.world;
                        if (world == null || player == null) return;
                        final var playTimeStat = Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME);
                        if (!client.isIntegratedServerRunning()) {
                            if (!firstUpdateDone) {
                                final var packet = new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS);
                                final var network = client.getNetworkHandler();
                                if (network == null) {
                                    DayCounterEnhanced.LOGGER.warn("Network handler is null");
                                    return;
                                }
                                network.sendPacket(packet);
                                lastTimeConnected = player.getStatHandler().getStat(playTimeStat);
                                firstUpdateDone = true;
                            }
                            draw(client, context, Math.floorDiv(timeConnected(),20*60*20)+1);
                            return;
                        }
                        // get server player entity
                        final var server = client.getServer();
                        if (server == null) return;
                        final var serverPlayer = server.getPlayerManager().getPlayer(player.getUuid());
                        assert serverPlayer != null;
                        // get and show time
                        final var time = serverPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
                        serverPlayer.getStatHandler().updateStatSet();
                        draw(client, context, Math.floorDiv(time,24000)+1);
                    }
            );
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isIntegratedServerRunning()) return;
            connectedAt = System.currentTimeMillis();
            lastTimeConnected = 0;
            firstUpdateDone = false;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.isIntegratedServerRunning()) return;
            if (connectedAt == -1) throw new IllegalStateException("Connected at was not set");
            // reset
            connectedAt = -1;
            lastTimeConnected = 0;
            firstUpdateDone = false;
        });
    }

    private void draw(MinecraftClient client, DrawContext context, long day) {
        context.drawTextWithShadow(client.textRenderer, "Day " + day, 5, 5, 0xFFFFFF);
    }

    private long timeConnected() {
        return Math.floorDiv(System.currentTimeMillis() - connectedAt, 50) + lastTimeConnected; // counts each tick
    }
}
