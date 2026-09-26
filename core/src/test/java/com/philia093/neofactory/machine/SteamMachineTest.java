package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.recipe.RecipeGrid;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what makes a steam machine a steam machine: a tank of sixteen thousand millibuckets, a recipe that
 * names what it spends, and an exhaust its spent steam leaves through.
 * <p>
 * The machine of these tests brings its own recipe, because a recipe of the game lives in the registry and is
 * read while the game starts: what is checked here is the loop around a recipe and not the files.
 */
class SteamMachineTest {

    private static final int Y = 201;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** Runs a machine for a stretch of time the way the world does. */
    private static void run(SteamMachine machine, float seconds) {
        int ticks = Math.round(seconds / TickClock.TICK_SECONDS);
        for (int tick = 0; tick < ticks; tick++) {
            machine.tick(TickClock.TICK_SECONDS);
        }
    }

    @Test
    void theSteamTankOfAMachineHoldsSixteenThousand() {
        TestSteamMachine machine = new TestSteamMachine();

        assertEquals(16000, machine.steam().capacity(), "the number a player reads in the machine");
        assertEquals(SteamMachine.STEAM_CAPACITY, machine.steam().capacity());
        assertTrue(machine.steam().isEmpty(), "a new machine holds no steam");
    }

    @Test
    void aRecipeSpendsItsSteamWhileItRuns() {
        TestSteamMachine machine = new TestSteamMachine();
        TestRecipe recipe = new TestRecipe("grinding", 2.0f, 1000);
        machine.offer(recipe);
        machine.steam().fill(Fluids.STEAM, 4000, false);

        run(machine, 2.2f);

        int spent = 4000 - machine.steam().amount();
        assertTrue(spent >= 1000 && spent <= 1150,
                "the craft took the thousand it asked for, and a frame of the next one: " + spent);
        assertEquals(1, machine.inventory().get(TestSteamMachine.OUTPUT).count(),
                () -> "and the product is in the output slot: " + machine + " slots " + machine.inventory());
    }

    @Test
    void aMachineWithoutEnoughSteamWaitsWithItsInput() {
        TestSteamMachine machine = new TestSteamMachine();
        machine.offer(new TestRecipe("melting", 1.0f, 8000));
        machine.steam().fill(Fluids.STEAM, 100, false);

        run(machine, 3.0f);

        assertEquals(100, machine.steam().amount(), "nothing was taken");
        assertTrue(machine.inventory().get(TestSteamMachine.OUTPUT).isEmpty(), "and nothing was made");
        assertEquals(MachineError.NO_STEAM, machine.error(),
                "a machine that cannot pay says what it is missing");
    }

    @Test
    void aCraftThatSpentSteamAsksTheBlockToLookAtTheExhaust() {
        TestSteamMachine machine = new TestSteamMachine();
        TestRecipe recipe = new TestRecipe("grinding", 0.5f, 400);

        machine.finishCraft(recipe);

        assertTrue(machine.takesAnExhaustCheck(), "the block has to look at the exhaust once");
        assertFalse(machine.takesAnExhaustCheck(), "and only once");

        machine.finishCraft(new TestRecipe("free", 0.5f, 0));

        assertFalse(machine.takesAnExhaustCheck(),
                "a recipe that spends no steam leaves nothing to blow out");
        assertEquals(400, SteamMachine.steamOf(recipe), "the recipe names its steam");
        assertEquals(0, SteamMachine.steamOf(new TestRecipe("free", 0.5f, 0)));
    }

    @Test
    void anExhaustThatWasFoundBlockedRefusesTheNextRecipe() {
        TestSteamMachine machine = new TestSteamMachine();
        machine.steam().fill(Fluids.STEAM, 4000, false);
        machine.offer(new TestRecipe("grinding", 1.0f, 400));

        machine.reportExhaust(true);

        assertTrue(machine.isWaitingForExhaust());
        assertEquals(MachineError.NO_EXHAUST, machine.error());
        assertNull(machine.findRecipe(), "the machine refuses the next recipe");

        machine.reportExhaust(false);

        assertFalse(machine.isWaitingForExhaust(), "a free exhaust lets it breathe again");
        assertEquals(MachineError.NONE, machine.error());
    }

