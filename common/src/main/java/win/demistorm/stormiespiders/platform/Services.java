package win.demistorm.stormiespiders.platform;

import win.demistorm.stormiespiders.Constants;
import win.demistorm.stormiespiders.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

// Service loaders help find interface implementations that change between environments
public class Services {

    // Platform helper provides info about what platform the mod is running on
    // Like checking if code is running on Forge vs Fabric or if another mod is loaded
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);



    // This loads a service for the current environment
    // Service implementation must be defined manually in META-INF/services
    // File name should be the fully qualified class name of the service
    // Inside the file write the implementation class name for the platform
    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}