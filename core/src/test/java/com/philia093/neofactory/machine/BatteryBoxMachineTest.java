package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.Battery;
import com.philia093.neofactory.item.BatteryChemistry;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a box of cells is: a buffer of energy that is the cells standing in it.
 * <p>
 * The box is the one machine of the game that holds nothing of its own. What it may take in and give out, what
 * it holds and what it can hold are all read from the cells a player put into it, so every question of this
 * test is asked of the machine and answered by the cells in it, see {@link BatteryBoxMachine} and
 * {@link BatteryBank}. The two plugs of the box and the panel it is drawn in are checked here as well, and the
 * last two tests put a box into a world: one where a neighbouring machine feeds it and one where it feeds the
 * machine next to it - the way the power really travels, which is the machine that wants it reaching for it and
 * never the cable, see {@code MachineBlockEntity#updateEnergy}.
 */
class BatteryBoxMachineTest {

    private static final int Y = 64;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aBoxWithNoCellInItHoldsNothingAndAsksForNothing() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 4);

        assertEquals(0, box.bank().amount());
        assertEquals(0, box.bank().capacity(), "a box holds what its cells hold, and there is no cell in it");
        assertEquals(0, box.bank().maxReceive());
        assertEquals(0, box.bank().maxExtract());
        assertEquals(0, box.requestEu(), "so it asks a line for nothing at all");
        assertFalse(box.bank().canReceive());
        assertFalse(box.bank().canExtract());
        assertTrue(box.bank().isEmpty());

        // The two plugs of a box are what a player built it for and not what lies in it: a player builds the
        // box and the line around it and puts the cells in afterwards.
        assertEquals(BlockFace.SOUTH, box.faces().energyIn());
        assertEquals(BlockFace.EAST, box.faces().energyOut());
        assertTrue(box.faces().takesPower() && box.faces().givesPower());
        assertFalse(box.faces().hasFront(), "and a box is the same from every side of it");
    }

    @Test
    void everyCellOfABoxIsAMouthOfItsOwn() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 4);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW, 0));

        assertEquals(1, box.bank().cells());
        assertEquals(2, BatteryBank.AMPS_IN, "one cell takes two amperes of its tier in a tick");
        assertEquals(2 * Voltage.LOW.euPerTick(), box.bank().maxReceive());
        assertEquals(1 * Voltage.LOW.euPerTick(), box.bank().maxExtract());
        assertEquals(box.bank().maxReceive(), box.requestEu(), "and that is what the box asks a line for");

        // A stack that is no cell adds nothing: the box holds cells and nothing else.
        box.inventory().set(1, ItemStack.of(Items.STONE, 1));
        assertEquals(1, box.bank().cells(), "a stone in a box is no mouth of it");
        assertEquals(2 * Voltage.LOW.euPerTick(), box.bank().maxReceive());

        box.inventory().set(2, cell(BatteryChemistry.LITHIUM, Voltage.LOW, 0));
        box.inventory().set(3, cell(BatteryChemistry.MERCURY, Voltage.LOW, 0));
        assertEquals(3, box.bank().cells(), "and the chemistry of a cell is none of the box's business");
        assertEquals(6 * Voltage.LOW.euPerTick(), box.bank().maxReceive());
        assertEquals(3 * Voltage.LOW.euPerTick(), box.bank().maxExtract());
    }

    @Test
    void theCellsOfABoxAreItsBuffer() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.MEDIUM, 4);
        ItemStack spent = cell(BatteryChemistry.SODIUM, Voltage.MEDIUM, 0);
        box.inventory().set(0, spent);
        Battery cell = Batteries.of(spent);

        assertEquals(cell.capacity(), box.bank().capacity(), "the box holds what its cells can hold");

        int taken = box.bank().receive(box.bank().maxReceive() * 10, false);
        assertEquals(box.bank().maxReceive(), taken, "a box takes what its cells take in a tick and no more");
        assertEquals(taken, box.bank().amount(), "and what went in is the charge of the cell");
        assertEquals(taken, cell.chargeOf(spent));
        assertEquals(taken, box.bank().receive(box.bank().capacity(), true),
                "a box asks its cells for an ampere's worth a tick, however much room they have");

        int given = box.bank().extract(box.bank().maxExtract() * 10, false);
        assertEquals(box.bank().maxExtract(), given, "and it gives what its cells give in a tick");
        assertEquals(taken - given, box.bank().amount());
        assertEquals(taken - given, box.bank().extract(box.bank().capacity(), true),
                "while what its cells hold is the limit of a tick");

        // A cell that is taken out of a box takes its charge with it, and one that is put in brings its own.
        ItemStack charged = cell(BatteryChemistry.SODIUM, Voltage.MEDIUM, cell.capacity());
        box.inventory().set(1, charged);
        assertEquals(cell.chargeOf(spent) + cell.capacity(), box.bank().amount());
        assertEquals(2 * cell.capacity(), box.bank().capacity());

        box.inventory().set(1, ItemStack.EMPTY);
        assertEquals(cell.chargeOf(spent), box.bank().amount(), "and the charge left the box with it");
        assertEquals(cell.capacity(), box.bank().capacity());
    }

    @Test
    void aBoxWhoseCellsAreFullTakesNothingAtAll() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 1);
        ItemStack nearlyFull = cell(BatteryChemistry.LITHIUM, Voltage.LOW, 0);
        // One unit of room left in the cell of the box, which is what a line finds when it arrives too late.
        nearlyFull.setDamage(1);
        box.inventory().set(0, nearlyFull);

        assertEquals(1, box.bank().receive(Voltage.LOW.euPerTick() * 10, false),
                "a box takes the last unit its cell has room for");
        assertTrue(box.bank().isFull(), "and then it is full");
        // A box with a cell in it keeps its mouth, however full that cell is: what stops the block of a box
        // from asking a line again is the full buffer and not the shape of the box, see BatteryBank#canReceive.
        assertTrue(box.bank().canReceive());
        assertEquals(0, box.bank().receive(Voltage.LOW.euPerTick(), false), "and nothing else goes in");
    }

    @Test
    void aBoxOnlyTakesTheCellsOfItsOwnTier() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.MEDIUM, 1);

        assertTrue(box.acceptsItem(0, cell(BatteryChemistry.LITHIUM, Voltage.MEDIUM, 0)),
                "a cell of the tier of the box is what a box is built for");
        assertTrue(box.acceptsItem(0, cell(BatteryChemistry.ACID, Voltage.MEDIUM, 0)),
                "whatever is inside it: a cell that never takes a charge is the business of the cell");
        assertFalse(box.acceptsItem(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW, 0)),
                "a cell of the low voltage would be destroyed by a line of the middle one");
        assertFalse(box.acceptsItem(0, cell(BatteryChemistry.LITHIUM, Voltage.HIGH, 0)),
                "and one of the high voltage is no cell of a box of the middle one");
        assertFalse(box.acceptsItem(0, ItemStack.of(Items.STONE, 1)), "a stone is no cell at all");
        assertFalse(box.acceptsItem(0, ItemStack.EMPTY), "and neither is nothing");
    }

    @Test
    void aBoxIsAPanelOfCellsThatWorksNoRecipe() {
        for (Voltage tier : BatteryBoxes.tiers()) {
            for (int cells : BatteryBoxes.CELLS) {
                BatteryBoxMachine box = new BatteryBoxMachine(tier, cells);
                String what = BatteryBoxes.nameOf(tier, cells);

                assertEquals(cells, box.cells());
                assertEquals(BatteryBoxes.titleOf(tier, cells), box.name(), what + " is read under its title");
                assertTrue(box.screen().isGrid(), what + " is drawn in a panel that is a grid");
                assertEquals(cells, box.screen().gridSlots());
                assertEquals(cells, box.inventory().slotsOf(MachineInventory.Role.BATTERY).size(),
                        what + " holds one slot per cell");
                assertEquals(MachineCasing.pictureOf(tier), box.casing(),
                        what + " is built of the casing of its own tier");
                assertTrue(box.recipeTypes().isEmpty(), what + " works no recipe");
                assertEquals(0, box.tankCount(), what + " holds no tank");
            }
        }
    }

    @Test
    void theBoxesOfTheGameAreFourSizesOfThreeTiers() {
        assertEquals(12, BatteryBoxes.COUNT);
        assertEquals(List.of(1, 4, 9, 16), BatteryBoxes.CELLS);
        assertEquals(MachineFamilies.TIERS, BatteryBoxes.tiers());
        assertEquals("battery_box_lv_4", BatteryBoxes.nameOf(Voltage.LOW, 4));
        assertEquals("LV Battery Box (4 Cells)", BatteryBoxes.titleOf(Voltage.LOW, 4));
        assertEquals("HV Battery Box (1 Cell)", BatteryBoxes.titleOf(Voltage.HIGH, 1),
                "a box of one cell holds one cell and not one cells");

        assertThrows(IllegalArgumentException.class, () -> BatteryBoxes.nameOf(Voltage.EXTREME, 4),
                "the game holds no box of a tier no machine of the line was drawn for");
        assertThrows(IllegalArgumentException.class, () -> BatteryBoxes.nameOf(Voltage.LOW, 2),
                "and no box of two cells, because the panel of a box is a square");
        assertThrows(IllegalArgumentException.class, () -> new BatteryBoxMachine(Voltage.LOW, 3));
    }

    @Test
    void theChargeOfTheCellsTravelsThroughASaveGame() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 4);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW, 500));
        int amount = box.bank().amount();
        NbtCompound data = new NbtCompound("");
        box.save(data);

        BatteryBoxMachine restored = new BatteryBoxMachine(Voltage.LOW, 4);
        restored.load(data);

        assertEquals(amount, restored.bank().amount(), "a box holds the charge of its cells and nothing else");
        assertEquals(500, Batteries.of(restored.inventory().get(0)).chargeOf(restored.inventory().get(0)),
                "which is the charge the cells carried: the amount a save game holds adds nothing to it");
        assertEquals(1, restored.bank().cells());
        assertEquals(BlockFace.SOUTH, restored.faces().energyIn(), "the sides of the box travelled as well");
    }

    @Test
    void aBoxIsFilledByTheMachineBesideIt() {
        World world = new World(914, 0, 0);
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 1);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW, 0));
        // A box takes the power in through the plug a player gave it and the machine that feeds it stands west
        // of it: two machines with nothing between them, so the energy crosses the gap with no loss, see
        // EnergyNet#hand.
        box.faces().setEnergyIn(BlockFace.WEST);
        machine(world, 1, box);
        machine(world, 0, new SourceMachine(Voltage.LOW));

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(2 * Voltage.LOW.euPerTick(), box.bank().amount(),
                "the box asked for the two amperes its one cell takes and the machine beside it brought them");

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(4 * Voltage.LOW.euPerTick(), box.bank().amount(),
                "and it keeps filling as long as its cell has room");
        assertTrue(box.bank().canReceive());
    }

    @Test
    void aBoxFeedsTheMachineThatReachesForItsPower() {
        World world = new World(915, 0, 0);
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 1);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW, 200));
        machine(world, 0, box);

        // A machine that works reaches for what it needs, and the box stands west of it: what the machine
        // takes comes out of the cell of the box, an ampere of the tier a tick, see BatteryBank#maxExtract.
        TestMachine machine = new TestMachine(Voltage.LOW);
        machine.faces().setEnergyIn(BlockFace.WEST);
        machine(world, 1, machine);

        int before = box.bank().amount();
        world.tick(TickClock.TICK_SECONDS);

        assertEquals(Voltage.LOW.euPerTick(), before - box.bank().amount(),
                "the box gave what one cell of it gives in a tick: one ampere of its tier");
        assertEquals(Voltage.LOW.euPerTick(), machine.buffer().amount(),
                "and the machine that reached for it took it in");
    }

    /**
     * A stack of a cell of a chemistry and a tier, charged with the given amount.
     *
     * @param chemistry what is inside the cell
     * @param tier tier the cell was built for
     * @param charge units the cell holds, from nothing to its capacity
     * @return the stack
     */
    private static ItemStack cell(BatteryChemistry chemistry, Voltage tier, int charge) {
        Batteries.Cell cell = Batteries.of(chemistry, tier);
        ItemStack stack = ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(cell)), 1);
        stack.setDamage(cell.capacity() - charge);
        return stack;
    }

    /** Puts a machine into a world at one cell of the row of these tests and hands its block entity back. */
    private static MachineBlockEntity machine(World world, int x, Machine machine) {
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.FURNACE, machine);
        entity.setPosition(x, Y, 0);
        world.setBlock(x, Y, 0, Blocks.FURNACE);
        world.addBlockEntity(entity);
        return entity;
    }

    /** A machine that only makes power: a full buffer of its own and no work to do. */
    private static final class SourceMachine extends Machine {

        private SourceMachine(Voltage tier) {
            super(new MachineScreen("Source", ProgressKind.NONE, List.of(), List.of(), 0, 0, false),
                    new MachineInventory(), new MachineEnergyStorage(2048, 2048, 2048, tier), List.of());
            ((MachineEnergyStorage) energy()).setAmount(2048);
        }

        @Override
        protected void update(float delta) {
            // A machine of the test makes no power of its own: the test fills its buffer by hand.
        }
    }

    /** A machine of the line that works no recipe, which is what a box is drained by. */
    private static final class TestMachine extends ElectricMachine {

        private TestMachine(Voltage tier) {
            super(new MachineScreen("Idle Machine", ProgressKind.GENERIC, List.of(SlotKind.GENERIC),
                            List.of(SlotKind.GENERIC), 0, 0, false),
                    new MachineInventory(MachineInventory.Role.INPUT, MachineInventory.Role.OUTPUT), tier,
                    ElectricMachine.STANDARD_AMPS, List.of(RecipeType.SMELTING));
        }
    }
}