    @Test
    void theExhaustIsLookedAtInTheWorld() {
        World world = new World(911, 0, 0);
        TestSteamMachine machine = new TestSteamMachine();
        BlockEntityType type = new BlockEntityType("test_steam_machine", TestEntity::new);
        MachineBlockEntity entity = new MachineBlockEntity(type, machine);
        entity.setPosition(3, Y, 5);
        world.setBlock(3, Y, 5, Blocks.BRONZE_BOILER);
        world.addBlockEntity(entity);

        // The exhaust of a machine that was never turned blows out of its south side, and a wall raised there
        // is what a player is told about: the craft in the machine ends, the next one waits for the way out.
        world.setBlock(3, Y, 6, Blocks.STONE);
        machine.finishCraft(new TestRecipe("grinding", 0.5f, 400));
        entity.tick(world, TickClock.TICK_SECONDS);

        assertTrue(machine.isWaitingForExhaust(), "the wall was found");
        assertEquals(MachineError.NO_EXHAUST, machine.error());

        world.setBlock(3, Y, 6, Blocks.AIR);
        entity.tick(world, TickClock.TICK_SECONDS);

        assertFalse(machine.isWaitingForExhaust(), "clearing the face lets the machine run on");
    }

    @Test
    void theWrenchTurnsAMachineAndSetsItsExhaust() {
        World world = new World(912, 0, 0);
        TestSteamMachine machine = new TestSteamMachine();
        BlockEntityType type = new BlockEntityType("test_steam_machine", TestEntity::new);
        MachineBlockEntity entity = new MachineBlockEntity(type, machine);
        entity.setPosition(3, Y, 5);
        world.setBlock(3, Y, 5, Blocks.BRONZE_BOILER);
        world.addBlockEntity(entity);

        assertEquals(BlockFace.NORTH, entity.facing(), "a machine that was just built looks north");
        assertEquals(BlockFace.SOUTH, entity.exhaustFace(), "and blows its steam out of the back");

        assertTrue(turn(entity, world, BlockFace.WEST, FaceTool.WRENCH, false), "the wrench turns the machine");
        assertEquals(BlockFace.WEST, entity.facing());
        assertTrue(turn(entity, world, BlockFace.TOP, FaceTool.WRENCH, true),
                "and the modifier key sets the exhaust");
        assertEquals(BlockFace.TOP, entity.exhaustFace());
        assertEquals(BlockFace.WEST, entity.facing(), "without turning the machine");

        assertFalse(turn(entity, world, BlockFace.EAST, FaceTool.SCREWDRIVER, false),
                "another tool does nothing");
        assertFalse(turn(entity, world, BlockFace.EAST, FaceTool.EMPTY_HAND, true),
                "and the modifier key of an empty hand sets nothing");
    }

    /** Works on one face of a machine the way a click on a cell of the grid does. */
    private static boolean turn(MachineBlockEntity entity, World world, BlockFace face, FaceTool tool,
            boolean modifier) {
        return entity.operateFace(world, entity.x(), entity.y(), entity.z(), face, tool, null,
                ItemStack.of(Items.WRENCH, 1), modifier);
    }

    /** A block entity of the tests, which is what the world stores for a machine. */
    private static final class TestEntity extends MachineBlockEntity {
        TestEntity(BlockEntityType type) {
            super(type, new TestSteamMachine());
        }
    }

    /** A recipe of the tests: a name, a duration and the steam it spends. */
    private record TestRecipe(String name, float seconds, int steam) implements MachineRecipe {

        @Override
        public RecipeType type() {
            return RecipeType.SMELTING;
        }

        @Override
        public ItemStack result() {
            return ItemStack.of(Items.IRON_ORE, 1);
        }

        @Override
        public boolean matches(RecipeGrid grid) {
            return true;
        }

        @Override
        public void consume(RecipeGrid grid) {
            // A recipe of the tests eats nothing, so a machine may be run without filling a slot.
        }
    }

    /** A steam machine of the tests: one slot in, one slot out and a recipe a test hands it. */
    static final class TestSteamMachine extends SteamMachine {

        /** Slot that holds what the machine works on. */
        static final int INPUT = 0;

        /** Slot the product appears in. */
        static final int OUTPUT = 1;

        private final List<MachineRecipe> offered = new ArrayList<>();

        TestSteamMachine() {
            super(new MachineScreen("Steam Machine", ProgressKind.GENERIC,
                            List.of(SlotKind.GENERIC), List.of(SlotKind.GENERIC), 0, 0, true),
                    new MachineInventory(MachineInventory.Role.INPUT, MachineInventory.Role.OUTPUT),
                    List.of(RecipeType.SMELTING));
        }

        /** Hands the machine the recipe it is to work on. */
        void offer(MachineRecipe recipe) {
            offered.clear();
            offered.add(recipe);
        }

        /** Ends a craft the way the loop does, so a test can drive what a machine says afterwards. */
        void finishCraft(MachineRecipe recipe) {
            craftFinished(recipe);
        }

        @Override
        protected MachineRecipe recognisedRecipe() {
            if (isWaitingForExhaust()) {
                return null;
            }
            return offered.isEmpty() ? super.recognisedRecipe() : offered.get(0);
        }
    }
}
