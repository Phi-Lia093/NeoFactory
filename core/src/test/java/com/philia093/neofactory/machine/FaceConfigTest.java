package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.interaction.FaceRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a player may make of the sides of a machine.
 * <p>
 * Every rule of the sides of a machine lives in one place, see {@link FaceConfig}, and this is where they are
 * pinned down: a side carries one job at a time, the front carries none, the wheel of a tank steps over the
 * sides another part of the machine owns, and the whole assignment travels with the machine.
 */
class FaceConfigTest {

    /** Side the machines of the tests look in, which is the one side that carries nothing. */
    private static final BlockFace FACING = BlockFace.NORTH;

    /** The sides of a boiler of the tests: a tank that is filled, one that is emptied and a vent. */
    private static FaceConfig boiler() {
        return new FaceConfig(new MachineTank.Role[] {MachineTank.Role.INPUT, MachineTank.Role.OUTPUT},
                new SimpleEnergyStorage(0)).withExhaust();
    }

    @Test
    void aMachineThatNobodyTouchedStandsWithItsPartsOnSidesOfTheirOwn() {
        FaceConfig faces = boiler();

        assertEquals(BlockFace.WEST, faces.faceOfTank(0), "the tank that is filled takes the left flank");
        assertEquals(BlockFace.EAST, faces.faceOfTank(1), "and the tank that is emptied the right one");
        assertEquals(BlockFace.SOUTH, faces.exhaust(), "the vent of the steam blows out of the back");
        assertFalse(faces.takesPower(), "a machine of steam takes no power");
        assertFalse(faces.givesPower());
        assertEquals(FaceRole.FLUID_IN, faces.roleOn(BlockFace.WEST, FACING));
        assertEquals(FaceRole.FLUID_OUT, faces.roleOn(BlockFace.EAST, FACING));
        assertEquals(FaceRole.EXHAUST, faces.roleOn(BlockFace.SOUTH, FACING));
        assertEquals(FaceRole.NONE, faces.roleOn(BlockFace.TOP, FACING),
                "a side no part of the machine stands on carries nothing");
    }

    @Test
    void aBufferThatMayBeFilledGivesAMachineAPlugToTakePowerIn() {
        FaceConfig consumer = new FaceConfig(new MachineTank.Role[0], new SimpleEnergyStorage(1000, 32, 0));
        assertTrue(consumer.takesPower());
        assertFalse(consumer.givesPower());
        assertEquals(BlockFace.SOUTH, consumer.energyIn());
        assertEquals(FaceRole.ENERGY_IN, consumer.roleOn(BlockFace.SOUTH, FACING));
        assertNull(consumer.energyOut(), "a machine that may not give has no plug to give through");

        FaceConfig generator = new FaceConfig(new MachineTank.Role[0], new SimpleEnergyStorage(1000, 0, 32));
        assertFalse(generator.takesPower(), "a generator is filled by itself and not by a line");
        assertTrue(generator.givesPower());
        assertEquals(BlockFace.EAST, generator.energyOut());
        assertEquals(FaceRole.ENERGY_OUT, generator.roleOn(BlockFace.EAST, FACING));
    }

    @Test
    void theFrontOfAMachineCarriesNothing() {
        FaceConfig faces = boiler();

        assertFalse(faces.setTank(0, FACING, FACING), "the front takes no tank");
        assertFalse(faces.setExhaust(FACING, FACING), "and no vent");
        assertEquals(BlockFace.WEST, faces.faceOfTank(0), "so the tank stayed where it was");
        assertEquals(FaceRole.NONE, faces.roleOn(FACING, FACING), "and the front carries no job at all");
    }

    @Test
    void aSideCarriesOneJobAtATime() {
        FaceConfig faces = boiler();

        // The vent moves onto the side the water is reached from: the water loses it.
        assertTrue(faces.setExhaust(BlockFace.WEST, FACING));
        assertEquals(BlockFace.WEST, faces.exhaust());
        assertNull(faces.faceOfTank(0), "the tank that stood there is left with no side at all");
        assertEquals(FaceRole.EXHAUST, faces.roleOn(BlockFace.WEST, FACING));

        // And the plug of the power takes a side away from a tank as well.
        FaceConfig burner = new FaceConfig(new MachineTank.Role[] {MachineTank.Role.INPUT},
                new SimpleEnergyStorage(1000, 32, 0));
        assertEquals(BlockFace.WEST, burner.faceOfTank(0));
        assertTrue(burner.setEnergyIn(BlockFace.WEST, FACING));
        assertNull(burner.faceOfTank(0));
        assertEquals(FaceRole.ENERGY_IN, burner.roleOn(BlockFace.WEST, FACING));
    }

