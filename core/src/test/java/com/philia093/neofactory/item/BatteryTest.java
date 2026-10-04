package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the fifteen cells of the industry and what a charge does to a stack of one.
 * <p>
 * <b>The table is the whole of a battery</b>, so most of this case reads it back: that every chemistry
 * comes in every tier, that the tier is what the amount of a cell is multiplied by, and that every row of
 * the table really has an item behind it with the name, the picture and the life the row asks for, see
 * {@link Batteries}.
 * <p>
 * <b>The charge of a battery is the wear of its stack</b>, and that is what the rest of the case is about:
 * a fresh cell holds everything it was built for, what is taken out of it is what its stack has taken, and
 * a cell that is spent is at the end of its life without being destroyed - the one place the game takes a
 * used up piece off a player is the swing that broke it, see {@code MiningController}, and a battery is
 * never swung at a block.
 */
class BatteryTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theTableHoldsACellOfEveryChemistryAndEveryTier() {
        assertEquals(BatteryChemistry.values().length * Batteries.TIERS.size(), Batteries.all().size(),
                "every chemistry comes in every tier");
        assertEquals(15, Batteries.all().size(), "five chemistries, three tiers and no cell twice");

        for (BatteryChemistry chemistry : BatteryChemistry.values()) {
            for (Voltage tier : Batteries.TIERS) {
                Batteries.Cell cell = Batteries.of(chemistry, tier);
                assertSame(chemistry, cell.chemistry());
                assertSame(tier, cell.voltage());
                assertSame(cell, Batteries.of(chemistry, tier),
                        "a lookup hands out the cell of the table and not a new one");
            }
            assertEquals(Batteries.TIERS.size(), Batteries.of(chemistry).size(),
                    "a chemistry is listed once per tier");
        }

        for (Voltage tier : Batteries.TIERS) {
            assertEquals(BatteryChemistry.values().length, Batteries.of(tier).size(),
                    "and a tier holds every chemistry");
        }

