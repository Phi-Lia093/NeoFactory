package com.philia093.neofactory.gui.container;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.material.Material;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the lines a tooltip is drawn from.
 * <p>
 * The box belongs to {@link ItemTooltip} and is drawn by two screens, so what is worth checking is
 * what it says: the name of the item first and, for a material of the game, the chemical formula
 * under it - the format every material of the industry is named in. An item that is not a material
 * keeps a single line, and a slot that holds nothing draws no box at all.
 */
class ItemTooltipTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aShapeOfAMaterialNamesItsFormulaUnderTheName() {
        assertEquals(List.of("Iron Plate", "Fe"), ItemTooltip.linesOf(Materials.IRON.plate()));
        assertEquals(List.of("Copper Gear", "Cu"), ItemTooltip.linesOf(Materials.COPPER.gear()));
        assertEquals(List.of("Tungsten Ingot", "W"), ItemTooltip.linesOf(Materials.TUNGSTEN.ingot()));
    }

    @Test
    void everyItemOfEveryMaterialBringsItsNameAndItsFormulaWhenItHasOne() {
        for (Material material : Materials.all()) {
            for (Item item : material.items()) {
                List<String> lines = ItemTooltip.linesOf(item);
                assertEquals(item.displayName(), lines.get(0), item.name());
                if (material.hasChemicalFormula()) {
                    assertEquals(2, lines.size(), item.name() + " does not name its formula");
                    assertEquals(material.chemicalFormula(), lines.get(1), item.name());
                } else {
                    // A material whose formula is not known yet draws its name and nothing else, so a line
                    // that is filled in later is one line here and one value in Materials.
                    assertEquals(1, lines.size(), item.name() + " names a formula it does not have");
                }
            }
        }
    }

    @Test
    void anItemWithoutAFormulaDrawsASingleLine() {
        assertEquals(List.of("Stone"), ItemTooltip.linesOf(Items.STONE));
        assertEquals(List.of("Diamond"), ItemTooltip.linesOf(Items.DIAMOND));
        assertEquals(List.of("Water Cell"), ItemTooltip.linesOf(Items.WATER_CELL));
    }

    @Test
    void aStackHandsTheLinesOfItsItemOut() {
        assertEquals(List.of("Iron Plate", "Fe"),
                ItemTooltip.linesOf(ItemStack.of(Materials.IRON.plate(), 4)));
    }

    @Test
    void aToolNamesWhatIsLeftOfItsLife() {
        assertEquals(List.of("Iron Pickaxe", ItemTooltip.LIFE_LABEL + "250 / 250"),
                ItemTooltip.linesOf(ItemStack.of(Items.IRON_PICKAXE, 1)));

        ItemStack used = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        used.setDamage(61);

        assertEquals(List.of("Diamond Pickaxe", ItemTooltip.LIFE_LABEL + "1500 / 1561"),
                ItemTooltip.linesOf(used), "what is left of a piece is named under its name");
    }

    @Test
    void theItemItselfCarriesNoLifeOfItsOwn() {
        // The damage belongs to the stack and not to the definition, so the type of a tool is one line
        // and a block that never wears out keeps its single line as well, see Damageable.
        assertEquals(List.of("Iron Pickaxe"), ItemTooltip.linesOf(Items.IRON_PICKAXE));
        assertEquals(List.of("Stone"), ItemTooltip.linesOf(ItemStack.of(Items.STONE, 4)));
    }

    @Test
    void nothingUnderTheMouseDrawsNothing() {
        assertTrue(ItemTooltip.linesOf((Item) null).isEmpty());
        assertTrue(ItemTooltip.linesOf((ItemStack) null).isEmpty());
        assertTrue(ItemTooltip.linesOf(ItemStack.EMPTY).isEmpty());
        assertTrue(ItemTooltip.linesOf(Items.AIR).isEmpty());
    }
}
