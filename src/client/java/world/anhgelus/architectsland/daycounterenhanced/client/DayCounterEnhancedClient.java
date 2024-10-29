package world.anhgelus.architectsland.daycounterenhanced.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.stat.Stats;
import world.anhgelus.architectsland.daycounterenhanced.DayCounterEnhanced;

import java.util.concurrent.atomic.AtomicInteger;

public class DayCounterEnhancedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AtomicInteger i = new AtomicInteger();
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            final var client = MinecraftClient.getInstance();
            final var player = client.player;
            final var world = client.world;
            if (world == null || player == null) return;
            i.getAndIncrement();
            if (client.isIntegratedServerRunning()) {
                if (i.get() % 140 == 0) DayCounterEnhanced.LOGGER.info("{}", world.getTime());
                drawContext.drawTextWithShadow(client.textRenderer, "Day " + (Math.floorDiv(world.getTime(),20000)+1), 5, 5, 0xFFFFFF);
                return;
            }
            // not working
            final var time = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            if (i.get() % 140 == 0) DayCounterEnhanced.LOGGER.info("{}", time);
            drawContext.drawTextWithShadow(client.textRenderer, "Day " + (Math.floorDiv(time,200000)+1), 5, 5, 0xFFFFFF);
        });
//        ClientLifecycleEvents.CLIENT_STARTED.register(t -> {
//
//        });
    }
}
