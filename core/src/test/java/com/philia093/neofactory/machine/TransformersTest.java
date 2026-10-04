package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.interaction.FaceRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the transformers of the industry: the table that names the six of them and what one of them does.
 * <p>
 * A transformer is two buffers of energy and a tick that hands the energy of one to the other: one side of it
 * carries the high voltage, five carry the low one, and <b>one ampere of the high side is four amperes of the
 * low one</b>, because the tiers of the line are four apart, see {@link Transformers} and
 * {@link TransformerMachine}. What a player does at one - a knock of a mallet - and what a line of cables does
 * with one is checked where the block is registered, see {@code TransformerLineTest}.
 */
class TransformersTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theTransformerOfATierJoinsItToTheAgeAbove() {
        assertEquals(Voltage.MEDIUM, Transformers.highOf(Voltage.LOW),
                "the transformer of the low voltage joins the low and the middle one");
        assertEquals(Voltage.HIGH, Transformers.highOf(Voltage.MEDIUM),
                "and the one of the middle voltage joins the middle and the high one");
        assertThrows(IllegalArgumentException.class, () -> Transformers.highOf(Voltage.HIGH),
                "there is no transformer of the high voltage, as the age above it has not been drawn");
    }

    @Test
    void theSizesOfATransformerAreItsTwoEnds() {
        assertEquals(4, Transformers.STEP, "one ampere of the high side is four of the low one");
        assertEquals(4, Transformers.lowAmperageOf(1));
        assertEquals(16, Transformers.lowAmperageOf(4));
        assertEquals(64, Transformers.lowAmperageOf(16));
        assertEquals(6, Transformers.COUNT, "two tiers of the line in three sizes each");

        assertEquals("transformer_lv", Transformers.nameOf(Voltage.LOW, 1));
        assertEquals("LV Transformer", Transformers.titleOf(Voltage.LOW, 1));
        assertEquals("transformer_mv_16", Transformers.nameOf(Voltage.MEDIUM, 16));
        assertEquals("MV 16x Transformer", Transformers.titleOf(Voltage.MEDIUM, 16));
        assertThrows(IllegalArgumentException.class, () -> Transformers.titleOf(Voltage.LOW, 2));
    }

    @Test
    void aTransformerThatStepsDownTakesItsPowerInOnTheHighSide() {
        TransformerMachine transformer = new TransformerMachine(Voltage.LOW, 1);
        BlockFace high = transformer.highSide();
        List<BlockFace> low = transformer.lowSides();

        assertEquals(Voltage.LOW, transformer.lowTier(), "a transformer is named for the age of its low side");
        assertEquals(Voltage.MEDIUM, transformer.highTier());
        assertEquals(1, transformer.highAmperage());
        assertEquals(4, transformer.lowAmperage());
        assertEquals(MachineSides.DEFAULT_FRONT, high, "the high side of a transformer is its front");
        assertEquals(5, low.size(), "and the five others are its low sides");
        assertFalse(low.contains(high));
        assertEquals(MachineCasing.pictureOf(Voltage.LOW), transformer.casing());
        assertFalse(transformer.opensPanel(), "a transformer has nothing a panel could show");

        assertTrue(transformer.isSteppingDown(), "a transformer steps down until a player says otherwise");
        assertEquals(FaceRole.ENERGY_IN, transformer.roleOn(high), "so the power runs in on the high side");
        assertEquals(FaceRole.ENERGY_OUT, transformer.roleOn(low.get(0)));
        assertEquals(List.of(high), transformer.inputSides(), "which is the one side it asks a line at");
        assertEquals(Voltage.MEDIUM, transformer.lineTier(high), "the high side carries a line of the middle one");
        assertEquals(Voltage.LOW, transformer.lineTier(low.get(3)), "and a low side one of the low one");
        assertEquals(Voltage.LOW, transformer.lineTier(), "while the machine itself is of its low age");
    }

    @Test
    void aPlayerTurnsATransformerAroundAndNoSideOfItMoves() {
        TransformerMachine transformer = new TransformerMachine(Voltage.LOW, 4);
        BlockFace high = transformer.highSide();

        transformer.toggle();

        assertFalse(transformer.isSteppingDown(), "a knock of a mallet turns the ends around");
        assertEquals(FaceRole.ENERGY_OUT, transformer.roleOn(high), "the high side now gives the power out");
        assertEquals(FaceRole.ENERGY_IN, transformer.roleOn(transformer.lowSides().get(0)));
        assertEquals(5, transformer.inputSides().size(), "so it asks at all five of its low sides");
        assertEquals(high, transformer.highSide(), "while which side is the high one never changes");

        transformer.toggle();
        assertTrue(transformer.isSteppingDown(), "and knocking again turns it back");
    }

    @Test
    void aTransformerHandsWhatItTakesOverWithoutLosingAnyOfIt() {
        TransformerMachine transformer = new TransformerMachine(Voltage.LOW, 1);
        MachineEnergyStorage in = transformer.inputBuffer();
        MachineEnergyStorage out = transformer.outputBuffer();

        assertSame(in, transformer.energyOn(transformer.highSide()), "the high side is reached at the high buffer");
        assertSame(out, transformer.energyOn(transformer.lowSides().get(2)), "and a low side at the low one");
        assertEquals(Voltage.MEDIUM.euPerTick(), transformer.requestEu(),
                "it wants an ampere of the middle voltage a tick");

        in.setAmount(Voltage.MEDIUM.euPerTick());
        transformer.tick(TickClock.TICK_SECONDS);

        assertEquals(0, in.amount(), "what it took in was handed over");
        assertEquals(Voltage.LOW.euPerTick() * 4, out.amount(), "as four amperes of the low voltage");
        assertEquals(Voltage.MEDIUM.euPerTick(), out.amount(), "which is the very energy that went in");
        assertEquals(Voltage.LOW, out.accepted(), "and what stands in the low side is a line of the low voltage");
    }

    @Test
    void aTransformerThatStepsUpAsksItsLowSidesAndFillsItsHighOne() {
        TransformerMachine transformer = new TransformerMachine(Voltage.MEDIUM, 16);
        transformer.toggle();

        MachineEnergyStorage in = transformer.inputBuffer();
        MachineEnergyStorage out = transformer.outputBuffer();
        assertSame(transformer.energyOn(transformer.lowSides().get(1)), in,
                "a transformer that steps up takes its power in on a low side");
        assertSame(transformer.energyOn(transformer.highSide()), out, "and hands it out of its high one");
        assertEquals(Voltage.HIGH.euPerTick() * 16, transformer.requestEu(),
                "it wants sixteen amperes of the high voltage, which is what its low sides bring together");

        in.setAmount(Voltage.HIGH.euPerTick() * 16);
        transformer.tick(TickClock.TICK_SECONDS);

        assertEquals(0, in.amount(), "what it took in was handed over");
        assertEquals(Voltage.HIGH.euPerTick() * 16, out.amount(),
                "so what sixteen amperes of the low side are worth arrives on the high one");
        assertEquals(Voltage.HIGH, out.accepted());
    }
}
