package win.demistorm.stormiespiders.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import win.demistorm.stormiespiders.config.NonClimbableBlocksConfig;

import java.util.ArrayList;
import java.util.List;

// Screen for managing non-climbable blocks
public class NonClimbableBlocksScreen extends Screen {
	private final Screen parent;
	private final Minecraft client = Minecraft.getInstance();

	// UI Components
	private EditBox blockIdInput;
	private BlockListWidget blockList;

	// Layout
	private int listTopY;

	// Data
	private List<String> nonClimbableBlocks;

	protected NonClimbableBlocksScreen(Screen parent) {
		super(Component.literal("Configure Disabled Climbing Blocks"));
		this.parent = parent;

		// Load current data
		this.nonClimbableBlocks = NonClimbableBlocksConfig.getNonClimbableBlocksList();
	}

	@Override
	protected void init() {
		int topY = 40;
		int listTopY = topY + 80; // More space for controls
		int bottomMargin = 70; // Space at bottom for Done button

		// Text input for new blocks (left side)
		blockIdInput = new EditBox(
			font,
			20,
			topY,
			180,
			18,
			Component.literal("Block ID")
		);
		blockIdInput.setHint(Component.literal("*trapdoor"));
		blockIdInput.setMaxLength(100);
		addRenderableWidget(blockIdInput);

		// Add button (next to text input)
		addRenderableWidget(
			Button.builder(
				Component.literal("Add"),
				btn -> addBlockId())
			.bounds(205, topY, 40, 18)
			.build());

		// Wildcard help button
		addRenderableWidget(
			Button.builder(
				Component.literal("?"),
				btn -> {})
			.bounds(250, topY, 18, 18)
			.tooltip(Tooltip.create(Component.literal(
				"Wildcard Syntax:\n" +
				"  *trapdoor - All trapdoors\n" +
				"  *door* - Anything with 'door'\n" +
				"  minecraft:* - All minecraft blocks\n" +
				"  Exact: minecraft:oak_trapdoor"
			)))
			.build());

		// Clear all button (right side)
		addRenderableWidget(
			Button.builder(
				Component.literal("Clear All"),
				btn -> {
					nonClimbableBlocks.clear();
					blockList.updateEntries();
					NonClimbableBlocksConfig.setNonClimbableBlocksList(nonClimbableBlocks);
				})
			.bounds(width - 180, topY, 160, 18)
			.tooltip(Tooltip.create(Component.literal("Remove all blocks from the list")))
			.build());

		// Create scrollable block list (two columns)
		int listBottom = height - bottomMargin;
		blockList = new BlockListWidget(client, width, height, listTopY, listBottom);
		blockList.updateEntries();
		addWidget(blockList);

		// Done button at bottom
		addRenderableWidget(
			Button.builder(
				Component.literal("Done"),
				btn -> client.setScreen(parent))
			.bounds(width / 2 - 50, height - 50, 100, 20)
			.build());
	}

	private void addBlockId() {
		String text = blockIdInput.getValue().trim();
		if (!text.isEmpty() && !nonClimbableBlocks.contains(text)) {
			nonClimbableBlocks.add(text);
			blockIdInput.setValue("");
			blockList.updateEntries();

			// Save immediately
			NonClimbableBlocksConfig.setNonClimbableBlocksList(nonClimbableBlocks);
		}
	}

	private void removeBlockId(String blockId) {
		nonClimbableBlocks.remove(blockId);
		blockList.updateEntries();

		// Save immediately
		NonClimbableBlocksConfig.setNonClimbableBlocksList(nonClimbableBlocks);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		// Render all widgets (buttons, text input) through super
		super.render(context, mouseX, mouseY, delta);

		// Render the block list manually
		if (blockList != null) {
			blockList.render(context, mouseX, mouseY, delta);
		}

		// Title at top
		context.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);

		// Label for text input
		context.drawString(font, "Add Block ID or Wildcard:", 20, 28, 0xFFFFFF);

