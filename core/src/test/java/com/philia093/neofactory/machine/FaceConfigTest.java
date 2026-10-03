package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.interaction.FaceRole;
import com.philia093.neofactory.world.save.SaveTags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a player may make of the sides of a machine.
 * <p>
 * Every rule of the sides of a machine lives in one place, see {@link FaceConfig}, and this is where they are
 * pinned down: <b>a side is a word a player reads at the machine and not a side of the world</b>, so the sides
 * of a machine travel with it and its front is never covered; a side carries one job at a time; a machine that
 * holds no part of a kind refuses that job; the wheel of a tank steps over the sides another part of the
 * machine owns; and the words of the assignment travel with the machine.
 */
class FaceConfigTest {

    /** The sides of a boiler of the tests: a tank that is filled, one that is emptied and a vent. */
    private static FaceConfig boiler() {
        return new FaceConfig(new MachineTank.Role[] {MachineTank.Role.INPUT, MachineTank.Role.OUTPUT},
                new SimpleEnergyStorage(0)).withExhaust();
    }

    /** The sides of a machine of the tests with one tank that is filled and a buffer that takes power in. */
    private static FaceConfig burner() {
        return new FaceConfig(new MachineTank.Role[] {MachineTank.Role.INPUT},
                new SimpleEnergyStorage(1000, 32, 0));
    }

    @Test
    void theSidesOfAMachineAreWordsAPlayerReads() {
        FaceConfig faces = boiler();

        assertEquals(MachineSides.RIGHT, faces.nameOfTank(0), "the tank that is filled takes the right flank");
        assertEquals(MachineSides.LEFT, faces.nameOfTank(1), "and the one that is emptied the left flank");
        assertEquals(MachineSides.NONE, faces.nameOfEnergyIn(), "a machine of steam takes no power");
        assertEquals(MachineSides.NONE, faces.nameOfEnergyOut());
        assertFalse(faces.takesPower());
        assertFalse(faces.givesPower());

        // A machine nobody has turned looks north, which is where those words come to the sides they always
        // stood on: the right flank of a machine that looks north is its west.
        assertEquals(BlockFace.NORTH, faces.facing());
        assertEquals(BlockFace.WEST, faces.faceOfTank(0));
        assertEquals(BlockFace.EAST, faces.faceOfTank(1));
        assertEquals(BlockFace.SOUTH, faces.exhaust());
        assertEquals(FaceRole.FLUID_IN, faces.roleOn(BlockFace.WEST));
        assertEquals(FaceRole.FLUID_OUT, faces.roleOn(BlockFace.EAST));
        assertEquals(FaceRole.EXHAUST, faces.roleOn(BlockFace.SOUTH));
        assertEquals(FaceRole.NONE, faces.roleOn(BlockFace.TOP), "a side no part stands on carries nothing");
    }

    @Test
    void theSidesOfAMachineTravelWithIt() {
        FaceConfig faces = boiler();
        faces.facing(BlockFace.EAST);

        assertEquals(MachineSides.RIGHT, faces.nameOfTank(0), "the water stays on the right flank");
        assertEquals(BlockFace.NORTH, faces.faceOfTank(0), "which is the north of a machine that looks east");
        assertEquals(MachineSides.LEFT, faces.nameOfTank(1));
        assertEquals(BlockFace.SOUTH, faces.faceOfTank(1), "and the steam on the left flank of that machine");
        assertEquals(BlockFace.WEST, faces.exhaust(), "while the vent keeps blowing out of the back");
        assertEquals(FaceRole.NONE, faces.roleOn(BlockFace.EAST), "the front of the machine carries nothing");
        assertEquals(FaceRole.FLUID_IN, faces.roleOn(BlockFace.NORTH));
    }

