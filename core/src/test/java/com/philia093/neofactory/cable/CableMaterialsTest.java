package com.philia093.neofactory.cable;

import com.philia093.neofactory.material.Material;
import com.philia093.neofactory.material.MaterialRegistry;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table of the cables: which material is a line of which voltage, how much current one cable of
 * it takes, what it loses and how much heat it stands.
 * <p>
 * The numbers themselves are the table of the industry and no arithmetic, so what is checked here is the
 * shape of the table: the ladder only ever climbs, every material loses nothing or something, a
 * superconductor of every age is written down where the age ends, and a handful of cells is pinned down as
 * they are, see {@link #theNumbersOfTheTableAreTheNumbersOfTheIndustry()}.
 */
class CableMaterialsTest {

    /** The ages that bring a superconductor with them, the middle voltage and every one above it. */
    private static final List<Voltage> AGES_WITH_A_SUPERCONDUCTOR = List.of(Voltage.MEDIUM, Voltage.HIGH,
            Voltage.EXTREME, Voltage.INSANE, Voltage.LUDICROUS, Voltage.ZERO_POINT_MODULE,
            Voltage.ULTIMATE, Voltage.ULTRA_HIGH, Voltage.ULTRA_EXCESSIVE, Voltage.ULTRA_IMMENSE,
            Voltage.ULTRA_MASSIVE);

    @Test
    void theTableHoldsALineOfEveryMaterialTheIndustryDraws() {
        assertEquals(45, CableMaterials.all().size(), "the cables of the table of the industry");
        Set<String> names = new HashSet<>();
        Set<String> words = new HashSet<>();
        for (CableMaterial material : CableMaterials.all()) {
            assertTrue(names.add(material.name()), "two cables share the name " + material.name());
            assertTrue(words.add(material.displayName()),
                    "two cables read as " + material.displayName());
            assertTrue(material.amperage() >= 1, material + " carries no current");
            assertTrue(material.loss() >= CableMaterial.NO_LOSS, material + " loses less than nothing");
            assertTrue(material.heat() >= CableMaterial.NO_HEAT, material + " has a heat below zero");
            assertSame(material, CableMaterials.byName(material.name()), "the name leads back to it");
            assertEquals(material.voltage().euPerTick() * material.amperage(), material.throughput());
        }
        assertNull(CableMaterials.byName("unobtainium"), "a material the table never heard of");
        assertNull(CableMaterials.byName(null));
    }

    @Test
    void theLadderOnlyEverClimbs() {
        List<CableMaterial> table = CableMaterials.all();
        for (int i = 1; i < table.size(); i++) {
            CableMaterial below = table.get(i - 1);
            CableMaterial above = table.get(i);
            assertTrue(above.voltage().isAtLeast(below.voltage()),
                    above + " stands below the line of " + below);
        }
        assertEquals(Voltage.ULTRA_LOW, table.get(0).voltage(), "the table starts in a workshop");
        assertEquals(Voltage.ULTRA_MASSIVE, table.get(table.size() - 1).voltage(),
                "and ends at the top of the ladder");
    }

    @Test
    void everyAgeEndsWithASuperconductor() {
        List<CableMaterial> superconductors = new ArrayList<>();
        for (CableMaterial material : CableMaterials.all()) {
            if (material.isSuperconductor()) {
                superconductors.add(material);
                assertEquals(CableMaterial.NO_LOSS, material.loss());
            }
        }
        assertEquals(AGES_WITH_A_SUPERCONDUCTOR.size(), superconductors.size(),
                "one superconductor for every age that brings one");
        for (int i = 0; i < superconductors.size(); i++) {
            assertEquals(AGES_WITH_A_SUPERCONDUCTOR.get(i), superconductors.get(i).voltage(),
                    "the superconductor of " + AGES_WITH_A_SUPERCONDUCTOR.get(i));
        }
        for (CableMaterial material : superconductors) {
            assertTrue(material.name().endsWith("_superconductor"),
                    material + " is a superconductor and has to be named like one");
        }
    }

    @Test
    void theTwoRedLinesAreTwoMaterials() {
        assertNotSame(CableMaterials.RED_ALLOY, CableMaterials.REDSTONE_ALLOY);
        assertEquals(Voltage.ULTRA_LOW, CableMaterials.RED_ALLOY.voltage());
        assertEquals(Voltage.LOW, CableMaterials.REDSTONE_ALLOY.voltage());
        assertFalse(CableMaterials.RED_ALLOY.isSuperconductor(), "the red alloy loses on the way");
        assertEquals(1, CableMaterials.RED_ALLOY.loss());
        assertFalse(CableMaterials.REDSTONE_ALLOY.isSuperconductor(),
                "the cheap line of the age of steam is no superconductor of the game");
        assertEquals(2, CableMaterials.REDSTONE_ALLOY.loss());
    }

    @Test
    void theNumbersOfTheTableAreTheNumbersOfTheIndustry() {
        assertEquals(Voltage.ULTRA_LOW, CableMaterials.RED_ALLOY.voltage());
        assertEquals(1, CableMaterials.RED_ALLOY.amperage());
        assertEquals(8, CableMaterials.RED_ALLOY.throughput(), "one ampere of eight units a tick");

        assertEquals(Voltage.LOW, CableMaterials.LEAD.voltage());
        assertEquals(2, CableMaterials.LEAD.amperage());
        assertEquals(4, CableMaterials.LEAD.loss(), "the lead of the first line of a boiler");
        assertEquals(2, CableMaterials.LEAD.heat());

        assertEquals(Voltage.MEDIUM, CableMaterials.COPPER.voltage());
        assertEquals(1, CableMaterials.COPPER.amperage());
        assertEquals(4, CableMaterials.COPPER.loss());
        assertEquals(Voltage.MEDIUM, CableMaterials.ANNEALED_COPPER.voltage());
        assertEquals(2, CableMaterials.ANNEALED_COPPER.loss(), "softened copper loses less");
        assertEquals(4, CableMaterials.CUPRONICKEL.amperage(), "four amperes of the middle voltage");
        assertEquals(6, CableMaterials.CUPRONICKEL.loss());

        assertEquals(Voltage.HIGH, CableMaterials.SILVER.voltage());
        assertEquals(Voltage.HIGH, CableMaterials.KANTHAL.voltage());
        assertEquals(5, CableMaterials.KANTHAL.amperage());

        assertEquals(Voltage.EXTREME, CableMaterials.STEEL.voltage());
        assertEquals(8, CableMaterials.NICHROME.loss(), "the line of a heating element loses most");
        assertEquals(4, CableMaterials.NICHROME.heat(), "and takes the most heat");

        assertEquals(Voltage.INSANE, CableMaterials.GRAPHENE.voltage());
        assertEquals(1, CableMaterials.GRAPHENE.amperage());
        assertEquals(2, CableMaterials.GRAPHENE.loss());

        assertEquals(Voltage.LUDICROUS, CableMaterials.OSMIUM.voltage());
        assertEquals(Voltage.ZERO_POINT_MODULE, CableMaterials.OSMIRIDIUM.voltage());
        assertEquals(16, CableMaterials.OSMIRIDIUM.amperage());
        assertEquals(2, CableMaterials.OSMIRIDIUM.loss(), "the highest current at the lowest loss");

        assertEquals(Voltage.ULTRA_HIGH, CableMaterials.HSS_S.voltage());
        assertEquals(8, CableMaterials.HSS_S.amperage());
        assertTrue(CableMaterials.HSS_S.hasHeat());

        assertEquals(64, CableMaterials.UMV_SUPERCONDUCTOR.amperage());
        assertEquals(0, CableMaterials.UMV_SUPERCONDUCTOR.loss());
        assertFalse(CableMaterials.UMV_SUPERCONDUCTOR.hasHeat(),
                "the table names no heat for a superconductor");
    }

    /**
     * Every cable of the table is a material the game holds.
     * <p>
     * A cable is a block of a material, so the table and the materials have to name one another: a row no
     * material answers to would be a cable nobody can build, and two names for one line would be a cable
     * that reads differently in its tooltip than in the hand of a player.
     */
    @Test
    void everyCableIsAMaterialOfTheGame() {
        TestRegistries.ensure();
        for (CableMaterial material : CableMaterials.all()) {
            Material game = MaterialRegistry.byName(material.name());
            assertNotNull(game, material + " is no material of the game");
            assertEquals(material.displayName(), game.displayName(),
                    material + " is named differently by Materials");
            assertEquals(material.color(), game.color(), material + " is painted differently");
        }
    }
}
