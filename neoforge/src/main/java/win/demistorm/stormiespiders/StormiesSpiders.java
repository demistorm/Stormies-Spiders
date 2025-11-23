package win.demistorm.stormiespiders;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import win.demistorm.stormiespiders.neoforge.NeoForgeClientSetup;
import win.demistorm.stormiespiders.neoforge.NeoForgeConfigScreen;

@Mod(Constants.MODID)
public class StormiesSpiders {

    public StormiesSpiders() {
        Constants.LOG.info("Initialzing NeoForge version");
        commonInit.init();

        // Set up client side
        if (FMLEnvironment.getDist().isClient()) {
            NeoForgeClientSetup.doClientSetup();
            // Register config screen
            NeoForgeConfigScreen.register();
        }
    }
}