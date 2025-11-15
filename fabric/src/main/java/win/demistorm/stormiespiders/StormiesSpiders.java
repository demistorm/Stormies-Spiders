package win.demistorm.stormiespiders;

import net.fabricmc.api.ModInitializer;

// Fabric entrypoint
public class StormiesSpiders implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Initializing Fabric version!");
        commonInit.init();
    }
}
