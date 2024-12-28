package world.anhgelus.architectsland.daycounterenhanced.client;

import net.fabricmc.loader.api.FabricLoader;
import world.anhgelus.architectsland.daycounterenhanced.DayCounterEnhanced;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ClientStorage {
    private static Map<String, Long> map = null;

    public static long get(String key) {
        if (map == null) initMap();
        assert map != null;
        return map.getOrDefault(key, 0L);
    }

    public static void set(String key, long value) {
        if (map == null) initMap();
        map.put(key, value);
    }

    private static void initMap() {
        map = new HashMap<>();
        final var file = path().toFile();
        Scanner scanner;
        try {
            if (file.createNewFile()) return;
            scanner = new Scanner(file);
        } catch (IOException e) {
            DayCounterEnhanced.LOGGER.error(e.toString());
            return;
        }
        while (scanner.hasNextLine()) {
            final var nl = scanner.nextLine();
            if (nl.equals("\n") || nl.isEmpty()) continue;
            final var splitted = nl.split("=");
            if (splitted.length != 2) continue;
            map.put(splitted[0].trim(), Long.parseLong(splitted[1].trim()));
        }
    }

    public static void writeMap() throws IOException {
        if (map == null) return;
        try (final var writer = new FileWriter(path().toString())) {
            for (var k : map.keySet()) {
                writer.write(k+"="+map.get(k)+"\n");
            }
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getGameDir().resolve("daycounterenhanced.properties");
    }
}