		// Label for block list
		context.drawString(font, "Non-Climbable Blocks:", 20, listTopY - 10, 0xFFFFFF);

		// Show count of blocks
		context.drawString(font, "(" + nonClimbableBlocks.size() + " entries)", 160, listTopY - 10, 0xAAAAAA);

		// Instructions at bottom
		String instructions = "Wildcards: *trapdoor matches all trapdoors, *door* matches anything with 'door'";
		context.drawCenteredString(font, instructions, width / 2, height - 75, 0x808080);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Handle Enter key in text field
		if (blockIdInput.isFocused() && (event.key() == 257 || event.key() == 335)) {
			addBlockId();
			return true;
		}
		return super.keyPressed(event);
	}

	// Custom list widget for displaying blocks in two columns
	private class BlockListWidget extends ObjectSelectionList<BlockListWidget.BlockEntry> {

		public BlockListWidget(Minecraft client, int width, int height, int y, int bottom) {
			super(client, width, bottom - y, y, 20);
		}

		public void updateEntries() {
			clearEntries();

			// Add blocks in pairs for two-column layout
			for (int i = 0; i < nonClimbableBlocks.size(); i += 2) {
				String leftBlock = nonClimbableBlocks.get(i);
				String rightBlock = i + 1 < nonClimbableBlocks.size() ? nonClimbableBlocks.get(i + 1) : null;
				addEntry(new BlockEntry(leftBlock, rightBlock));
			}
		}

		@Override
		public int getRowWidth() {
			return width - 60;
		}

		// Entry representing a row with two blocks
		public class BlockEntry extends ObjectSelectionList.Entry<BlockEntry> {
			private final String leftBlock;
			private final String rightBlock;
			private final Button leftRemoveButton;
			private final Button rightRemoveButton;

			public BlockEntry(String leftBlock, String rightBlock) {
				this.leftBlock = leftBlock;
				this.rightBlock = rightBlock;

				// Create remove buttons for each block
				this.leftRemoveButton = Button.builder(
					Component.literal("×"),
					btn -> removeBlockId(leftBlock))
				.bounds(0, 0, 16, 16)
				.tooltip(Tooltip.create(Component.literal("Remove " + leftBlock)))
				.build();

				this.rightRemoveButton = rightBlock != null ? Button.builder(
					Component.literal("×"),
					btn -> removeBlockId(rightBlock))
				.bounds(0, 0, 16, 16)
				.tooltip(Tooltip.create(Component.literal("Remove " + rightBlock)))
				.build() : null;
			}

			@Override
			public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean isHovering, float delta) {
				// Get entry position (not content position - match VR Throwing Extensions pattern)
				int x = this.getX();
				int y = this.getY();
				int width = this.getWidth();
				int height = this.getHeight();

				int columnWidth = width / 2;

				// Position and render left remove button
				leftRemoveButton.setPosition(x + columnWidth - 20, y + 6);
				leftRemoveButton.render(context, mouseX, mouseY, delta);

				// Left column - at y (top), bright red, NO truncation
				context.drawString(font, leftBlock, x + 5, y, 0xFFFF0000);

				// Right column (if exists)
				if (rightBlock != null) {
					rightRemoveButton.setPosition(x + width - 20, y + 6);
					rightRemoveButton.render(context, mouseX, mouseY, delta);
					context.drawString(font, rightBlock, x + columnWidth + 5, y, 0xFFFF0000);
				}
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
				// Forward click events to buttons (1.21.10 API)
				if (leftRemoveButton.mouseClicked(event, isDoubleClick)) {
					return true;
				}
				if (rightRemoveButton != null && rightRemoveButton.mouseClicked(event, isDoubleClick)) {
					return true;
				}
				return false;
			}

			@Override
			public Component getNarration() {
				return Component.literal(leftBlock + (rightBlock != null ? " and " + rightBlock : ""));
			}
		}
	}
}
