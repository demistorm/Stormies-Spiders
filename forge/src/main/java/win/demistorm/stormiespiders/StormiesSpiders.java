package win.demistorm.stormiespiders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import win.demistorm.stormiespiders.forge.ForgeClientSetup;
import win.demistorm.stormiespiders.forge.ForgeConfigScreen;

@Mod(Constants.MODID)
public class StormiesSpiders {

    public StormiesSpiders() {
        Constants.LOG.info("Initialzing Forge version");
        commonInit.init();

        // Set up client side
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientSetup.doClientSetup();
            // Register config screen
            ForgeConfigScreen.register();
        }
    }
}
