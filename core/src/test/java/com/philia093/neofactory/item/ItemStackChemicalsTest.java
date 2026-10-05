package com.philia093.neofactory.item;

import com.philia093.neofactory.chemistry.Amounts;
import com.philia093.neofactory.chemistry.Blend;
import com.philia093.neofactory.chemistry.Chemical;
import com.philia093.neofactory.chemistry.Fraction;
import com.philia093.neofactory.material.MaterialForm;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.save.SaveTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the pile of substances an item and a stack of it are.
 * <p>
 * The numbers under test are the ones the industry is measured in: an ingot is a hundred millibuckets of
 * its metal, a dust a hundred, a nugget ten, and an ingot of bronze is seventy five of copper and twenty
 * five of tin - an alloy read as its metals side by side and never as one compound. A stack that names a
 * pile of its own is checked as the other half of the rule, including what a save game has to carry.
 */
class ItemStackChemicalsTest {

    private static final Chemical IRON = Chemical.parse("[Fe]");
    private static final Chemical COPPER = Chemical.parse("[Cu]");
    private static final Chemical TIN = Chemical.parse("[Sn]");

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void anIngotIsAHundredMillibucketsOfItsMetal() {
        Blend iron = Materials.IRON.ingot().chemicals();

        assertEquals(Amounts.INGOT, iron.total());
        assertEquals(Fraction.of(100), iron.amountOf(IRON));
        assertTrue(iron.isPure());
    }

    @Test
    void anIngotOfAnAlloyIsItsMetalsSideBySide() {
        Blend bronze = Materials.BRONZE.ingot().chemicals();

        assertFalse(bronze.isPure(), "an alloy is a pile of metals and not one compound");
        assertEquals(Fraction.of(75), bronze.amountOf(COPPER));
        assertEquals(Fraction.of(25), bronze.amountOf(TIN));
        assertEquals(Amounts.INGOT, bronze.total());
    }

    @Test
    void theShapesOfAMaterialCarryTheAmountsOfTheIndustry() {
        assertEquals(Amounts.DUST, Materials.IRON.dust().chemicals().total());
        assertEquals(Amounts.NUGGET, Materials.IRON.nugget().chemicals().total());
        assertEquals(Amounts.FLUID_CELL, MaterialForm.FLUID_CELL.millibuckets());
        assertEquals(Amounts.INGOT, MaterialForm.INGOT.millibuckets());
    }

    @Test
    void aNuggetOfAnAlloySplitsExactly() {
        Blend nugget = Materials.BRONZE.nugget().chemicals();

        assertEquals(Amounts.NUGGET, nugget.total());
        assertEquals(Fraction.of(15, 2), nugget.amountOf(COPPER), "seven and a half, and not a rounded seven");
        assertEquals(Fraction.of(5, 2), nugget.amountOf(TIN));
    }

    @Test
    void aShapeWithNoMeasuredAmountHoldsNoChemistry() {
        assertTrue(Materials.IRON.plate().chemicals().isEmpty(), "a plate is not a quantity yet");
        assertTrue(Items.STICK.chemicals().isEmpty(), "a stick is not a chemical at all");
        assertTrue(Items.MALLET.chemicals().isEmpty());
    }

    @Test
    void aStackAnswersWithThePileOfItsItem() {
        ItemStack ingots = ItemStack.of(Materials.IRON.ingot(), 4);

        assertEquals(Fraction.of(100), ingots.chemicals().amountOf(IRON));
    }

    @Test
    void aPlainItemCarriesThePileItsStackNames() {
        Blend pile = Blend.of(COPPER, 75).plus(Blend.of(TIN, 25));
        ItemStack dust = ItemStack.of(Items.STICK, 1, pile);

        assertEquals(pile, dust.chemicals());
        assertTrue(Items.STICK.chemicals().isEmpty(), "the plain item itself holds no pile");
        assertTrue(ItemStack.of(Items.STICK, 1).chemicals().isEmpty(),
                "and neither does a stack that names none");
    }

    @Test
    void twoStacksOfOnePileMergeAndTwoDifferentOnesDoNot() {
        ItemStack copper = ItemStack.of(Items.STICK, 1, Blend.of(COPPER, 100));
        ItemStack tin = ItemStack.of(Items.STICK, 1, Blend.of(TIN, 100));
        ItemStack moreCopper = ItemStack.of(Items.STICK, 1, Blend.of(COPPER, 100));

        assertFalse(copper.isStackableWith(tin), "two piles are two stacks however plain the item is");
        assertTrue(copper.isStackableWith(moreCopper));
        assertEquals(copper, moreCopper);
    }

    @Test
    void aCopyAndASplitCarryThePile() {
        Blend pile = Blend.of(COPPER, 75);
        ItemStack dust = ItemStack.of(Items.STICK, 4, pile);

        assertEquals(pile, dust.copy().chemicals());
        assertEquals(pile, dust.split(2).chemicals());
        assertEquals(2, dust.count(), "the stack that was split lost what went into the other");
    }

    @Test
    void aPileSurvivesASaveGame() {
        Blend pile = Blend.of(COPPER, Fraction.of(75)).plus(Blend.of(TIN, Fraction.of(25)));
        PlayerInventory inventory = new PlayerInventory();
        inventory.set(3, ItemStack.of(Items.STICK, 1, pile));

        NbtList list = SaveTags.writeInventory(inventory);
        PlayerInventory back = new PlayerInventory();
        SaveTags.readInventory(back, list);

        assertEquals(Items.STICK, back.get(3).item());
        assertEquals(pile, back.get(3).chemicals());
    }
}
