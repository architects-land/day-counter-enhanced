package world.anhgelus.architectsland.daycounterenhanced.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import world.anhgelus.architectsland.daycounterenhanced.DayCounterEnhanced;

import java.io.IOException;

public class DayCounterEnhancedClient implements ClientModInitializer {
    private long connectedAt = -1;
    private String address = "";
    private long lastTimeConnected = 0;

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
                        if (!client.isIntegratedServerRunning()) {
                            draw(client, context, Math.floorDiv(timeConnected(),20*60*10)+1);
                            return;
                        }
                        // get server player entity
                        final var server = client.getServer();
                        if (server == null) return;
                        final var serverPlayer = server.getPlayerManager().getPlayer(player.getUuid());
                        assert serverPlayer != null;
                        // get and show time
                        final var time = serverPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
                        draw(client, context, Math.floorDiv(time,24000)+1);
                    }
            );
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isIntegratedServerRunning()) return;
            address = handler.getConnection().getAddressAsString(true);
            connectedAt = System.currentTimeMillis();
            lastTimeConnected = ClientStorage.get(address);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.isIntegratedServerRunning()) return;
            if (connectedAt == -1) throw new IllegalStateException("Connected at was not set");
            ClientStorage.set(address, timeConnected());
            // reset
            address = "";
            connectedAt = -1;
            lastTimeConnected = 0;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            try {
                ClientStorage.writeMap();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void draw(MinecraftClient client, DrawContext context, long day) {
        context.drawTextWithShadow(client.textRenderer, "Day " + day, 5, 5, 0xFFFFFF);
    }

    private long timeConnected() {
        return Math.floorDiv(System.currentTimeMillis() - connectedAt, 100) + lastTimeConnected; // counts each 0.1s
    }
}
