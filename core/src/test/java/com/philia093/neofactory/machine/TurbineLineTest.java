package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterials;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.energy.EnergyGrid;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks a steam turbine against a world: the steam it drinks, the line it feeds and the vent it has to keep
 * open.
 * <p>
 * The machine is the one the block of the game carries and it is put into the world the way a player builds
 * it, so what is checked is the way from the tank of the turbine through the line of cables into the buffer of
 * the next machine - the whole of what the first machine of the game that makes power is for, see
 * {@link SteamTurbineMachine} and {@code MachineBlockEntity#updateEnergy}.
 */
class TurbineLineTest {

    private static final int SEED = 41;

    /** Height the machines and the cable of these tests stand at. */
    private static final int Y = 64;

    /** Capacity of the tank of a turbine, the size every machine of the age of steam holds. */
    private static final int STEAM = SteamMachine.STEAM_CAPACITY;

    /** Capacity of the buffer of the machine that takes the power of a turbine. */
    private static final int BUFFER = 4096;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aTurbineFeedsTheLineAtItsPlug() {
        World world = new World(SEED, 0, 0);
        SteamTurbineMachine turbine = turbine(world, 0, Y, 0);
        // One copper cable between the plug of the turbine and the machine that works on the power.
        cable(world, 1, Y, 0);
        MachineBlockEntity sink = sink(world, 2, Y, 0);
        EnergyGrid.Line line = line(world, 1, Y, 0);

        settle(world, 1);

        assertEquals(turbine.euPerTick() - line.net().totalLoss(), bufferOf(sink).amount(),
                "the ampère of the turbine arrived, less the loss of the cable");
        assertEquals(0, turbine.energy().amount(),
                "and the buffer of the turbine is empty: the line took what the tick made");
        assertEquals(STEAM - turbine.steamPerTick(), turbine.steam().amount());
    }

    @Test
    void aTurbineWithNoLineKeepsWhatItMakes() {
        World world = new World(SEED, 1, 0);
        SteamTurbineMachine turbine = turbine(world, 0, Y, 0);

        settle(world, 1);

        assertEquals(turbine.euPerTick(), turbine.energy().amount(),
                "a turbine with nothing to feed fills its own buffer");
        assertTrue(turbine.isRunning());
    }

    @Test
    void aTurbineWhoseVentIsWalledInStandsStill() {
        World world = new World(SEED, 2, 0);
        SteamTurbineMachine turbine = turbine(world, 0, Y, 0);
        // The vent of a machine that looks north blows out of its back, so the wall is built there.
        world.setBlock(0, Y, 1, Blocks.STONE);

        settle(world, 2);

        assertEquals(STEAM - turbine.steamPerTick(), turbine.steam().amount(),
                "the steam of the first tick is gone and no more is drunk");
        assertEquals(turbine.euPerTick(), turbine.energy().amount());
        assertEquals(MachineError.NO_EXHAUST, turbine.error(), "and the machine reports its walled vent");
    }

    /** Places a turbine of bronze with its tank full, the way a player builds one. */
    private static SteamTurbineMachine turbine(World world, int x, int y, int z) {
        world.setBlock(x, y, z, Blocks.STEAM_TURBINE_LV);
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.STEAM_TURBINE_LV,
                new SteamTurbineMachine(TurbineTier.LV));
        entity.setPosition(x, y, z);
        world.addBlockEntity(entity);
        SteamTurbineMachine turbine = (SteamTurbineMachine) entity.machine();
        turbine.steam().fill(Fluids.STEAM, STEAM, false);
        return turbine;
    }

    /** Places one copper cable, joined to the two sides a line of these tests runs along. */
    private static void cable(World world, int x, int y, int z) {
        world.setBlock(x, y, z, Cables.of(CableMaterials.COPPER, CableSize.SINGLE, CableKind.CABLE)
                .block());
        world.setState(x, y, z, Cables.stateOf(Cables.mask(BlockFace.WEST, BlockFace.EAST)));
    }

    /** Places a machine of the test that takes power in through its west side. */
    private static MachineBlockEntity sink(World world, int x, int y, int z) {
        world.setBlock(x, y, z, Blocks.FURNACE);
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.FURNACE, new TestSink());
        entity.setPosition(x, y, z);
        entity.machine().faces().setEnergyIn(BlockFace.WEST);
        world.addBlockEntity(entity);
        return entity;
    }

    /** Lets the world tick a while, which is what moves the steam and the power of these tests. */
    private static void settle(World world, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    /** The line of cables the cable of a cell belongs to. */
    private static EnergyGrid.Line line(World world, int x, int y, int z) {
        EnergyGrid.Line line = EnergyGrid.line(EnergyGrid.of(world), x, y, z);
        assertNotNull(line, "a cable stands in that cell");
        return line;
    }

    /** Buffer of a machine behind a block. */
    private static MachineEnergyStorage bufferOf(MachineBlockEntity entity) {
        return (MachineEnergyStorage) entity.machine().energy();
    }

    /** A machine of the test: a buffer of the middle voltage, one slot and no work of its own. */
    private static final class TestSink extends Machine {

        private TestSink() {
            super(new MachineScreen("Test", ProgressKind.GENERIC, List.of(SlotKind.SMELTING), List.of(),
                            0, 0, false),
                    new MachineInventory(MachineInventory.Role.INPUT),
                    new MachineEnergyStorage(BUFFER, Voltage.MEDIUM), List.of());
        }

        @Override
        protected void update(float delta) {
            // A machine of the test takes the power of a line and does nothing with it.
        }
    }
}
