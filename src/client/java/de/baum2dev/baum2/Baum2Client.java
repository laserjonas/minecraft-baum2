package de.baum2dev.baum2;

import net.fabricmc.api.ClientModInitializer;
import de.baum2dev.baum2.networking.ClientNetworkingHandler;

public class Baum2Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Baum2.LOGGER.info("Baum2 client initializing...");
        ClientNetworkingHandler.registerClientHandlers();
    }
}
