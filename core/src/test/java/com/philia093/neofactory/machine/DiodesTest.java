package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.DiodeBlockEntity;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceClick;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the diodes of the industry: the table that names the fifteen of them and what one of them does.
 * <p>
 * A diode is one piece of a line of the power that carries it one way: its tier is what it may carry, its
 * width is the current it passes, and the two sides a line runs in and out by are the flanks of the block,
 * which are what the block was built for and never what a player sets - a player turns the whole block with
 * the wrench instead, see {@link Diodes} and {@link DiodeMachine}. What a line does with a diode is checked
 * where a line is walked, see {@code EnergyLineTest}.
 */
class DiodesTest {

    /** Height the diodes of these tests stand at. */
    private static final int Y = 64;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theNarrowestDiodeOfATierIsNamedForItsCasing() {
        assertEquals("machine_casing_lv", Diodes.nameOf(Voltage.LOW, 1));
        assertEquals("LV Machine Casing", Diodes.titleOf(Voltage.LOW, 1));
        assertEquals("cable_diode_lv_2", Diodes.nameOf(Voltage.LOW, 2), "while every wider one names its width");
        assertEquals("LV Cable Diode 2x", Diodes.titleOf(Voltage.LOW, 2));
        assertEquals("cable_diode_hv_16", Diodes.nameOf(Voltage.HIGH, 16));
        assertEquals("HV Cable Diode 16x", Diodes.titleOf(Voltage.HIGH, 16));
    }

    @Test
    void everyDiodeOfTheGameIsNamedOnce() {
        List<String> names = new ArrayList<>();
        for (Voltage tier : Diodes.tiers()) {
            for (int width : Diodes.WIDTHS) {
                names.add(Diodes.nameOf(tier, width));
                assertEquals(width, Diodes.amperageOf(width), "a diode of N amperes carries N of them");
            }
        }
        assertEquals(Diodes.COUNT, names.size(), "five widths of three tiers");
        assertEquals(names.size(), new HashSet<>(names).size(), "and every one of them once");
    }

