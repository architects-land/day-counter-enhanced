package world.anhgelus.architectsland.daycounterenhanced;

import eu.midnightdust.lib.config.MidnightConfig;

public class Config extends MidnightConfig {
    public enum Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    @Entry public static Position position = Position.TOP_LEFT;
}