    @Test
    void aBufferThatMayBeFilledGivesAMachineAPlugToTakePowerIn() {
        FaceConfig consumer = burner();

        assertTrue(consumer.takesPower());
        assertFalse(consumer.givesPower());
        assertEquals(MachineSides.BACK, consumer.nameOfEnergyIn(), "the plug of a machine is on its back");
        assertEquals(BlockFace.SOUTH, consumer.energyIn());
        assertEquals(FaceRole.ENERGY_IN, consumer.roleOn(BlockFace.SOUTH));
        assertEquals(MachineSides.NONE, consumer.nameOfEnergyOut(),
                "a machine that may not give has no plug to give through");

        FaceConfig generator = new FaceConfig(new MachineTank.Role[0], new SimpleEnergyStorage(1000, 0, 32));
        assertFalse(generator.takesPower(), "a generator is filled by itself and not by a line");
        assertTrue(generator.givesPower());
        assertEquals(MachineSides.LEFT, generator.nameOfEnergyOut());
        assertEquals(BlockFace.EAST, generator.energyOut());
        assertEquals(FaceRole.ENERGY_OUT, generator.roleOn(BlockFace.EAST));
    }

    @Test
    void theFrontOfAMachineCarriesNothing() {
        FaceConfig faces = boiler();

        assertFalse(faces.setTank(0, BlockFace.NORTH), "the front takes no tank");
        assertFalse(faces.setExhaust(BlockFace.NORTH), "and no vent");
        assertEquals(MachineSides.RIGHT, faces.nameOfTank(0), "so the tank stayed where it was");

        // And a machine that is turned onto a side never covers it either: the words follow the new front.
        faces.facing(BlockFace.WEST);
        assertEquals(FaceRole.NONE, faces.roleOn(BlockFace.WEST));
        assertFalse(faces.setTank(0, BlockFace.WEST), "the front of a machine that was turned is its west");
        assertEquals(MachineSides.RIGHT, faces.nameOfTank(0));
        assertEquals(BlockFace.SOUTH, faces.faceOfTank(0),
                "and the water is still on the right flank of the machine, which is its south now");
    }

    @Test
    void aSideCarriesOneJobAtATime() {
        FaceConfig faces = boiler();

        // The vent moves onto the flank the water is reached over: the water loses it.
        assertTrue(faces.setExhaust(BlockFace.WEST));
        assertEquals(BlockFace.WEST, faces.exhaust());
        assertEquals(MachineSides.NONE, faces.nameOfTank(0),
                "the tank that stands on that flank is left with no side at all");
        assertEquals(FaceRole.EXHAUST, faces.roleOn(BlockFace.WEST));

        // And the plug of the power takes a side away from a tank as well.
        FaceConfig burner = burner();
        assertEquals(MachineSides.RIGHT, burner.nameOfTank(0));
        assertTrue(burner.setEnergyIn(BlockFace.WEST));
        assertEquals(MachineSides.NONE, burner.nameOfTank(0), "the plug took the side of the tank");
        assertEquals(FaceRole.ENERGY_IN, burner.roleOn(BlockFace.WEST));
    }

    @Test
    void aMachineThatHoldsNoPartOfAKindRefusesTheJob() {
        FaceConfig faces = new FaceConfig(new MachineTank.Role[] {MachineTank.Role.INPUT},
                new SimpleEnergyStorage(0));

        assertFalse(faces.setEnergyIn(BlockFace.WEST), "a machine without a buffer has no plug");
        assertFalse(faces.setEnergyOut(BlockFace.WEST));
        assertFalse(faces.setExhaust(BlockFace.WEST), "and a machine that does not breathe has no vent");
        assertFalse(faces.setTank(7, BlockFace.WEST), "and a tank it does not hold has no side to set");
        assertTrue(faces.setTank(0, null), "the side of a tank is taken away with nothing at all");
        assertEquals(MachineSides.NONE, faces.nameOfTank(0));
    }

