package de.baum2dev.baum2;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.baum2dev.baum2.classes.ClassManager;
import de.baum2dev.baum2.commands.Baum2Commands;
import de.baum2dev.baum2.events.LevelUpHandler;
import de.baum2dev.baum2.events.MobDeathHandler;
import de.baum2dev.baum2.events.ProgressionTickHandler;
import de.baum2dev.baum2.networking.Baum2Networking;

public class Baum2 implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("baum2");

    @Override
    public void onInitialize() {
        LOGGER.info("Baum2 mod initializing...");

        Baum2Commands.registerCommands();
        Baum2Networking.registerServerPayloads();
        LevelUpHandler.registerEvents();
        MobDeathHandler.registerEvents();
        ProgressionTickHandler.registerEvents();
        ClassManager.registerEvents();

        LOGGER.info("Baum2 progression system loaded");
    }
}
