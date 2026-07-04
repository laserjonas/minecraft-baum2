package de.baum2dev.baum2;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.baum2dev.baum2.commands.Baum2Commands;
import de.baum2dev.baum2.events.LevelUpHandler;
import de.baum2dev.baum2.events.MobDeathHandler;
import de.baum2dev.baum2.events.ProgressionTickHandler;
import de.baum2dev.baum2.networking.Baum2Networking;
import de.baum2dev.baum2.progression.PlayerLevelSystem;

public class Baum2 implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("baum2");

    @Override
    public void onInitialize() {
        LOGGER.info("Baum2 mod initializing...");

        // Must run first: registers the progression AttachmentType before any player can
        // possibly join and have their saved data deserialized. See PlayerLevelSystem.bootstrap().
        PlayerLevelSystem.bootstrap();

        Baum2Commands.registerCommands();
        Baum2Networking.registerServerPayloads();
        LevelUpHandler.registerEvents();
        MobDeathHandler.registerEvents();
        ProgressionTickHandler.registerEvents();

        LOGGER.info("Baum2 progression system loaded");
    }
}