    @Test
    void theWheelOfATankStepsOverWhatAnotherPartOwns() {
        FaceConfig faces = boiler();

        // The water stands on the right flank, the steam on the left one and the vent blows out of the back:
        // the wheel walks the words a player reads and steps over the three of them.
        assertTrue(faces.cycleTank(0, 1));
        assertEquals(MachineSides.NONE, faces.nameOfTank(0), "a tank with no side is where the wheel starts");
        assertTrue(faces.cycleTank(0, 1));
        assertEquals(MachineSides.UP, faces.nameOfTank(0), "and the ceiling is the first word that is free");
        assertTrue(faces.cycleTank(0, 1));
        assertEquals(MachineSides.DOWN, faces.nameOfTank(0));
        assertTrue(faces.cycleTank(0, 1));
        assertEquals(MachineSides.RIGHT, faces.nameOfTank(0), "until the wheel comes back to where it began");
        assertEquals(MachineSides.LEFT, faces.nameOfTank(1), "and the steam of the boiler was never touched");
        assertTrue(faces.cycleTank(1, -1));
        assertEquals(MachineSides.DOWN, faces.nameOfTank(1),
                "the wheel of the steam walks the other way as well");
        assertFalse(faces.cycleTank(0, 0), "a wheel that did not turn changes nothing");
    }

    @Test
    void theWheelStepsOverAPlugOfThePower() {
        FaceConfig burner = burner();

        assertTrue(burner.cycleTank(0, 1));
        assertEquals(MachineSides.NONE, burner.nameOfTank(0));
        for (String expected : new String[] {MachineSides.UP, MachineSides.DOWN, MachineSides.LEFT,
                MachineSides.RIGHT}) {
            assertTrue(burner.cycleTank(0, 1));
            assertEquals(expected, burner.nameOfTank(0), "the plug of the power is stepped over");
        }
        assertEquals(MachineSides.BACK, burner.nameOfEnergyIn(), "and it stays where it was");
    }

    @Test
    void theSidesOfAMachineTravelWithASaveGame() {
        FaceConfig faces = boiler();
        faces.setTank(0, BlockFace.TOP);
        faces.setExhaust(BlockFace.BOTTOM);
        NbtCompound data = new NbtCompound("");
        faces.save(data);

        FaceConfig loaded = boiler();
        loaded.facing(BlockFace.SOUTH);
        loaded.load(data);

        assertEquals(MachineSides.UP, loaded.nameOfTank(0), "the words travelled and not sides of the world");
        assertEquals(MachineSides.LEFT, loaded.nameOfTank(1), "a tank that was not touched kept its side");
        assertEquals(MachineSides.DOWN, loaded.nameOfExhaust());
        assertEquals(BlockFace.TOP, loaded.faceOfTank(0),
                "the ceiling of a machine is its ceiling whichever way its front is turned");
    }

    @Test
    void aMachineStoredWithoutSidesKeepsTheSidesItWasBuiltWith() {
        FaceConfig untouched = boiler();
        untouched.load(new NbtCompound(""));

        assertEquals(MachineSides.RIGHT, untouched.nameOfTank(0));
        assertEquals(MachineSides.LEFT, untouched.nameOfTank(1));
        assertEquals(MachineSides.BACK, untouched.nameOfExhaust());

        // A side that was stored as none at all really is no side of the machine.
        FaceConfig emptied = boiler();
        emptied.setTank(0, null);
        NbtCompound data = new NbtCompound("");
        emptied.save(data);
        FaceConfig reloaded = boiler();
        reloaded.load(data);

        assertEquals(MachineSides.NONE, reloaded.nameOfTank(0), "the tank was stored without a side");
    }

    @Test
    void aSideOfTheWorldIsNoSideOfAMachine() {
        // The sides of a machine were stored as sides of the world before they became words a player reads,
        // and a world that is that old is refused anyway: a word that names no side of a machine is read as no
        // side at all, see FaceConfig#stored(String).
        NbtCompound faces = new NbtCompound(SaveTags.MACHINE_FACES);
        faces.putString("Tank0", "WEST");
        NbtCompound data = new NbtCompound("");
        data.put(faces);

        FaceConfig loaded = boiler();
        loaded.load(data);

        assertEquals(MachineSides.NONE, loaded.nameOfTank(0), "the stored west is no word a machine reads");
        assertEquals(MachineSides.LEFT, loaded.nameOfTank(1), "and the rest of the machine is untouched");
    }
}
