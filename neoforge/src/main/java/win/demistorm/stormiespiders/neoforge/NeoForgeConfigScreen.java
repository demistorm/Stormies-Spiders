package win.demistorm.stormiespiders.neoforge;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import win.demistorm.stormiespiders.client.config.ConfigScreen;

public class NeoForgeConfigScreen {

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (minecraft, screen) -> ConfigScreen.SimpleToggleScreen.create(screen));
    }
}