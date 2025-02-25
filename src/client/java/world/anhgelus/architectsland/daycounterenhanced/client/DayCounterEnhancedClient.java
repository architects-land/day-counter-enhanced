package world.anhgelus.architectsland.daycounterenhanced.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.stat.Stats;

import java.io.IOException;

public class DayCounterEnhancedClient implements ClientModInitializer {
    private long connectedAt = -1;
    private String address = "";
    private long lastTimeConnected = 0;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!MinecraftClient.isHudEnabled()) return;
            final var client = MinecraftClient.getInstance();
            if (client.getDebugHud().shouldShowDebugHud()) return;
            final var player = client.player;
            final var world = client.world;
            if (world == null || player == null) return;
            if (!client.isIntegratedServerRunning()) {
                drawContext.drawTextWithShadow(client.textRenderer, "Day " + (Math.floorDiv(timeConnected(),20*60*10)+1), 5, 5, 0xFFFFFF);
                return;
            }
            // get server player entity
            final var server = client.getServer();
            if (server == null) return;
            final var serverPlayer = server.getPlayerManager().getPlayer(player.getUuid());
            assert serverPlayer != null;
            // get and show time
            final var time = serverPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            drawContext.drawTextWithShadow(client.textRenderer, "Day " + (Math.floorDiv(time,20000)+1), 5, 5, 0xFFFFFF);
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

    private long timeConnected() {
        return Math.floorDiv(System.currentTimeMillis() - connectedAt, 100) + lastTimeConnected; // counts each 0.1s
    }
}
