package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.interaction.FacePicture;
import com.philia093.neofactory.world.interaction.FaceRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the terminal of a machine: the plug of the power in the colour of the age the machine was built in.
 * <p>
 * What a side of a machine that carries a job is drawn with is the pair of pictures a block entity names, and
 * the plug of the power is the one of them that says which age the machine belongs to: the picture of the
 * pack for the low voltage, its yellow for the middle one and its orange for the high one, see
 * {@link MachineTerminals}. The pictures themselves are drawn from the grey one of the pack by
 * {@code tools/verify/import_energy_terminals.ps1}, so what is checked here is which picture a role of a
 * given age asks for, and that a machine of the game really asks for it.
 */
class MachineTerminalsTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aPlugOfALaterAgeWearsItsOwnColour() {
        assertEquals("machine_overlay/energy_in", MachineTerminals.overlayOf(FaceRole.ENERGY_IN, Voltage.LOW),
                "the low voltage is the picture the pack brought");
        assertEquals("machine_overlay/energy_in_mv",
                MachineTerminals.overlayOf(FaceRole.ENERGY_IN, Voltage.MEDIUM));
        assertEquals("machine_overlay/energy_in_hv",
                MachineTerminals.overlayOf(FaceRole.ENERGY_IN, Voltage.HIGH));
        assertEquals("machine_overlay/energy_out_mv",
                MachineTerminals.overlayOf(FaceRole.ENERGY_OUT, Voltage.MEDIUM),
                "the two plugs are coloured apart as well");
        assertEquals("machine_overlay/energy_out_hv",
                MachineTerminals.overlayOf(FaceRole.ENERGY_OUT, Voltage.HIGH));
    }

    @Test
    void aMachineThatHangsOnNoLineKeepsThePictureOfThePack() {
        assertEquals(FaceRole.ENERGY_IN.overlay(), MachineTerminals.overlayOf(FaceRole.ENERGY_IN, null),
                "a machine that names no tier wears the terminal of the youngest age");
        assertEquals(FaceRole.ENERGY_IN.overlay(),
                MachineTerminals.overlayOf(FaceRole.ENERGY_IN, Voltage.ULTRA_LOW),
                "and so does a tier the industry was not drawn in");
    }

    @Test
    void aPipeIsAPipeInEveryAge() {
        for (Voltage tier : MachineFamilies.TIERS) {
            assertEquals(FaceRole.FLUID_IN.overlay(), MachineTerminals.overlayOf(FaceRole.FLUID_IN, tier),
                    "a pipe of a fluid is the same picture in every age");
            assertEquals(FaceRole.EXHAUST.overlay(), MachineTerminals.overlayOf(FaceRole.EXHAUST, tier),
                    "and so is the vent of a machine of steam");
        }
    }

    @Test
    void everyPictureATerminalWearsIsListedOnce() {
        List<String> pictures = MachineTerminals.overlays();

        for (Voltage tier : MachineFamilies.TIERS) {
            for (FaceRole role : FaceRole.values()) {
                String picture = MachineTerminals.overlayOf(role, tier);
                if (!picture.isEmpty()) {
                    assertTrue(pictures.contains(picture),
                            "the pictures of the sides of a machine are collected for the mesher, and " + role
                                    + " of the " + tier + " is missing");
                }
            }
        }
        assertEquals(pictures.size(), new HashSet<>(pictures).size(), "and every one of them once");
        assertTrue(pictures.contains("machine_overlay/energy_in_hv"), "the terminals of both later ages");
        assertTrue(pictures.contains("machine_overlay/energy_out_mv"));
    }

    @Test
    void aMachineOfATierShowsThePlugOfItsAge() {
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.FURNACE,
                new ElectricFurnaceMachine(Voltage.MEDIUM));
        entity.machine().faces().setEnergyIn(BlockFace.WEST);

        assertEquals(new FacePicture(MachineCasing.pictureOf(Voltage.MEDIUM), "machine_overlay/energy_in_mv"),
                entity.pictureOn(BlockFace.WEST),
                "the casing of the tier with the terminal of that age over it");
        assertNull(entity.pictureOn(BlockFace.NORTH), "a side that carries no job keeps the picture of the model");
    }

    @Test
    void theTwoLaterAgesAreColouredAndTheYoungestIsNot() {
        MachineBlockEntity low = new MachineBlockEntity(BlockEntityTypes.FURNACE,
                new ElectricFurnaceMachine(Voltage.LOW));
        MachineBlockEntity high = new MachineBlockEntity(BlockEntityTypes.FURNACE,
                new ElectricFurnaceMachine(Voltage.HIGH));
        low.machine().faces().setEnergyIn(BlockFace.WEST);
        high.machine().faces().setEnergyIn(BlockFace.WEST);

        assertEquals("machine_overlay/energy_in", low.pictureOn(BlockFace.WEST).overlay(),
                "a machine of the low voltage wears the terminal of the pack");
        assertEquals("machine_overlay/energy_in_hv", high.pictureOn(BlockFace.WEST).overlay(),
                "and one of the high voltage wears it in orange");
    }

    @Test
    void theGeneratorOfAnAgeWearsTheSameTerminalAsTheMachineThatWorks() {
        MachineBlockEntity turbine = new MachineBlockEntity(BlockEntityTypes.STEAM_TURBINE_LV,
                new SteamTurbineMachine(TurbineTier.MV));
        turbine.machine().faces().setEnergyOut(BlockFace.WEST);

        assertEquals(Voltage.MEDIUM, turbine.machine().lineTier(), "a turbine of the middle voltage");
        assertEquals("machine_overlay/energy_out_mv", turbine.pictureOn(BlockFace.WEST).overlay(),
                "so the line it gives its power to is drawn in the colour of that age as well");
    }
}
