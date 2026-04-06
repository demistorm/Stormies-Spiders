package win.demistorm.stormiespiders.forge;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import win.demistorm.stormiespiders.client.config.ConfigScreen;

public class ForgeConfigScreen {

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> ConfigScreen.SimpleToggleScreen.create(screen)));
    }
}
