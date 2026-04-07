package win.demistorm.stormiespiders.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

	// UI Bits
	private EditBox blockIdInput;
	private BlockListWidget blockList;

	// Layout
	private int topY;
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
		topY = 55;
		listTopY = topY + 45;
		int bottomMargin = 47;

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
				"  *trapdoor - Anything ending with 'trapdoor'\n" +
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
			.bounds(width / 2 - 100, height - 27, 200, 20)
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
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		// Render all widgets (buttons, text input)
		super.extractRenderState(context, mouseX, mouseY, delta);

        if (blockList != null) {
			blockList.extractRenderState(context, mouseX, mouseY, delta);
		}

		context.centeredText(font, title, width / 2, 20, 0xFFFFFF);
		context.text(font, "Add Block ID or Wildcard:", 20, topY - 12, 0xFFFFFF);
		context.text(font, "Non-Climbable Blocks:", 20, listTopY - 12, 0xFFFFFF);
		context.text(font, "(" + nonClimbableBlocks.size() + " entries)", 160, listTopY - 12, 0xAAAAAA);

        // Instructions at bottom
        String instructions = "Wildcards: *trapdoor matches all trapdoors, *door* matches anything with 'door'";
        context.centeredText(font, instructions, width / 2, height - 75, 0x808080);
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

	// List widget for displaying blocks in two columns
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
			public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean isHoverings, float delta) {
				// Get entry position (not content position - match VR Throwing Extensions pattern)
				int x = this.getX();
				int y = this.getY();
				int width = this.getWidth();
				int height = this.getHeight();

				int columnWidth = width / 2;

				// Position and render left remove button
				leftRemoveButton.setPosition(x + columnWidth - 20, y + 6);
				leftRemoveButton.extractRenderState(context, mouseX, mouseY, delta);

				// Left column
				context.text(font, Component.literal(leftBlock).withStyle(style -> style.withUnderlined(true)), x + 5, y + 9, 0xFFFFFFFF);

				// Right column
				if (rightBlock != null) {
					rightRemoveButton.setPosition(x + width - 20, y + 6);
					rightRemoveButton.extractRenderState(context, mouseX, mouseY, delta);
					context.text(font, Component.literal(rightBlock).withStyle(style -> style.withUnderlined(true)), x + columnWidth + 5, y + 9, 0xFFFFFFFF);
				}
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
				// Forward click events to buttons
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