    @Test
    void aDiodeOfNoSuchWidthIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Diodes.nameOf(Voltage.LOW, 3));
        assertThrows(IllegalArgumentException.class, () -> Diodes.amperageOf(12));
        assertThrows(IllegalArgumentException.class, () -> Diodes.titleOf(Voltage.EXTREME, 1));
    }

    @Test
    void aDiodeHandsThePowerFromOneOfItsFlanksToTheOther() {
        DiodeMachine diode = new DiodeMachine(Voltage.MEDIUM, 2);

        assertEquals(Voltage.MEDIUM, diode.tier(), "the tier of a diode is what it may take in");
        assertEquals(2, diode.amperage(), "and its width is the current it carries across");
        assertEquals(Voltage.MEDIUM, diode.lineTier(), "and it wears the terminal of its own age");
        assertEquals(MachineCasing.pictureOf(Voltage.MEDIUM), diode.casing());
        assertFalse(diode.opensPanel(), "a diode has nothing a panel could show");

        // The two sides a diode hands the power between are the flanks of a machine that looks north: the power
        // runs in by the left one and out by the right one, and neither of them is a side a player sets.
        BlockFace in = MachineSides.leftOf(MachineSides.DEFAULT_FRONT);
        BlockFace out = MachineSides.rightOf(MachineSides.DEFAULT_FRONT);
        assertEquals(in, diode.faces().energyIn());
        assertEquals(out, diode.faces().energyOut());
        assertEquals(List.of(in), diode.inputSides(), "the line at the left flank is the one that feeds it");
        assertSame(diode.inputSide(), diode.energyOn(in), "and behind it stands the buffer power arrives in");
        assertSame(diode.outputSide(), diode.energyOn(out), "while the right one is the buffer it leaves by");
        assertNull(diode.energyOn(BlockFace.NORTH), "the front of a diode reaches nothing at all");
        assertNull(diode.energyOn(BlockFace.SOUTH), "and so does its back");
        assertEquals(Voltage.MEDIUM.euPerTick() * 2, diode.requestEu(),
                "what it wants a tick is the whole width it was built with");
    }

    @Test
    void aDiodeCarriesWhatItTookInOverToTheSideItHandsOutWithoutLosingAny() {
        DiodeMachine diode = new DiodeMachine(Voltage.MEDIUM, 2);
        diode.inputSide().setAmount(Voltage.MEDIUM.euPerTick() * 2);

        diode.tick(TickClock.TICK_SECONDS);

        assertEquals(0, diode.inputSide().amount(), "what arrived was carried over");
        assertEquals(Voltage.MEDIUM.euPerTick() * 2, diode.outputSide().amount(),
                "and stands in the side the lines behind the diode are fed from, whole");
        assertEquals(Diodes.capacityOf(Voltage.MEDIUM), diode.outputSide().capacity(),
                "a side of a diode holds the charge of its own age");
        assertEquals(Diodes.capacityOf(Voltage.MEDIUM), diode.inputSide().capacity());
    }

    @Test
    void theTwoFlanksOfADiodeTurnWithTheBlock() {
        DiodeMachine diode = new DiodeMachine(Voltage.LOW, 1);
        diode.faces().facing(BlockFace.EAST);

        assertEquals(MachineSides.leftOf(BlockFace.EAST), diode.faces().energyIn(),
                "the flanks of the diode are the flanks of the machine it is turned to");
        assertEquals(MachineSides.rightOf(BlockFace.EAST), diode.faces().energyOut());
        assertSame(diode.inputSide(), diode.energyOn(diode.faces().energyIn()),
                "and the two buffers behind them turned with it");
        assertSame(diode.outputSide(), diode.energyOn(diode.faces().energyOut()));
    }

    @Test
    void aPlayerTurnsADiodeAndMovesNoSideOfIt() {
        World world = new World(7, 0, 0);
        DiodeBlockEntity diode = place(world, Voltage.LOW, 2);
        DiodeMachine machine = (DiodeMachine) diode.machine();
        BlockFace in = machine.faces().energyIn();
        BlockFace out = machine.faces().energyOut();

        assertFalse(click(world, diode, BlockFace.EAST, FaceClick.SHIFT_LEFT),
                "the side the power runs in by is what the block was built for");
        assertFalse(click(world, diode, BlockFace.EAST, FaceClick.SHIFT_RIGHT),
                "and so is the side it runs out by");
        assertFalse(click(world, diode, BlockFace.EAST, FaceClick.LEFT), "and no left click does anything");
        assertEquals(in, machine.faces().energyIn(), "so both of them stay where they were");
        assertEquals(out, machine.faces().energyOut());

        assertTrue(click(world, diode, BlockFace.WEST, FaceClick.RIGHT),
                "while the wrench still turns the whole block");
        assertEquals(BlockFace.WEST, diode.facing(), "the diode now looks towards the side that was clicked");
        assertEquals(MachineSides.leftOf(BlockFace.WEST), machine.faces().energyIn(),
                "and its two flanks turned with it");
        assertEquals(MachineSides.rightOf(BlockFace.WEST), machine.faces().energyOut());
    }

    @Test
    void aDiodeHoldsTheChargeOfItsOwnAgeAndOfNoAgeAboveIt() {
        assertEquals(2112, Diodes.capacityOf(Voltage.LOW), "the age of the first line of the power");
        assertEquals(6912, Diodes.capacityOf(Voltage.MEDIUM), "the middle voltage");
        assertEquals(26112, Diodes.capacityOf(Voltage.HIGH), "the high voltage");
        assertThrows(IllegalArgumentException.class, () -> Diodes.capacityOf(Voltage.EXTREME),
                "no diode of the game is built above the high voltage, and no rule carries the table on");
        assertThrows(IllegalArgumentException.class, () -> Diodes.capacityOf(Voltage.ULTRA_LOW),
                "nor below the low one");
    }

    /** Places one diode of the game in a world, turned towards the north. */
    private static DiodeBlockEntity place(World world, Voltage tier, int width) {
        String name = Diodes.nameOf(tier, width);
        Block block = BlockRegistry.byName(name);
        world.setBlock(0, Y, 0, block);
        world.setState(0, Y, 0, block.states().stateOf(Map.of("facing", "north")));
        DiodeBlockEntity entity = new DiodeBlockEntity(BlockEntityRegistry.byName(name),
                new DiodeMachine(tier, width));
        entity.setPosition(0, Y, 0);
        world.addBlockEntity(entity);
        return entity;
    }

    /** One click of the wrench at a diode. */
    private static boolean click(World world, DiodeBlockEntity diode, BlockFace face, FaceClick click) {
        return diode.operateFace(world, diode.x(), diode.y(), diode.z(), face, FaceTool.WRENCH, null,
                ItemStack.EMPTY, click);
    }
}
