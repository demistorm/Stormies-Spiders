package win.demistorm.stormiespiders.client.config;

import win.demistorm.stormiespiders.config.ModConfig;
import win.demistorm.stormiespiders.Constants;

public final class ClientConfigHelper {
    private ClientConfigHelper() {}

    public static void init() {
        // Load config on client side
        ModConfig.load();
        Constants.LOG.info("Client config initialized for Stormie's Spiders");
    }
}