package win.demistorm.stormiespiders.fabric;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import win.demistorm.stormiespiders.client.config.ConfigScreen;

public class FabricConfigScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen.SimpleToggleScreen::create;
    }
}