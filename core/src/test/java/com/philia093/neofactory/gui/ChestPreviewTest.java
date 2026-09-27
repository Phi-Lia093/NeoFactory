package com.philia093.neofactory.gui;

import com.philia093.neofactory.blockentity.ChestBlockEntity;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.GuiPreview;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints the screen of a chest the way the game draws it and writes the result to a picture.
 * <p>
 * The test builds the very layout the screen uses, {@link ChestLayout#of}, and paints it with the very
 * pictures the game uses: the panel of {@code gui/inventory_icons.png} stretched in nine cells, the
 * bevel of every slot, the small cube a block item is folded into, the bar of a worn tool and the name
 * of the container in the band the layout keeps free at the top, see {@link GuiPreview}.
 * <p>
 * The result lies in {@code core/build/reports/chest-preview.png} and is meant to be looked at: it is how
 * the layout of a container screen is reviewed without starting the game. The test also pins down that
 * the slots of the player land on the rows the layout names, so the drawn panel and the click of the
 * player stay the same place.
 */
class ChestPreviewTest {

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "chest-preview.png");

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theScreenOfAChestIsPaintedAsTheGameDrawsIt() throws IOException {
        Inventory contents = new Inventory(ChestBlockEntity.SLOTS);
        PlayerInventory player = new PlayerInventory();
        ContainerLayout layout = ChestLayout.of(contents, player);

        assertEquals(ChestBlockEntity.SLOTS + PlayerInventory.SLOT_COUNT, layout.size());
        assertEquals(PanelTextures.PANEL_WIDTH, layout.panelWidth());
        assertEquals(ChestLayout.HOTBAR_Y + Slot.size() + ContainerLayout.PADDING,
                layout.panelHeight(), "the panel ends below the hotbar row");
        assertEquals(ChestLayout.STORAGE_Y, layout.slots().get(ChestBlockEntity.SLOTS).y(),
                "the storage of the player lands on the row the layout names");
        assertEquals(ChestLayout.HOTBAR_Y,
                layout.slots().get(layout.size() - PlayerInventory.HOTBAR_SLOTS).y(),
                "the hotbar of the player lands on the row the layout names");

        fillChest(contents);
        fillPlayer(player);

        BufferedImage sheet = GuiPreview.read(GuiPreview.SHEET);
        BufferedImage picture = GuiPreview.newPicture(
                GuiPreview.MARGIN + layout.panelWidth() + GuiPreview.MARGIN,
                GuiPreview.MARGIN + layout.panelHeight() + GuiPreview.MARGIN);

        GuiPreview.drawPanel(sheet, picture, GuiPreview.MARGIN, GuiPreview.MARGIN,
                layout.panelWidth(), layout.panelHeight());
        for (Slot slot : layout.slots()) {
            GuiPreview.drawSlot(sheet, picture, GuiPreview.MARGIN + slot.x(),
                    GuiPreview.MARGIN + slot.y());
            GuiPreview.drawItem(picture, GuiPreview.MARGIN + slot.x(), GuiPreview.MARGIN + slot.y(),
                    slot.stack());
            GuiPreview.drawDurabilityBar(picture, GuiPreview.MARGIN + slot.x(),
                    GuiPreview.MARGIN + slot.y(), slot.stack());
        }
        GuiPreview.drawText(picture, GuiPreview.read(GuiPreview.FONT), Items.CHEST.displayName(),
                GuiPreview.MARGIN + ContainerLayout.PADDING, GuiPreview.MARGIN + ContainerGui.TITLE_TOP);

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(picture, "png", PREVIEW.toFile());
        System.out.println("chest preview written to " + PREVIEW.toAbsolutePath() + ": panel "
                + layout.panelWidth() + " x " + layout.panelHeight() + ", " + layout.size()
                + " slots");

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
    }

    /** Puts a small kit into the chest, so the picture shows what a player keeps in it. */
    private static void fillChest(Inventory contents) {
        contents.set(0, ItemStack.of(Items.STONE, 64));
        contents.set(1, ItemStack.of(Items.COBBLESTONE, 32));
        contents.set(2, ItemStack.of(Items.COAL_ORE, 12));
        contents.set(3, ItemStack.of(Materials.IRON.ingot(), 9));
        contents.set(4, ItemStack.of(Items.PLANKS_OAK, 48));
        contents.set(5, ItemStack.of(Items.GLASS, 16));
        contents.set(9, ItemStack.of(Items.FURNACE, 2));
        contents.set(10, ItemStack.of(Items.CRAFTING_TABLE, 1));
        contents.set(13, ItemStack.of(Items.TORCH, 24));
        contents.set(18, ItemStack.of(Materials.COPPER.ingot(), 5));
    }

    /** Puts a small kit into the inventory of the player, a worn tool included. */
    private static void fillPlayer(PlayerInventory player) {
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        pickaxe.setDamage(Items.DIAMOND_TOOL_DURABILITY / 3);
        player.set(0, pickaxe);
        player.set(1, ItemStack.of(Items.SAND, 21));
        player.set(2, ItemStack.of(Items.STICK, 32));
        player.set(9, ItemStack.of(Items.DIRT, 64));
        player.set(11, ItemStack.of(Items.GRAVEL, 64));
        player.set(13, ItemStack.of(Materials.BRONZE.ingot(), 11));
        player.set(20, ItemStack.of(Items.CLAY_BALL, 13));
        player.set(29, ItemStack.of(Items.LADDER, 6));
    }
}
