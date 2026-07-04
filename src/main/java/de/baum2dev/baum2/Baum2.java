package de.baum2dev.baum2;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Baum2 implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("baum2");

    @Override
    public void onInitialize() {
        LOGGER.info("Baum2 mod initializing...");
    }
}
