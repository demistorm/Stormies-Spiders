package win.demistorm.stormiespiders.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import win.demistorm.stormiespiders.config.ModConfig;
import win.demistorm.stormiespiders.Constants;

public final class ConfigScreen {

    private ConfigScreen() {}

    public static class SimpleToggleScreen extends Screen {
        private final Screen parent;
        private final Minecraft client = Minecraft.getInstance();
        private boolean preventClimbingInRainValue = ModConfig.Data.preventClimbingInRain();
        private boolean canCrawlOnCeilingValue = ModConfig.Data.canCrawlOnCeiling();

        protected SimpleToggleScreen(Screen parent) {
            super(Component.literal("Stormie's Spiders Configuration"));
            this.parent = parent;
        }

        // Create screen for ModMenu
        public static SimpleToggleScreen create(Screen parent) {
            return new SimpleToggleScreen(parent);
        }

        @Override
        protected void init() {
            // Prevent climbing in rain button
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Prevent Climbing in Rain: " + (preventClimbingInRainValue ? "ON" : "OFF")),
                                    btn -> {
                                        preventClimbingInRainValue = !preventClimbingInRainValue;
                                        btn.setMessage(Component.literal(
                                                "Prevent Climbing in Rain: " + (preventClimbingInRainValue ? "ON" : "OFF")));
                                    })
                            .bounds(width / 2 - 80, height / 4 + 24, 160, 20)
                            .tooltip(Tooltip.create(Component.literal(
                                    "EXPERIMENTAL: When enabled, spiders will not climb on surfaces during rain")))
                            .build());

            // Ceiling crawling button
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Ceiling Crawling: " + (canCrawlOnCeilingValue ? "ON" : "OFF")),
                                    btn -> {
                                        canCrawlOnCeilingValue = !canCrawlOnCeilingValue;
                                        btn.setMessage(Component.literal(
                                                "Ceiling Crawling: " + (canCrawlOnCeilingValue ? "ON" : "OFF")));
                                    })
                            .bounds(width / 2 - 80, height / 4 + 48, 160, 20)
                            .tooltip(Tooltip.create(Component.literal(
                                    "Allow spiders to crawl on ceilings for extra fun")))
                            .build());

            // Disabled Climbing Blocks button
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Disabled Climbing Blocks..."),
                                    btn -> client.setScreen(new NonClimbableBlocksScreen(this)))
                            .bounds(width / 2 - 80, height / 4 + 72, 160, 20)
                            .tooltip(Tooltip.create(Component.literal(
                                    "Configure which blocks spiders cannot climb on (supports wildcards)")))
                            .build());

            // Rotation Overrides button
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Rotation Overrides..."),
                                    btn -> client.setScreen(new RotationOverridesScreen(this)))
                            .bounds(width / 2 - 80, height / 4 + 96, 160, 20)
                            .tooltip(Tooltip.create(Component.literal(
                                    "Configure rotation overrides and climbing behavior. " +
                                    "ROTATIONS ONLY mobs use spider rotations but without the pathfinding/etc, DISABLED spiders completely bypass the mod and " +
                                    "work like vanilla. (Useful for disabling modded spiders that " +
                                    "break with this mod and for adding rotations to mods like silverfish!)")))
                            .build());

            // Extras button
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Extras..."),
                                    btn -> client.setScreen(new ExtrasScreen.ExtrasToggleScreen(this)))
                            .bounds(width / 2 - 80, height / 4 + 120, 160, 20)
                            .tooltip(Tooltip.create(Component.literal(
                                    "Extra configuration options")))
                            .build());

            // Done button
            addRenderableWidget(
                    Button.builder(Component.literal("Done"),
                                    btn -> {
                                        // Save the settings
                                        ModConfig.Data.setPreventClimbingInRain(preventClimbingInRainValue);
                                        ModConfig.Data.setCanCrawlOnCeiling(canCrawlOnCeilingValue);

                                        Constants.LOG.info("Config saved: preventClimbingInRain = {}, canCrawlOnCeiling = {}", preventClimbingInRainValue, canCrawlOnCeilingValue);
                                        client.setScreen(parent);
                                    })
                            .bounds(width / 2 - 100, height - 27, 200, 20)
                            .build());
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            renderBackground(context);
            super.render(context, mouseX, mouseY, delta);
            // Draw title at top
            context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        }
    }
}