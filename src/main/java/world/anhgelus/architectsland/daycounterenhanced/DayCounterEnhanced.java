package world.anhgelus.architectsland.daycounterenhanced;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DayCounterEnhanced implements ModInitializer {

    public static final String MOD_ID = "daycounterenhanced";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        MidnightConfig.init(MOD_ID, Config.class);
        LOGGER.info("Day Counter Enhanced initialized");
    }
}
