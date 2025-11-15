package win.demistorm.stormiespiders.fabric;

import net.fabricmc.api.ClientModInitializer;
import win.demistorm.stormiespiders.client.config.ClientConfigHelper;

public class FabricClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Initialize client code
        ClientConfigHelper.init();
    }
}