package win.demistorm.stormiespiders.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import win.demistorm.stormiespiders.config.ModConfig;
import win.demistorm.stormiespiders.Constants;

public final class ExtrasScreen {

    private ExtrasScreen() {}

    public static class ExtrasToggleScreen extends Screen {
        private final Screen parent;
        private final Minecraft client = Minecraft.getInstance();
        private boolean canSwimValue = ModConfig.Data.canSwim();
        private boolean reducedAttackRangeValue = ModConfig.Data.reducedAttackRange();

        protected ExtrasToggleScreen(Screen parent) {
            super(Component.literal("Stormie's Spiders - Extras"));
            this.parent = parent;
        }

        public static ExtrasToggleScreen create(Screen parent) {
            return new ExtrasToggleScreen(parent);
        }

        @Override
        protected void init() {
            // Can Swim button
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Can Swim: " + (canSwimValue ? "ON" : "OFF")),
                                    btn -> {
                                        canSwimValue = !canSwimValue;
                                        btn.setMessage(Component.literal(
                                                "Can Swim: " + (canSwimValue ? "ON" : "OFF")));
                                    })
                            .bounds(width / 2 - 80, height / 4 + 24, 160, 20)
                            .tooltip(Tooltip.create(Component.literal(
                                    "When disabled, spiders cannot swim and will sink water")))
                            .build());

            // Reduced Attack Range button
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Reduced Attack Range: " + (reducedAttackRangeValue ? "ON" : "OFF")),
                                    btn -> {
                                        reducedAttackRangeValue = !reducedAttackRangeValue;
                                        btn.setMessage(Component.literal(
                                                "Reduced Attack Range: " + (reducedAttackRangeValue ? "ON" : "OFF")));
                                    })
                            .bounds(width / 2 - 80, height / 4 + 48, 160, 20)
                            .tooltip(Tooltip.create(Component.literal(
                                    "Spiders need to be closer to attack (50% of normal range)")))
                            .build());

            // Done button
            addRenderableWidget(
                    Button.builder(Component.literal("Done"),
                                    btn -> {
                                        ModConfig.Data.setCanSwim(canSwimValue);
                                        ModConfig.Data.setReducedAttackRange(reducedAttackRangeValue);

                                        Constants.LOG.info("Extras config saved: canSwim = {}, reducedAttackRange = {}", canSwimValue, reducedAttackRangeValue);
                                        client.setScreen(parent);
                                    })
                            .bounds(width / 2 - 100, height - 27, 200, 20)
                            .build());
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            renderBackground(context);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        }
    }
}