    @Test
    void aMachineThatIsTurnedLeavesTheJobOfTheSideItTurnsOnto() {
        FaceConfig faces = boiler();

        assertTrue(faces.turned(BlockFace.WEST), "the left flank becomes the front of the machine");
        assertNull(faces.faceOfTank(0), "so the tank that was reached there is reached from nowhere");
        assertEquals(FaceRole.NONE, faces.roleOn(BlockFace.WEST, BlockFace.WEST));
        assertFalse(faces.turned(BlockFace.TOP), "a side that carries nothing has nothing to give up");
    }

    @Test
    void theWheelOfATankStepsOverWhatAnotherPartOwns() {
        FaceConfig faces = boiler();

        // The water is reached from the west, the steam from the east and the vent blows out of the back: the
        // wheel of the water tank walks the sides a player reads and steps over the three of them.
        assertTrue(faces.cycleTank(0, 1, FACING));
        assertNull(faces.faceOfTank(0), "a tank with no side is where the wheel starts");
        assertTrue(faces.cycleTank(0, 1, FACING));
        assertEquals(BlockFace.TOP, faces.faceOfTank(0), "and the ceiling is the first side that is free");
        assertTrue(faces.cycleTank(0, 1, FACING));
        assertEquals(BlockFace.BOTTOM, faces.faceOfTank(0));
        assertTrue(faces.cycleTank(0, 1, FACING));
        assertEquals(BlockFace.WEST, faces.faceOfTank(0), "until the wheel comes back to where it started");
        assertEquals(BlockFace.EAST, faces.faceOfTank(1), "and the steam of the boiler was never touched");
        assertTrue(faces.cycleTank(1, -1, FACING));
        assertEquals(BlockFace.BOTTOM, faces.faceOfTank(1),
                "the wheel of the steam walks the other way as well");
    }

    @Test
    void theWheelStepsOverAPlugOfThePower() {
        FaceConfig burner = new FaceConfig(new MachineTank.Role[] {MachineTank.Role.INPUT},
                new SimpleEnergyStorage(1000, 32, 0));

        assertTrue(burner.cycleTank(0, 1, FACING));
        assertNull(burner.faceOfTank(0));
        for (BlockFace expected : new BlockFace[] {BlockFace.TOP, BlockFace.BOTTOM, BlockFace.EAST,
                BlockFace.WEST}) {
            assertTrue(burner.cycleTank(0, 1, FACING));
            assertEquals(expected, burner.faceOfTank(0), "the plug of the power is stepped over");
        }
        assertEquals(BlockFace.SOUTH, burner.energyIn(), "and it stays where it was");
    }

    @Test
    void theSidesOfAMachineTravelWithIt() {
        FaceConfig faces = boiler();
        faces.setTank(0, BlockFace.TOP, FACING);
        faces.setExhaust(BlockFace.BOTTOM, FACING);
        NbtCompound data = new NbtCompound("");
        faces.save(data);

        FaceConfig loaded = boiler();
        loaded.load(data);

        assertEquals(BlockFace.TOP, loaded.faceOfTank(0));
        assertEquals(BlockFace.EAST, loaded.faceOfTank(1), "a tank that was not touched kept its side");
        assertEquals(BlockFace.BOTTOM, loaded.exhaust());
    }

    @Test
    void aMachineStoredWithoutSidesKeepsTheSidesItWasBuiltWith() {
        FaceConfig untouched = boiler();
        untouched.load(new NbtCompound(""));

        assertEquals(BlockFace.WEST, untouched.faceOfTank(0));
        assertEquals(BlockFace.EAST, untouched.faceOfTank(1));
        assertEquals(BlockFace.SOUTH, untouched.exhaust());

        // A side that was stored as none at all really is no side of the machine.
        FaceConfig emptied = boiler();
        emptied.setTank(0, null, FACING);
        NbtCompound data = new NbtCompound("");
        emptied.save(data);
        FaceConfig reloaded = boiler();
        reloaded.load(data);

        assertNull(reloaded.faceOfTank(0), "the tank was stored without a side and keeps none");
    }
}
