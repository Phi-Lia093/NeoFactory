package com.philia093.neofactory.gui;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.recipe.CraftingField;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.GuiPreview;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints the screen of the table of the workshop the way the game draws it and writes the result to a
 * picture.
 * <p>
 * The field of nine cells is filled with the pattern of the game - the planks and the sticks a pickaxe
 * is laid out from, the recipe of iron - so the picture shows what a player really sees: the pattern with
 * the product beside it, the arrow between the two and the rows of the player below them, see
 * {@link GuiPreview}. The result and the loaded pattern of the field are checked as well, so the picture
 * cannot show a table that works in no game.
 * <p>
 * The result lies in {@code core/build/reports/crafting-preview.png}.
 */
class CraftingPreviewTest {

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "crafting-preview.png");

    /** The pattern of a pickaxe, the recipe of the picture. */
    private static final String PICKAXE = "{ \"pattern\": [\"III\", \" S \", \" S \"],"
            + " \"key\": { \"I\": \"iron_ingot\", \"S\": \"stick\" },"
            + " \"result\": { \"item\": \"iron_pickaxe\" } }";

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void registerRecipe() {
        RecipeRegistry.clear();
        RecipeRegistry.register(
                RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "iron_pickaxe", PICKAXE));
    }

    @Test
    void theTableIsPaintedWithItsPatternAndItsResult() throws IOException {
        Inventory cells = new Inventory(CraftingField.CELLS);
        layOutThePickaxe(cells);
        CraftingField field = new CraftingField(cells);
        PlayerInventory player = new PlayerInventory();
        ContainerLayout layout = CraftingLayout.of(field.resultSlot(), cells, player);

        assertEquals(CraftingField.CELLS + 1 + PlayerInventory.SLOT_COUNT, layout.size());
        assertTrue(field.makesSomething(), "the picture shows a table that works");

        BufferedImage sheet = GuiPreview.read(GuiPreview.SHEET);
        BufferedImage picture = GuiPreview.newPicture(
                GuiPreview.MARGIN + layout.panelWidth() + GuiPreview.MARGIN,
                GuiPreview.MARGIN + layout.panelHeight() + GuiPreview.MARGIN);
        GuiPreview.drawPanel(sheet, picture, GuiPreview.MARGIN, GuiPreview.MARGIN,
                layout.panelWidth(), layout.panelHeight());
        GuiPreview.drawArrow(sheet, picture, GuiPreview.MARGIN + CraftingLayout.ARROW_X,
                GuiPreview.MARGIN + CraftingLayout.ARROW_Y);
        for (Slot slot : layout.slots()) {
            GuiPreview.drawSlot(sheet, picture, GuiPreview.MARGIN + slot.x(),
                    GuiPreview.MARGIN + slot.y());
            GuiPreview.drawItem(picture, GuiPreview.MARGIN + slot.x(), GuiPreview.MARGIN + slot.y(),
                    slot.stack());
        }
        GuiPreview.drawText(picture, GuiPreview.read(GuiPreview.FONT),
                Items.CRAFTING_TABLE.displayName(), GuiPreview.MARGIN + ContainerLayout.PADDING,
                GuiPreview.MARGIN + ContainerGui.TITLE_TOP);

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(picture, "png", PREVIEW.toFile());
        System.out.println("crafting preview written to " + PREVIEW.toAbsolutePath() + ": panel "
                + layout.panelWidth() + " x " + layout.panelHeight() + ", " + layout.size()
                + " slots");

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
    }

    /** Lays the pattern of a pickaxe into the cells and fills the inventory of the player. */
    private static void layOutThePickaxe(Inventory cells) {
        for (int column = 0; column < CraftingField.COLUMNS; column++) {
            cells.set(column, ItemStack.of(Materials.IRON.ingot(), 3));
        }
        cells.set(4, ItemStack.of(Items.STICK, 2));
        cells.set(7, ItemStack.of(Items.STICK, 2));
    }
}
