package win.demistorm.stormiespiders.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;

import java.util.Map;

public class RotationOverridesScreen extends Screen {
	private final Screen parent;
	private final Minecraft client = Minecraft.getInstance();

	private EditBox entityIdInput;
	private EntityListWidget entityList;

	private int topY;
	private int listTopY;

	private Map<String, Boolean> entityOverrides;

	private static final int WIDGET_HEIGHT = 20;

	protected RotationOverridesScreen(Screen parent) {
		super(Component.literal("Configure Rotation Overrides"));
		this.parent = parent;
		this.entityOverrides = RotationOverrideConfig.getRotationOverrideMap();
	}

	@Override
	protected void init() {
		topY = 55;
		listTopY = topY + 45;
		int bottomMargin = 47;

		entityIdInput = new EditBox(
			font,
			20,
			topY,
			180,
			18,
			Component.literal("Entity ID")
		);
		entityIdInput.setHint(Component.literal("minecraft:silverfish"));
		entityIdInput.setMaxLength(100);
		addRenderableWidget(entityIdInput);

		addRenderableWidget(
			Button.builder(
				Component.literal("Add"),
				btn -> addEntityId())
			.bounds(205, topY, 40, 18)
			.build());

		addRenderableWidget(
			Button.builder(
				Component.literal("?"),
				btn -> {})
			.bounds(250, topY, 18, 18)
			.tooltip(Tooltip.create(Component.literal(
				"Wildcard Syntax:\n" +
				"  *skeleton - Anything ending with 'skeleton'\n" +
				"  *zombie* - Anything with 'zombie'\n" +
				"  minecraft:* - All minecraft entities\n" +
				"  Exact: minecraft:silverfish\n\n" +
				"ROTATIONS ONLY: Apply rotations to this mob (disables mod's pathfinding/etc)\n" +
				"DISABLED: Disable mod's features for this mob"
			)))
			.build());

		addRenderableWidget(
			Button.builder(
				Component.literal("Clear All"),
				btn -> {
					entityOverrides.clear();
					entityList.updateEntries();
					RotationOverrideConfig.setRotationOverrideMap(entityOverrides);
				})
			.bounds(width - 180, topY, 160, 18)
			.tooltip(Tooltip.create(Component.literal("Remove all entities from the list")))
			.build());

		int listBottom = height - bottomMargin;
		entityList = new EntityListWidget(client, width, height, listTopY, listBottom);
		entityList.updateEntries();
		addWidget(entityList);

		addRenderableWidget(
			Button.builder(
				Component.literal("Done"),
				btn -> client.setScreen(parent))
			.bounds(width / 2 - 100, height - 27, 200, 20)
			.build());
	}

	private void addEntityId() {
		String text = entityIdInput.getValue().trim();
		if (!text.isEmpty() && !entityOverrides.containsKey(text)) {
			boolean defaultEnabled = RotationOverrideConfig.isSmartDefaultEnabled(text);
			entityOverrides.put(text, defaultEnabled);
			entityIdInput.setValue("");
			entityList.updateEntries();
			RotationOverrideConfig.setRotationOverrideMap(entityOverrides);
		}
	}

	private void removeEntity(String entityId) {
		entityOverrides.remove(entityId);
		entityList.updateEntries();
		RotationOverrideConfig.setRotationOverrideMap(entityOverrides);
	}

	private void toggleEntity(String entityId) {
		Boolean current = entityOverrides.get(entityId);
		if (current != null) {
			entityOverrides.put(entityId, !current);
			entityList.updateEntries();
			RotationOverrideConfig.setRotationOverrideMap(entityOverrides);
		}
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);

		if (entityList != null) {
			entityList.render(context, mouseX, mouseY, delta);
		}

		context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
		context.drawString(font, "Add Entity ID or Wildcard:", 20, topY - 12, 0xFFFFFF);
		context.drawString(font, "Rotation Override Entities:", 20, listTopY - 12, 0xFFFFFF);
		context.drawString(font, "(" + entityOverrides.size() + " entries)", 175, listTopY - 12, 0xAAAAAA);

		String instructions = "ROTATIONS ONLY = Apply rotations | DISABLED = Disable mod's features";
		context.drawCenteredString(font, instructions, width / 2, height - 75, 0x808080);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (entityIdInput.isFocused() && (keyCode == 257 || keyCode == 335)) {
			addEntityId();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private class EntityListWidget extends ObjectSelectionList<EntityListWidget.EntityEntry> {

		public EntityListWidget(Minecraft client, int width, int height, int y, int bottom) {
			super(client, width, bottom - y, y, WIDGET_HEIGHT + 4);
		}

		public void updateEntries() {
			clearEntries();

			for (Map.Entry<String, Boolean> entry : entityOverrides.entrySet()) {
				addEntry(new EntityEntry(entry.getKey(), entry.getValue()));
			}
		}

		@Override
		public int getRowWidth() {
			return width - 40;
		}

		public class EntityEntry extends ObjectSelectionList.Entry<EntityEntry> {
			private final String entityId;
			private final boolean enabled;
			private final Button toggleButton;
			private final Button removeButton;

			public EntityEntry(String entityId, boolean enabled) {
				this.entityId = entityId;
				this.enabled = enabled;

				Component toggleTooltip = Component.literal(
					"ROTATIONS ONLY: Apply rotations to this mob (disables mod's pathfinding/etc)\n" +
					"DISABLED: Disable mod's features for this mob");

				this.toggleButton = Button.builder(
					Component.literal(enabled ? "ROTATIONS ONLY" : "DISABLED"),
					btn -> toggleEntity(entityId))
				.bounds(0, 0, 95, WIDGET_HEIGHT)
				.tooltip(Tooltip.create(toggleTooltip))
				.build();

				this.removeButton = Button.builder(
					Component.literal("\u00d7"),
					btn -> removeEntity(entityId))
				.bounds(0, 0, 13, 13)
				.tooltip(Tooltip.create(Component.literal("Remove " + entityId)))
				.build();
			}

			@Override
			public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
				int removeX = x + entryWidth - 18;
				int removeY = y + (entryHeight - 13) / 2;
				removeButton.setPosition(removeX, removeY);
				removeButton.render(context, mouseX, mouseY, delta);

				int toggleX = removeX - 95 - 5;
				int toggleY = y + (entryHeight - WIDGET_HEIGHT) / 2;
				toggleButton.setPosition(toggleX, toggleY);
				toggleButton.render(context, mouseX, mouseY, delta);

				int fixedRightWidth = 95 + 13 + 20;
				int maxLabelWidth = entryWidth - fixedRightWidth;
				String display = entityId;
				if (font.width(display) > maxLabelWidth) {
					display = font.plainSubstrByWidth(display, maxLabelWidth - 15) + "...";
				}

				int nameColor = enabled ? 0xFFFFFF : 0xAAAAAA;
				context.drawString(font, Component.literal(display).withStyle(style -> style.withUnderlined(true)), x + 5, y + 6, nameColor);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (toggleButton.mouseClicked(mouseX, mouseY, button)) {
					return true;
				}
				if (removeButton.mouseClicked(mouseX, mouseY, button)) {
					return true;
				}
				return false;
			}

			@Override
			public Component getNarration() {
				return Component.literal(entityId + " - " + (enabled ? "ROTATIONS ONLY" : "DISABLED"));
			}
		}
	}
}
