package com.philia093.neofactory.block.state;

import com.philia093.neofactory.block.state.BlockStateTable.Variant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a blockstate file makes of a state, without opening a window.
 * <p>
 * A cell of a stored world carries the number of a state and nothing else, so the only thing that
 * matters here is that a number means the same thing while a block is written, while it is read and
 * while it is drawn: the values of the properties are counted in the order they are written down, and
 * the state every property at its first value is the zero of the table.
 */
class BlockStateTest {

    /** The furnace of the game, the one block that looks in a direction. */
    private static final String FURNACE = """
            { "properties": { "facing": ["north", "east", "south", "west"] },
              "variants": { "facing=north": { "model": "furnace", "y": 0 },
                            "facing=east": { "model": "furnace", "y": 270 },
                            "facing=south": { "model": "furnace", "y": 180 },
                            "facing=west": { "model": "furnace", "y": 90 } } }
            """;

    @Test
    void theValuesOfAPropertyAreCountedInTheOrderTheyAreWrittenDown() {
        BlockStateTable table = BlockStateLoader.parse("furnace", FURNACE);

        assertEquals(4, table.stateCount());
        assertEquals(0, table.stateOf(Map.of("facing", "north")), "the first value is the zero");
        assertEquals(1, table.stateOf(Map.of("facing", "east")));
        assertEquals(2, table.stateOf(Map.of("facing", "south")));
        assertEquals(3, table.stateOf(Map.of("facing", "west")));
    }

    @Test
    void aStateIsTurnedBackIntoItsValues() {
        BlockStateTable table = BlockStateLoader.parse("furnace", FURNACE);

        for (int state = 0; state < table.stateCount(); state++) {
            Map<String, String> values = table.decode(state);
            assertEquals(state, table.stateOf(values), "state " + state + " reads back as itself");
        }
        assertEquals("east", table.decode(1).get("facing"));
    }

    @Test
    void aVariantTurnsTheModelItNames() {
        BlockStateTable table = BlockStateLoader.parse("furnace", FURNACE);

        assertEquals(new Variant("furnace", 0), table.variant(0));
        assertEquals(new Variant("furnace", 270), table.variant(1),
                "a furnace that faces east is the model that faces north, turned by 270 degrees");
        assertEquals(new Variant("furnace", 180), table.variant(2));
        assertEquals(new Variant("furnace", 90), table.variant(3));
    }

    @Test
    void aBlockWithoutAPropertyHasOneState() {
        BlockStateTable table = BlockStateLoader.parse("stone", "{ \"model\": \"stone\" }");

        assertTrue(table.properties().isEmpty());
        assertEquals(1, table.stateCount());
        assertEquals(0, table.defaultState());
        assertEquals("stone", table.variant(0).model());
        assertFalse(table.variant(0).isTurned());
    }

    @Test
    void aKeyOfAStarCoversEveryState() {
        BlockStateTable table = BlockStateLoader.parse("stone", """
                { "properties": { "axis": ["x", "y", "z"] },
                  "variants": { "*": { "model": "log_oak" } } }
                """);

        assertEquals(3, table.stateCount());
        for (int state = 0; state < table.stateCount(); state++) {
            assertEquals("log_oak", table.variant(state).model(),
                    "the star covers every value of the property");
        }
    }

    @Test
    void aStateTheFileDoesNotNameUsesTheFirstOne() {
        BlockStateTable table = BlockStateLoader.parse("machine", """
                { "properties": { "shape": ["plain", "corner", "cross"] },
                  "variants": { "shape=corner": { "model": "machine_corner" } } }
                """);

        assertEquals("machine_corner", table.variant(1).model());
        assertEquals("machine_corner", table.variant(0).model(),
                "a shape the file does not name is drawn like the first one it does");
        assertEquals("machine_corner", table.variant(2).model());
    }

    @Test
    void aPropertyTheBlockDoesNotCarryIsRefused() {
        BlockStateTable table = BlockStateLoader.parse("furnace", FURNACE);

        assertThrows(IllegalArgumentException.class,
                () -> table.stateOf(Map.of("shape", "plain")));
        assertThrows(IllegalArgumentException.class,
                () -> table.stateOf(Map.of("facing", "up")));
        assertThrows(IllegalArgumentException.class, () -> BlockStateLoader.parse("broken", """
                { "properties": { "shape": [] }, "variants": { "": {} } }
                """));
    }

    @Test
    void aTurnIsKeptWithinAQuarterOfACircle() {
        assertEquals(90, new Variant("model", 450).rotateY(), "a whole turn is taken off");
        assertEquals(270, new Variant("model", -90).rotateY(), "and the other way round as well");
        assertThrows(IllegalArgumentException.class, () -> new Variant("model", 45));
    }

    @Test
    void twoPropertiesCountTheFirstOneMost() {
        BlockStateTable table = BlockStateLoader.parse("machine", """
                { "properties": { "facing": ["north", "east"], "lit": ["false", "true"] },
                  "variants": { "facing=north,lit=true": { "model": "machine_lit" },
                                "facing=east,lit=false": { "model": "machine_side" } } }
                """);

        assertEquals(4, table.stateCount(), "two values each make four states");
        assertEquals(0, table.stateOf(Map.of("facing", "north", "lit", "false")));
        assertEquals(1, table.stateOf(Map.of("facing", "north", "lit", "true")));
        assertEquals(2, table.stateOf(Map.of("facing", "east", "lit", "false")));
        assertEquals(3, table.stateOf(Map.of("facing", "east", "lit", "true")));
        assertEquals("machine_lit", table.variant(1).model());
        assertEquals("machine_side", table.variant(2).model());
        assertEquals(List.of("facing", "lit"), table.properties());
    }
}