        assertThrows(IllegalArgumentException.class,
                () -> Batteries.of(BatteryChemistry.ACID, Voltage.ULTRA_LOW),
                "a tier no battery is built for is not a cell of the table");
    }

    @Test
    void everyTierOfAChemistryHoldsFourTimesTheOneBelowIt() {
        // The amounts of the table, as a player reads them in a tooltip: one ampere of a low voltage cell
        // lasts the nominal time of its chemistry, and a bigger cell of the same chemistry is a cell that
        // holds more and not one that gives more, so a tier of the line is four times the one below it.
        int[] low = {18_000, 32_000, 50_000, 75_000, 100_000};
        BatteryChemistry[] chemistries = BatteryChemistry.values();
        assertEquals(low.length, chemistries.length, "there is an amount for every chemistry");

        for (int index = 0; index < chemistries.length; index++) {
            BatteryChemistry chemistry = chemistries[index];
            assertEquals(low[index], Batteries.capacityOf(chemistry, Voltage.LOW),
                    "the cell of the low voltage of " + chemistry);
            assertEquals(low[index] * 4, Batteries.capacityOf(chemistry, Voltage.MEDIUM),
                    "the cell of the middle voltage holds four times as much");
            assertEquals(low[index] * 16, Batteries.capacityOf(chemistry, Voltage.HIGH),
                    "and one of the high voltage four times again");
        }

        // What the chemistries are good for, in the order of the table: the better the chemistry, the
        // longer an ampere of it lasts, and the three that hold the longest are the three that take a
        // charge again, see BatteryChemistry.
        assertTrue(BatteryChemistry.ACID.nominalSeconds() < BatteryChemistry.MERCURY.nominalSeconds());
        assertTrue(BatteryChemistry.MERCURY.nominalSeconds() < BatteryChemistry.SODIUM.nominalSeconds());
        assertTrue(BatteryChemistry.SODIUM.nominalSeconds() < BatteryChemistry.CADMIUM.nominalSeconds());
        assertTrue(BatteryChemistry.CADMIUM.nominalSeconds() < BatteryChemistry.LITHIUM.nominalSeconds());
    }

    @Test
    void everyCellOfTheTableHasAnItemOfItsOwn() {
        int expected = Items.BATTERY_FIRST_ID;
        for (Batteries.Cell cell : Batteries.all()) {
            String name = Batteries.itemNameOf(cell);
            Item item = ItemRegistry.byName(name);
            assertNotNull(item, name + " is a cell of the table and has no item");
            assertEquals(expected++, item.id(), "the run of the cells is tight, see Items#BATTERY_FIRST_ID");

            assertTrue(item.isBattery(), name + " is a cell of energy");
            assertSame(cell, item.battery(), "and it is the cell of its row of the table");
            assertFalse(item.isStackable(), "two cells are two charges, so one is one piece");
            assertEquals(cell.capacity(), item.maxDamage(),
                    "the life of a cell is what it holds, see Battery#capacity");
            assertEquals(Batteries.pictureOf(cell), item.texture());
            assertEquals(Batteries.displayNameOf(cell), item.displayName());
        }
        assertEquals(Items.NEXT_FREE_ID, expected,
                "the cells stand behind the machines of the line and in front of the materials");

        assertFalse(Items.STONE.isBattery(), "a block of stone holds no charge");
        assertNull(Batteries.of(Items.STONE), "and it is no cell either");
        assertNull(Batteries.of((Item) null), "neither is a slot that holds nothing");
    }

    @Test
    void theChargeOfACellIsTheWearOfItsStack() {
        Batteries.Cell cell = Batteries.of(BatteryChemistry.LITHIUM, Voltage.LOW);
        ItemStack stack = ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(cell)), 1);

        assertEquals(cell.capacity(), cell.chargeOf(stack), "a fresh cell holds everything it was built for");
        assertEquals(0, stack.damage(), "which is a stack that has taken no damage");

        assertEquals(4_000, cell.extract(stack, 4_000), "four thousand units come out of it");
        assertEquals(96_000, cell.chargeOf(stack));
        assertEquals(4_000, stack.damage(), "which is what the stack has taken");
        assertEquals(0.04f, stack.wear(), 1.0e-6f, "and the bar under its icon reads it");

        assertEquals(4_000, cell.insert(stack, 4_000), "and what came out goes back in");
        assertEquals(cell.capacity(), cell.chargeOf(stack));
        assertEquals(0, stack.damage(), "so the cell is whole again");
    }

    @Test
    void aCellThatIsSpentStaysInTheSlotOfItsOwner() {
        Batteries.Cell cell = Batteries.of(BatteryChemistry.CADMIUM, Voltage.MEDIUM);
        Item item = ItemRegistry.byName(Batteries.itemNameOf(cell));
        ItemStack stack = ItemStack.of(item, 1);

        assertEquals(cell.capacity(), cell.extract(stack, cell.capacity()), "a cell gives all it holds");

        assertTrue(cell.isDrained(stack));
        assertEquals(0, cell.chargeOf(stack));
        assertTrue(stack.isBroken(), "a spent cell is at the end of the life of its stack");
        assertSame(item, stack.item(), "but a spent cell is still the cell it was");
        assertEquals(1, stack.count(), "and it is not taken out of the slot");
        assertSame(cell, Batteries.of(stack.item()), "the table still knows what it is");
        assertEquals(0, cell.extract(stack, 100), "and nothing more comes out of it");
    }

    @Test
    void onlyACellThatIsMadeToTakeAChargeTakesOne() {
        assertTrue(BatteryChemistry.SODIUM.isRechargeable());
        assertTrue(BatteryChemistry.CADMIUM.isRechargeable());
        assertTrue(BatteryChemistry.LITHIUM.isRechargeable());
        assertFalse(BatteryChemistry.ACID.isRechargeable(), "an acid cell is used once");
        assertFalse(BatteryChemistry.MERCURY.isRechargeable(), "and so is a mercury one");

        for (BatteryChemistry chemistry : List.of(BatteryChemistry.ACID, BatteryChemistry.MERCURY)) {
            Batteries.Cell cell = Batteries.of(chemistry, Voltage.LOW);
            ItemStack stack = ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(cell)), 1);

            assertFalse(cell.isRechargeable());
            assertEquals(1_000, cell.extract(stack, 1_000), "a cell gives what is asked of it");
            assertEquals(0, cell.insert(stack, 1_000), "but a spent one of these is never filled again");
            assertEquals(cell.capacity() - 1_000, cell.chargeOf(stack), "so what left it is lost");
        }
    }

    @Test
    void nothingIsLostOverTheBrimOfACell() {
        Batteries.Cell cell = Batteries.of(BatteryChemistry.SODIUM, Voltage.LOW);
        ItemStack stack = ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(cell)), 1);

        assertEquals(4_000, cell.extract(stack, 4_000), "a cell gives four thousand units up");
        assertEquals(4_000, cell.insert(stack, 100_000), "and takes four thousand of a hundred thousand back");
        assertEquals(cell.capacity(), cell.chargeOf(stack), "so it is full and not past it");
        assertEquals(0, cell.insert(stack, 500), "a cell that is full takes nothing");
        assertEquals(0, cell.extract(stack, -1), "and an amount that is not asked for moves nothing");
        assertEquals(cell.capacity(), cell.chargeOf(stack));
    }

    @Test
    void aChargeIsNeverMoreThanACellHoldsAndNeverLessThanNothing() {
        Batteries.Cell cell = Batteries.of(BatteryChemistry.LITHIUM, Voltage.HIGH);
        ItemStack stack = ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(cell)), 1);

        assertEquals(cell.capacity(), cell.extract(stack, cell.capacity() * 2),
                "a cell gives what it holds even when more is asked of it");
        assertEquals(0, cell.chargeOf(stack), "and never less than nothing");
        assertEquals(0, cell.extract(stack, 10_000));
        assertEquals(cell.capacity(), cell.insert(stack, Integer.MAX_VALUE),
                "nor does it hold more than it was built for");
    }

    @Test
    void everyChemistryIsAColourOfItsOwn() {
        List<Integer> colours = new ArrayList<>(BatteryChemistry.values().length);
        for (BatteryChemistry chemistry : BatteryChemistry.values()) {
            Color colour = chemistry.colour();
            assertTrue(colour.r + colour.g + colour.b > 0.1f,
                    chemistry + " is a colour a window can be poured in");

            int packed = Color.rgba8888(colour);
            assertFalse(colours.contains(packed), chemistry + " wears the colour of another chemistry");
            colours.add(packed);
        }
        assertEquals(BatteryChemistry.values().length, colours.size());
    }

    @Test
    void theSizeIsReadAsAWordAndTheTierStandsInTheName() {
        Batteries.Cell small = Batteries.of(BatteryChemistry.ACID, Voltage.LOW);
        Batteries.Cell middle = Batteries.of(BatteryChemistry.ACID, Voltage.MEDIUM);
        Batteries.Cell large = Batteries.of(BatteryChemistry.ACID, Voltage.HIGH);

        assertEquals("battery_acid_lv", Batteries.itemNameOf(small));
        assertEquals("battery_acid_mv", Batteries.itemNameOf(middle));
        assertEquals("battery_acid_hv", Batteries.itemNameOf(large));

        assertEquals("Small Acid Battery", Batteries.displayNameOf(small));
        assertEquals("Medium Acid Battery", Batteries.displayNameOf(middle));
        assertEquals("Large Acid Battery", Batteries.displayNameOf(large));

        assertEquals("items/battery_acid_small", Batteries.pictureOf(small));
        assertEquals("items/battery_acid_large", Batteries.pictureOf(middle),
                "the two tiers above the low voltage share the large pack");
        assertEquals("items/battery_acid_large", Batteries.pictureOf(large));

        assertTrue(Batteries.isSmall(Voltage.LOW), "a cell of the low voltage is the small one");
        assertFalse(Batteries.isSmall(Voltage.MEDIUM));
        assertFalse(Batteries.isSmall(Voltage.HIGH));
    }
}
