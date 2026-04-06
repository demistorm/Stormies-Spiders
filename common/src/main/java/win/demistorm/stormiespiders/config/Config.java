package win.demistorm.stormiespiders.config;

public class Config {

    public static final class COMMON {
        public static boolean preventClimbingInRain() {
            return ModConfig.Data.preventClimbingInRain();
        }

        public static boolean disableDataSync() {
            return ModConfig.Data.disableDataSync();
        }

        public static boolean canCrawlOnCeiling() {
            return ModConfig.Data.canCrawlOnCeiling();
        }

        public static boolean enableFallbackRotation() {
            return ModConfig.Data.enableFallbackRotation();
        }

        public static int fallbackUpdateInterval() {
            return ModConfig.Data.fallbackUpdateInterval();
        }

        public static boolean canSwim() {
            return ModConfig.Data.canSwim();
        }
    }
}
