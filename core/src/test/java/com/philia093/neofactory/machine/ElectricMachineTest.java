package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterial;
import com.philia093.neofactory.cable.CableMaterials;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.BatteryChemistry;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.recipe.EnergyRecipe;
import com.philia093.neofactory.recipe.RecipeGrid;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what makes a machine of the electric age an electric machine: a buffer of its tier that may only be
 * filled, the amperes it asks a line for, the recipes its voltage and its current allow, and the over-clock a
 * machine of a later tier gives to the recipes of an earlier one.
 * <p>
 * The machine of these tests brings its own recipe, because a recipe of the game lives in the registry and is
 * read while the game starts: what is checked here is the arithmetic around a recipe and not the files. The
 * world of the last test is the one of the game - a block, a cable and the entity the world ticks - so the
 * way from a block entity to the buffer of the machine is checked as well, the over-voltage included.
 */
class ElectricMachineTest {

    private static final int Y = 64;

    private static final int SEED = 7;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theBufferOfAMachineHoldsSixtyFourTicksOfItsTierAndOnlyTakes() {
        TestMachine low = machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS);

        assertEquals(2048, low.bufferCapacity(), "sixty four ticks of thirty two units");
        assertEquals(64, low.buffer().receive(1000, false),
                "a line fills it no faster than two amperes of its tier a tick");
        assertTrue(low.buffer().canReceive());
        assertFalse(low.buffer().canExtract(), "a machine that works is never a source of its line");
        assertEquals(Voltage.LOW, low.buffer().accepted(), "and it is a buffer of the tier it was built for");
        assertNotNull(low.faces().energyIn(), "a machine that takes power carries the plug it comes in through");
        assertNull(low.faces().energyOut(), "and no plug it gives power out through");
    }

    @Test
    void aMachineThatWaitsAsksOneAmpereOfItsTier() {
        TestMachine low = machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS);

        assertEquals(1, low.requestAmps());
        assertEquals(32, low.requestEu());
        assertEquals(0, low.power(), "a machine that is idle draws nothing");
        assertEquals(0, low.overclockSteps());
        assertEquals(MachineError.NONE, low.error());
    }

    @Test
    void theCasingOfAMachineOfTheLineIsTheCasingOfItsOwnTier() {
        // A side a player gives a job to is drawn by the block entity of the machine, which writes the casing
        // of the machine under the overlay of that job - the plug of the line of cables. A machine of the line
        // stands in the casing of its own age and never in the bronze of the age of steam, which is what a
        // machine that says nothing falls back to, see MachineBlockEntity#pictureOn.
        for (Voltage tier : MachineFamilies.TIERS) {
            TestMachine machine = machine(tier, ElectricMachine.STANDARD_AMPS);

            assertEquals(MachineCasing.pictureOf(tier), machine.casing(),
                    "a machine of the " + tier.displayName() + " is built of the casing of its own age");
            assertNotEquals(Machine.CASING, machine.casing(),
                    "and never of the bronze the age of steam is built of");
            assertTrue(machine.casing().contains(tier.fileName()),
                    "so a player reads the age of the machine off the name of the picture");
        }

        assertEquals("machine_lv/machine_lv", MachineCasing.pictureOf(Voltage.LOW),
                "the casing of a tier is the name of the tier spelled out");
        assertThrows(IllegalArgumentException.class, () -> MachineCasing.pictureOf(Voltage.EXTREME),
                "a tier no machine of the line was drawn for has no casing at all");
    }

    @Test
    void aWorkingMachineAsksTheAmperesOfItsWork() {
        TestMachine medium = machine(Voltage.MEDIUM, ElectricMachine.STANDARD_AMPS);
        medium.offer(recipe("electrolysis", 120, Voltage.MEDIUM, 10.0f));
        medium.buffer().setAmount(medium.bufferCapacity());

        medium.tick(TickClock.TICK_SECONDS);

        assertTrue(medium.isRunning(), "the work started");
        assertEquals(120, medium.power(), "the recipe draws its own power on the tier it asks for");
        assertEquals(0, medium.overclockSteps());
        assertEquals(2, medium.requestAmps(), "floor(120 * 2 / 128) + 1, which is what it may take");
        assertEquals(256, medium.requestEu(), "two amperes of the middle voltage");
    }

    @Test
    void aRecipeThatAsksForMoreCurrentThanTheMachineTakesNeverRuns() {
        // A hundred and twenty units a tick of the low voltage asks for eight amperes of a line of thirty two
        // volts, and a machine of the usual kind takes two: it is no recipe for that machine.
        TestMachine low = machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS);
        low.offer(recipe("grinding", 120, Voltage.LOW, 10.0f));
        low.buffer().setAmount(low.bufferCapacity());

        assertNull(low.findRecipe(), "the machine refuses a recipe it cannot pay for");
        low.tick(TickClock.TICK_SECONDS);

        assertFalse(low.isRunning(), "so it never starts the work");
        assertEquals(0, low.power());
        assertEquals(1, low.requestAmps(), "and it waits with the ampere it always waits with");
    }

    @Test
    void aMachineOfTheNextTierRunsARecipeTheLowerOneCannot() {
        TestRecipe recipe = recipe("electrolysis", 120, Voltage.MEDIUM, 10.0f);
        TestMachine low = machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS);
        TestMachine medium = machine(Voltage.MEDIUM, ElectricMachine.STANDARD_AMPS);
        low.offer(recipe);
        medium.offer(recipe);
        medium.buffer().setAmount(medium.bufferCapacity());

        assertNull(low.findRecipe(), "a machine of the low voltage never runs a recipe of the middle one");
        assertSame(recipe, medium.findRecipe(), "while the machine of the middle voltage takes it");

        medium.tick(TickClock.TICK_SECONDS);

        assertTrue(medium.isRunning());
        assertEquals(120, medium.power(), "at the power the recipe asks for");
        assertEquals(2, medium.requestAmps(), "which is two amperes of its own tier");
    }

    @Test
    void aWorseRecipeIsOverclockedByABetterMachine() {
        TestMachine low = machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS);
        TestMachine medium = machine(Voltage.MEDIUM, ElectricMachine.STANDARD_AMPS);
        low.offer(recipe("smelting", 15, Voltage.LOW, 10.0f));
        medium.offer(recipe("smelting", 15, Voltage.LOW, 10.0f));
        low.buffer().setAmount(low.bufferCapacity());
        medium.buffer().setAmount(medium.bufferCapacity());

        low.tick(TickClock.TICK_SECONDS);
        medium.tick(TickClock.TICK_SECONDS);

        assertEquals(15, low.power(), "a machine of the tier of the recipe runs it as it is");
        assertEquals(0, low.overclockSteps());
        assertEquals(60, medium.power(), "a machine of the next tier runs it at four times the power");
        assertEquals(1, medium.overclockSteps(), "and one step is what a tier is worth");
        assertEquals(1, medium.requestAmps(), "sixty units a tick is one ampere of the middle voltage");
    }

    @Test
    void aStepOfOverclockDoublesWhatACraftCosts() {
        // Fifteen units a tick over ten seconds is three thousand units on the low voltage. On the middle
        // voltage the same recipe runs at sixty a tick for five seconds, which is six thousand: a step of
        // over-clock halves the time and doubles the bill, and the machine measures its frame against it.
        assertEquals(3000, costOfOneCraft(machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS)), 30);
        assertEquals(6000, costOfOneCraft(machine(Voltage.MEDIUM, ElectricMachine.STANDARD_AMPS)), 120);
    }

    @Test
    void aMachineThatLosesPowerKeepsItsWork() {
        TestMachine medium = machine(Voltage.MEDIUM, ElectricMachine.STANDARD_AMPS);
        medium.offer(recipe("electrolysis", 120, Voltage.MEDIUM, 10.0f));
        medium.buffer().setAmount(medium.bufferCapacity());
        medium.tick(TickClock.TICK_SECONDS);
        assertTrue(medium.isRunning(), "the work started");
        float progress = medium.craftProgress();

        medium.buffer().setAmount(0);
        medium.tick(TickClock.TICK_SECONDS);
        medium.tick(TickClock.TICK_SECONDS);

        assertTrue(medium.isRunning(), "the machine waits with its work instead of losing it");
        assertEquals(progress, medium.craftProgress(), 0.001f, "and the work it did is kept");
        assertEquals(MachineError.NO_POWER, medium.error(), "and it says what it is missing");
    }

    @Test
    void aLineOfAHigherTierTakesTheMachineAway() {
        World world = new World(SEED, 0, 0);
        // A machine of the low voltage stands at the end of a copper cable, which is fed by a machine of the
        // middle voltage: it burns when it reaches for the power, while the cable of that line takes the tier
        // it is fed with and stands, and the machine that only makes power survives.
        world.setBlock(0, Y, 0, Blocks.FURNACE);
        MachineBlockEntity source = new MachineBlockEntity(BlockEntityTypes.FURNACE,
                new SourceMachine(Voltage.MEDIUM));
        source.setPosition(0, Y, 0);
        world.addBlockEntity(source);
        cable(world, 1, Y, 0);
        world.setBlock(2, Y, 0, Blocks.FURNACE);
        MachineBlockEntity machine = new MachineBlockEntity(BlockEntityTypes.FURNACE,
                machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS));
        machine.setPosition(2, Y, 0);
        machine.machine().faces().setEnergyIn(BlockFace.WEST);
        world.addBlockEntity(machine);

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(Blocks.AIR, world.getBlock(2, Y, 0), "the machine the line was too much for is gone");
        assertEquals(Cables.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.CABLE).block(),
                world.getBlock(1, Y, 0), "while the copper cable of the line takes what feeds it and stands");
        assertEquals(Blocks.FURNACE, world.getBlock(0, Y, 0), "the machine that only makes power survives");
        assertEquals(2048, source.machine().energy().amount(), "with what it held");
    }

    @Test
    void aBoxOfALaterAgeBesideAMachineDestroysIt() {
        World world = new World(916, 0, 0);
        // A box of the high voltage stands west of a furnace of the low voltage with nothing but the air
        // between them: what the furnace is fed by is the machine beside it, which is a line of the high
        // voltage, so it is destroyed the way a line of that tier destroys it - and there is no cable of that
        // line to be taken away with it, see EnergyNet#overvolts.
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.HIGH, 1);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.HIGH));
        place(world, 0, box);

        TestMachine furnace = machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS);
        furnace.faces().setEnergyIn(BlockFace.WEST);
        place(world, 1, furnace);
        int held = box.bank().amount();

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(Blocks.AIR, world.getBlock(1, Y, 0), "the furnace of the low voltage is gone");
        assertEquals(0, furnace.buffer().amount(), "and not one unit of the power was handed over");
        assertEquals(Blocks.FURNACE, world.getBlock(0, Y, 0), "while the box that would have fed it survives");
        assertEquals(held, box.bank().amount(), "with every unit its cell holds");
    }

    @Test
    void aBoxOfAnEarlierAgeBesideAMachineFeedsIt() {
        World world = new World(918, 0, 0);
        // The other way round: a box of the low voltage feeds a machine of the high voltage, because a better
        // machine takes a worse line and only the machine that asks for more than it was built for is
        // destroyed, see EnergyAcceptor#accepts.
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 1);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW));
        place(world, 0, box);

        TestMachine machine = machine(Voltage.HIGH, ElectricMachine.STANDARD_AMPS);
        machine.faces().setEnergyIn(BlockFace.WEST);
        place(world, 1, machine);
        int held = box.bank().amount();

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(Voltage.LOW.euPerTick(), held - box.bank().amount(),
                "the box gave the one ampere its cell gives in a tick");
        assertEquals(Voltage.LOW.euPerTick(), machine.buffer().amount(),
                "which the machine of the high voltage took in");
        assertEquals(Blocks.FURNACE, world.getBlock(1, Y, 0), "and it stands where it was");
    }

    @Test
    void aBoxOfALaterAgeACableAwayDestroysAMachineOfAnEarlierOne() {
        World world = new World(917, 0, 0);
        // The same two machines with a copper cable between them: the machine of the low voltage reaches for
        // the power of the box over a line the box feeds with the high voltage, so it is destroyed by it, and
        // the copper cable, which is a line of the middle voltage, cannot take that much either and melts
        // where the power enters the run. The box that gave the line its power stays where it is and keeps
        // what its cell holds, see EnergyGrid.Line#pull.
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.HIGH, 1);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.HIGH));
        place(world, 0, box);
        cable(world, 1, Y, 0);

        TestMachine furnace = machine(Voltage.LOW, ElectricMachine.STANDARD_AMPS);
        furnace.faces().setEnergyIn(BlockFace.WEST);
        place(world, 2, furnace);
        int held = box.bank().amount();

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(Blocks.AIR, world.getBlock(2, Y, 0), "the furnace of the low voltage is gone");
        assertEquals(Blocks.AIR, world.getBlock(1, Y, 0),
                "and the copper cable, which cannot take what the box feeds the line with");
        assertEquals(Blocks.FURNACE, world.getBlock(0, Y, 0), "while the box that gave the power survives");
        assertEquals(held, box.bank().amount(), "with every unit its cell holds");
    }

    @Test
    void aCableOfAnEarlierAgeBehindABoxOfALaterOneMeltsAndTheMachineStands() {
        World world = new World(919, 0, 0);
        // A box of the high voltage behind a tin cable, which is a line of the low voltage: the machine at the
        // far end was built for the high voltage and survives, while the cable that cannot take what the box
        // feeds the run with melts where the power enters it - a machine of the right tier outlives the run
        // that was too weak for it, see EnergyGrid.Line#melt.
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.HIGH, 1);
        box.inventory().set(0, cell(BatteryChemistry.LITHIUM, Voltage.HIGH));
        place(world, 0, box);
        cable(world, CableMaterials.TIN, 1, Y, 0);

        TestMachine machine = machine(Voltage.HIGH, ElectricMachine.STANDARD_AMPS);
        machine.faces().setEnergyIn(BlockFace.WEST);
        place(world, 2, machine);
        int held = box.bank().amount();

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(Blocks.AIR, world.getBlock(1, Y, 0),
                "the tin cable, which cannot take the high voltage of the box, melted where the power enters it");
        assertEquals(Blocks.FURNACE, world.getBlock(2, Y, 0), "while the machine of the high voltage survives");
        assertEquals(0, machine.buffer().amount(), "with nothing handed over while the run melted");
        assertEquals(Blocks.FURNACE, world.getBlock(0, Y, 0), "and the box that fed the run survives");
        assertEquals(held, box.bank().amount(), "with every unit its cell holds");
    }

    @Test
    void everyFamilyAndTierOfTheLineIsAMachineOfItsOwn() {
        assertEquals(24, MachineFamilies.all().size() * MachineFamilies.TIERS.size(),
                "six families of three tiers make the machines of the line");

        for (MachineFamilies.Family family : MachineFamilies.all()) {
            for (Voltage tier : MachineFamilies.TIERS) {
                String name = family.nameOf(tier);
                ElectricMachine machine = family.machine(tier);

                assertEquals(tier, machine.tier(), name + " is built for the tier it is named after");
                assertEquals(family.maxAmps(), machine.maxAmps(), name + " takes the current of its family");
                assertEquals(ElectricMachine.BUFFER_TICKS * tier.euPerTick(), machine.bufferCapacity(),
                        name + " holds sixty four ticks of its own tier");
                assertEquals(family.recipeTypes(), machine.recipeTypes(),
                        name + " works through the group of its family");
                assertEquals(family.inputs().size(),
                        machine.inventory().slotsOf(MachineInventory.Role.INPUT).size(),
                        name + " holds the input slots of its family");
                assertEquals(family.outputs().size(),
                        machine.inventory().slotsOf(MachineInventory.Role.OUTPUT).size(),
                        name + " holds the output slots of its family");

                // The screen a player reads: the tier in front of the name of the family, in the grey panel.
                MachineScreen screen = machine.screen();
                assertEquals(family.titleOf(tier), screen.title(), name + " is read under its own title");
                assertEquals(MachineStyle.NORMAL, screen.style(), "a machine of the line is drawn in grey");
                assertEquals(ProgressKind.GENERIC, screen.progress());
                assertEquals(family.inputs(), screen.inputs());
                assertEquals(family.outputs(), screen.outputs());
                assertEquals(family.fluidInputs(), screen.fluidInputs());
                assertEquals(family.fluidOutputs(), screen.fluidOutputs());
                assertEquals(family.fluidInputs() + family.fluidOutputs(), machine.tankCount(),
                        name + " holds the tanks its family names and no others");

                // And the shape of the machine is really on disk, see import_basicmachines.ps1.
                assertPresent("blocks/" + family.pictureOf(tier, "front") + ".png");
                assertPresent("models/block/" + name + ".json");
                assertPresent("blockstates/" + name + ".json");
            }
        }
    }

    @Test
    void aMachineOfTheLineRunsARecipeOfTheGroupItReads() throws IOException {
        for (MachineFamilies.Family family : MachineFamilies.all()) {
            RecipeType group = family.recipeTypes().get(0);
            MachineRecipe recipe = firstRecipeOf(group);

            for (Voltage tier : MachineFamilies.TIERS) {
                assertTrue(family.machine(tier).canRun(recipe),
                        family.nameOf(tier) + " refuses a recipe of " + group.name());
            }
        }
    }

    /** {@code true} when a file of the game is really there, the art and the shape of a machine included. */
    private static void assertPresent(String relative) {
        assertTrue(Files.isRegularFile(TestRegistries.ASSETS.resolve(relative)),
                "the machine asks for " + relative + ", which is not there");
    }

    /** The first recipe file of a group, read the way the game reads it. */
    private static MachineRecipe firstRecipeOf(RecipeType group) throws IOException {
        Path folder = TestRegistries.ASSETS.resolve(RecipeLoader.FOLDER).resolve(group.name());
        try (var entries = Files.list(folder)) {
            Path file = entries
                    .filter(path -> path.getFileName().toString().endsWith(RecipeLoader.EXTENSION))
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No recipe of " + group.name()));
            String fileName = file.getFileName().toString();
            String name = fileName.substring(0, fileName.length() - RecipeLoader.EXTENSION.length());
            return assertInstanceOf(MachineRecipe.class,
                    RecipeLoader.parse(group, name, Files.readString(file)));
        }
    }

    /** A machine of the test of a tier and a current, with one slot in and one slot out. */
    private static TestMachine machine(Voltage tier, int maxAmps) {
        return new TestMachine(tier, maxAmps);
    }

    /** A recipe of the test: one that draws power at a tier and eats nothing. */
    private static TestRecipe recipe(String name, int euPerTick, Voltage voltage, float seconds) {
        return new TestRecipe(name, RecipeType.SMELTING, ItemStack.of(Items.IRON_ORE, 1), seconds, euPerTick,
                voltage.euPerTick());
    }

    /**
     * Energy a machine spends on one whole craft of a recipe of the low voltage.
     * <p>
     * The buffer is topped up after every tick, because no line stands over a machine of a test: what one
     * craft draws is then the sum of what the machine spent, and the test reads the sum and not the buffer.
     *
     * @param machine machine that runs the craft
     * @return the energy the craft took
     */
    private static int costOfOneCraft(TestMachine machine) {
        machine.offer(recipe("smelting", 15, Voltage.LOW, 10.0f));
        machine.buffer().setAmount(machine.bufferCapacity());
        int paid = 0;
        for (int tick = 0; tick < 400; tick++) {
            machine.tick(TickClock.TICK_SECONDS);
            paid += refill(machine);
            if (!machine.isRunning()) {
                break;
            }
        }
        return paid;
    }

    /** Fills the buffer of a machine back to its brim, the way a line would, and says how much it took. */
    private static int refill(TestMachine machine) {
        MachineEnergyStorage buffer = machine.buffer();
        return buffer.receive(buffer.capacity() - buffer.amount(), false);
    }

    /** Places one copper cable, joined to the two sides a line of these tests runs along. */
    private static void cable(World world, int x, int y, int z) {
        cable(world, CableMaterials.COPPER, x, y, z);
    }

    /**
     * Places one cable of a material, joined to the two sides a line of these tests runs along.
     *
     * @param world world the cable is placed in
     * @param material metal the cable is drawn from, which is the tier of the line it is part of
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     */
    private static void cable(World world, CableMaterial material, int x, int y, int z) {
        world.setBlock(x, y, z, Cables.of(material, CableSize.SINGLE, CableKind.CABLE).block());
        world.setState(x, y, z, Cables.stateOf(Cables.mask(BlockFace.WEST, BlockFace.EAST)));
    }

    /** Puts a machine into a world at one cell of the row these tests run along, and hands its entity back. */
    private static MachineBlockEntity place(World world, int x, Machine machine) {
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.FURNACE, machine);
        entity.setPosition(x, Y, 0);
        world.setBlock(x, Y, 0, Blocks.FURNACE);
        world.addBlockEntity(entity);
        return entity;
    }

    /** A stack of one cell of a chemistry and a tier, fresh and full. */
    private static ItemStack cell(BatteryChemistry chemistry, Voltage tier) {
        return ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(Batteries.of(chemistry, tier))), 1);
    }

    /**
     * A recipe of the tests: one item out, a power a tick and a voltage, and nothing to eat.
     * <p>
     * It recognises every input, so a machine may be driven without filling a slot, and it is both a
     * {@link MachineRecipe} and an {@link EnergyRecipe}, which is the shape every recipe of the electric age
     * has.
     */
    private record TestRecipe(String name, RecipeType type, ItemStack result, float seconds, int euPerTick,
            int voltage) implements MachineRecipe, EnergyRecipe {

        @Override
        public boolean matches(RecipeGrid grid) {
            return true;
        }

        @Override
        public void consume(RecipeGrid grid) {
            // A recipe of the tests eats nothing, so a machine may be run without filling a slot.
        }
    }

    /** A machine of the electric age that carries its own recipe, the way a machine of a test does. */
    private static final class TestMachine extends ElectricMachine {

        /** Slot that holds what the machine works on. */
        static final int INPUT = 0;

        private final List<MachineRecipe> offered = new ArrayList<>();

        private TestMachine(Voltage tier, int maxAmps) {
            super(new MachineScreen("Electric Machine", ProgressKind.GENERIC,
                            List.of(SlotKind.GENERIC), List.of(SlotKind.GENERIC), 0, 0, true),
                    new MachineInventory(MachineInventory.Role.INPUT, MachineInventory.Role.OUTPUT),
                    tier, maxAmps, List.of(RecipeType.SMELTING));
        }

        /** Hands the machine the recipe it is to work on. */
        private void offer(MachineRecipe recipe) {
            offered.clear();
            offered.add(recipe);
        }

        @Override
        protected MachineRecipe recognisedRecipe() {
            return offered.isEmpty() ? super.recognisedRecipe() : offered.get(0);
        }
    }

    /** A machine of the test that only makes power: a buffer that may be emptied and no work of its own. */
    private static final class SourceMachine extends Machine {

        private SourceMachine(Voltage tier) {
            super(new MachineScreen("Source", ProgressKind.GENERIC, List.of(SlotKind.GENERIC), List.of(),
                            0, 0, false),
                    new MachineInventory(MachineInventory.Role.INPUT),
                    new MachineEnergyStorage(2048, tier), List.of());
            ((MachineEnergyStorage) energy()).setAmount(2048);
        }

        @Override
        protected void update(float delta) {
            // A machine of the test makes no power of its own: the test fills the buffer by hand.
        }
    }
}
