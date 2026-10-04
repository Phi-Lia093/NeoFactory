package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceClick;
import com.philia093.neofactory.world.interaction.FaceMark;
import com.philia093.neofactory.world.interaction.FaceRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the sides of a machine that has no front at all, which is what a box of cells is.
 * <p>
 * A machine of the game is built with its front to the north and that front carries no job: a pipe, a cable
 * and a belt are built towards the other five sides and never towards the one the machine shows, see
 * {@link FaceConfig}. <b>A box of cells has no such side.</b> A player fills it from wherever they stand, so
 * every one of its six sides may be given a job, it is never turned, and no side of it is crossed out by the
 * grid of faces. What such a machine says about its sides is the side of the world itself - {@code NORTH},
 * {@code UP} - because {@code LEFT} and {@code BACK} are words about a front it does not have, see
 * {@link MachineSides}.
 */
class MachineWithoutFrontTest {

    private static final int Y = 64;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aMachineWithoutAFrontMayBeReachedFromEverySide() {
        FaceConfig faces = box();

        assertFalse(faces.hasFront(), "a box of cells has no mouth a player stands in front of");
        assertNull(faces.facing(), "so it looks nowhere at all");
        assertEquals("SOUTH", faces.nameOfEnergyIn(), "and names its sides the way the world does");
        assertEquals("EAST", faces.nameOfEnergyOut());
        assertEquals(BlockFace.SOUTH, faces.energyIn(), "the plug of the power in keeps the side it always stood on");
        assertEquals(BlockFace.EAST, faces.energyOut());
        assertEquals(FaceRole.ENERGY_IN, faces.roleOn(BlockFace.SOUTH));
        assertEquals(FaceRole.ENERGY_OUT, faces.roleOn(BlockFace.EAST));
        assertEquals(FaceRole.NONE, faces.roleOn(BlockFace.TOP), "the rest of the box carries nothing");

        // The side a machine of the game is built towards is no side of a box at all, so a plug may be put
        // there like it is put on any other one of the six.
        assertTrue(faces.setEnergyIn(BlockFace.TOP));
        assertEquals(BlockFace.TOP, faces.energyIn());
        assertEquals(MachineSides.UP, faces.nameOfEnergyIn());
        assertTrue(faces.setEnergyOut(null), "and a plug may be taken away again");
        assertNull(faces.energyOut());

        // A box that is turned is a box that stands still: there is no front to be turned towards.
        faces.facing(BlockFace.WEST);
        assertNull(faces.facing());
        assertFalse(faces.hasFront());
    }

    @Test
    void oneSideOfABoxCarriesOneJob() {
        FaceConfig faces = box();

        assertTrue(faces.setEnergyOut(BlockFace.SOUTH), "the plug out takes the side of the plug in");

        assertNull(faces.energyIn(), "which lost the side it stood on");
        assertEquals(BlockFace.SOUTH, faces.energyOut());
        assertEquals(FaceRole.ENERGY_OUT, faces.roleOn(BlockFace.SOUTH));

        assertTrue(faces.setEnergyIn(BlockFace.SOUTH), "and it takes the side back");
        assertNull(faces.energyOut());
        assertEquals(FaceRole.ENERGY_IN, faces.roleOn(BlockFace.SOUTH));
    }

    @Test
    void theSidesOfABoxTravelWithASaveGame() {
        FaceConfig faces = box();
        faces.setEnergyIn(BlockFace.TOP);
        NbtCompound data = new NbtCompound("");
        faces.save(data);

        FaceConfig loaded = box();
        loaded.load(data);

        assertEquals("UP", loaded.nameOfEnergyIn(), "a box is stored with the sides of the world");
        assertEquals(BlockFace.TOP, loaded.energyIn());
        assertEquals("EAST", loaded.nameOfEnergyOut(), "and a plug nobody touched stays where it was");
    }

