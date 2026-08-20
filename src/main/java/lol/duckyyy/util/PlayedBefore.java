package lol.duckyyy.util;

import lol.duckyyy.CoduhLink;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class PlayedBefore {
    private static final String FILE_NAME = String.format("%s_has_played_before.txt", CoduhLink.MOD_ID);
    public static Path PATH;

    public PlayedBefore() {
        PATH  = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void set(boolean value) {
        try {
            Files.writeString(PATH, String.valueOf(value));
        } catch (Exception e) {
            CoduhLink.LOGGER.error(e.getMessage());
        }
    }

    public static boolean get() {
        if(!Files.exists(PATH)) {
            return false;
        }

        try {
            return Boolean.parseBoolean(Files.readString(PATH));
        } catch(Exception e) {
            CoduhLink.LOGGER.error(e.getMessage());
            return false;
        }
    }

    public PlayedBefore init() {
        return this;
    }
}
