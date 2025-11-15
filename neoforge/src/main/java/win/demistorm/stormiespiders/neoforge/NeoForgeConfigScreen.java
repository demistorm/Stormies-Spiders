package win.demistorm.stormiespiders.neoforge;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import win.demistorm.stormiespiders.client.config.ConfigScreen;

public class NeoForgeConfigScreen {

    public static void register() {
        // Register config screen
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) ->
                ConfigScreen.SimpleToggleScreen.create(screen)));
    }
}