    @Test
    void theWrenchGivesEverySideOfABoxAJobAndNeverTurnsIt() {
        World world = new World(913, 0, 0);
        TestMachine machine = new TestMachine();
        MachineBlockEntity entity = new MachineBlockEntity(new BlockEntityType("test_box", TestEntity::new),
                machine);
        entity.setPosition(3, Y, 5);
        world.setBlock(3, Y, 5, Blocks.BRONZE_BOILER);
        world.addBlockEntity(entity);
        entity.tick(world, TickClock.TICK_SECONDS);

        // The first tick of a machine reads the side it was built towards out of the block and hands it to the
        // machine, see MachineBlockEntity#settleFacing: a box has no front to take over, so it keeps the sides
        // its plugs stood on.
        assertFalse(machine.faces().hasFront());
        assertEquals(BlockFace.SOUTH, machine.faces().energyIn());
        assertEquals(BlockFace.EAST, machine.faces().energyOut());

        assertTrue(operate(entity, world, BlockFace.TOP, FaceClick.SHIFT_LEFT),
                "the wrench puts the plug of the power in on the ceiling of a box");
        assertEquals(BlockFace.TOP, machine.faces().energyIn());

        assertFalse(operate(entity, world, BlockFace.WEST, FaceClick.RIGHT),
                "and it never turns a box, because there is no front to turn towards");
        assertEquals(BlockFace.TOP, machine.faces().energyIn(), "so the box is as it was");

        assertFalse(operate(entity, world, BlockFace.NORTH, FaceTool.SCREWDRIVER, FaceClick.SHIFT_LEFT),
                "another tool sets nothing");

        // The grid of faces crosses out the front of a machine; a box has no such side, so every one of the
        // six carries what it really holds.
        for (BlockFace side : BlockFace.ALL) {
            assertNotEquals(FaceMark.CLOSED, entity.faceMark(world, 3, Y, 5, side),
                    "no side of a box carries nothing by being its front");
        }
        assertEquals(FaceMark.ENERGY_IN, entity.faceMark(world, 3, Y, 5, BlockFace.TOP));
        assertEquals(FaceMark.ENERGY_OUT, entity.faceMark(world, 3, Y, 5, BlockFace.EAST));
        assertEquals(FaceMark.NOTHING, entity.faceMark(world, 3, Y, 5, BlockFace.WEST));
    }

    /** The sides of a box of the tests: a buffer that may be filled and emptied and no front. */
    private static FaceConfig box() {
        return new FaceConfig(new MachineTank.Role[0], new SimpleEnergyStorage(1000, 32, 32)).withoutFront();
    }

    /** Works on one side of a box the way a click of the wrench does. */
    private static boolean operate(MachineBlockEntity entity, World world, BlockFace face, FaceClick click) {
        return operate(entity, world, face, FaceTool.WRENCH, click);
    }

    /** Works on one side of a box with one tool. */
    private static boolean operate(MachineBlockEntity entity, World world, BlockFace face, FaceTool tool,
            FaceClick click) {
        return entity.operateFace(world, entity.x(), entity.y(), entity.z(), face, tool, null,
                ItemStack.of(Items.WRENCH, 1), click);
    }

    /** A machine of the tests: a box of cells, a buffer of its own and no front. */
    private static final class TestMachine extends Machine {

        private TestMachine() {
            super(new MachineScreen("Box", ProgressKind.NONE, List.of(), List.of(), 0, 0, false),
                    new MachineInventory(), new SimpleEnergyStorage(1000, 32, 32), List.of());
            faces().withoutFront();
        }

        @Override
        protected void update(float delta) {
            // A box of cells does no work of its own: the power it holds is in the cells that stand in it.
        }
    }

    /** A block entity of the tests, which is what a world stores for a machine. */
    private static final class TestEntity extends MachineBlockEntity {

        TestEntity(BlockEntityType type) {
            super(type, new TestMachine());
        }
    }
}
