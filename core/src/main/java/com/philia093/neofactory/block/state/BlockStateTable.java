package com.philia093.neofactory.block.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The states a block may take, and the model each of them is drawn with.
 * <p>
 * A block is not always the same shape: a furnace looks in one of four directions and a machine will
 * one day be built in one of several shapes, so a cell carries a number beside the id of its block -
 * the state, see {@link com.philia093.neofactory.world.Section#state(int, int, int)}. Nothing in the
 * world knows what that number means; the table of the block does, and the file
 * {@code assets/blockstates/<block>.json} is where it is written down:
 *
 * <pre>
 * blockstates/furnace.json    { "properties": { "facing": ["north", "east", "south", "west"] },
 *                               "variants": { "facing=north": { "model": "furnace", "y": 0 },
 *                                             "facing=east":  { "model": "furnace", "y": 270 },
 *                                             "facing=south": { "model": "furnace", "y": 180 },
 *                                             "facing=west":  { "model": "furnace", "y": 90 } } }
 * blockstates/stone_slab.json { "properties": { "type": ["bottom", "top"] },
 *                               "variants": { "type=bottom": { "model": "stone_slab" },
 *                                             "type=top":    { "model": "stone_slab_top" } } }
 * </pre>
 *
 * <b>The number of a state is the order of its values.</b> The properties are written down in an
 * order, every one of them counts from zero, and the number of the state is the values read as a
 * number whose digits are the properties: the first property counts the most. The state a player
 * never wrote down - every property at its first value - is therefore the zero of the table, which
 * is the state a cell of a stored chunk carries when nothing ever set one.
 * <p>
 * <b>A variant may turn the model.</b> {@link Variant#rotateY()} is a quarter turn around the
 * vertical axis of the block, which is how one model of a furnace serves all four directions it may
 * look in.
 * <p>
 * <b>The model of a state is also the shape of the cell.</b> What a body runs into is the boxes of the
 * model that state is drawn with, so a slab is half a block high for the player as well, see
 * {@link com.philia093.neofactory.block.Block#shape(int, com.philia093.neofactory.util.Aabb)}.
 */
public final class BlockStateTable {

    /**
     * What one state of a block shows.
     *
     * @param model name of the model, {@code null} to show the model of the block itself
     * @param rotateY quarter turns the model is turned by around the vertical axis of the block
     */
    public record Variant(String model, int rotateY) {

        /** The variant of a block that is always drawn the same way. */
        public static final Variant NONE = new Variant(null, 0);

        /** Checks the turn, so a broken blockstate file fails while it is read. */
        public Variant {
            if (rotateY % 90 != 0) {
                throw new IllegalArgumentException("A model is turned by a quarter turn, not by "
                        + rotateY + " degrees");
            }
            rotateY = ((rotateY % 360) + 360) % 360;
        }

        /** {@code true} when this variant turns the model. */
        public boolean isTurned() {
            return rotateY != 0;
        }
    }

    /** A table of no states, the one a block without a blockstate file gets. */
    public static final BlockStateTable NONE =
            new BlockStateTable("", new LinkedHashMap<>(), new ArrayList<>(List.of(Variant.NONE)));

    private final String blockName;
    private final Map<String, List<String>> values;
    private final List<Variant> variants;

    /**
     * Creates a table.
     *
     * @param blockName name of the block this table belongs to
     * @param values values of every property, in the order they are written down
     * @param variants variant of every state, indexed by the number of the state
     */
    public BlockStateTable(String blockName, Map<String, List<String>> values,
            List<Variant> variants) {
        this.blockName = Objects.requireNonNull(blockName, "blockName");
        this.values = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> property : values.entrySet()) {
            if (property.getValue().isEmpty()) {
                throw new IllegalArgumentException("The property '" + property.getKey()
                        + "' of '" + blockName + "' names no value");
            }
            this.values.put(property.getKey(), List.copyOf(property.getValue()));
        }
        this.variants = List.copyOf(variants);
        if (this.variants.isEmpty()) {
            throw new IllegalArgumentException("The block '" + blockName + "' has no state");
        }
    }

    /** Name of the block this table belongs to. */
    public String blockName() {
        return blockName;
    }

    /** Names of the properties, in the order they were written down. */
    public List<String> properties() {
        return List.copyOf(values.keySet());
    }

    /** {@code true} when this block carries the given property. */
    public boolean hasProperty(String property) {
        return values.containsKey(property);
    }

    /**
     * Values of one property, in the order they were written down.
     *
     * @param property name of the property
     * @return the values, or {@code null} when the block carries no such property
     */
    public List<String> valuesOf(String property) {
        return values.get(property);
    }

    /** Amount of states this block may take. */
    public int stateCount() {
        return variants.size();
    }

    /**
     * The state every property at its first value, which is the one a cell carries by default.
     *
     * @return zero
     */
    public int defaultState() {
        return 0;
    }

    /**
     * The number of the state with the given values.
     *
     * @param state values by property name, a property that is left out counts zero
     * @return the number of the state
     * @throws IllegalArgumentException when a property is named that this block does not carry, or a
     *         value the property does not take
     */
    public int stateOf(Map<String, String> state) {
        for (String property : state.keySet()) {
            if (!values.containsKey(property)) {
                throw new IllegalArgumentException("The block '" + blockName + "' carries no "
                        + "property '" + property + "'");
            }
        }
        int number = 0;
        for (Map.Entry<String, List<String>> property : values.entrySet()) {
            String wanted = state.get(property.getKey());
            int index = wanted == null ? 0 : property.getValue().indexOf(wanted);
            if (index < 0) {
                throw new IllegalArgumentException("The property '" + property.getKey()
                        + "' of '" + blockName + "' does not take the value '" + wanted + "'");
            }
            number = number * property.getValue().size() + index;
        }
        return number;
    }

    /**
     * The values of a state.
     *
     * @param state number of the state
     * @return the values by property name, in the order the properties were written down
     */
    public Map<String, String> decode(int state) {
        List<String> names = properties();
        Map<String, String> decoded = new LinkedHashMap<>();
        int rest = Math.max(0, Math.min(state, stateCount() - 1));
        for (int index = 0; index < names.size(); index++) {
            List<String> propertyValues = values.get(names.get(index));
            int step = 1;
            for (int later = index + 1; later < names.size(); later++) {
                step *= values.get(names.get(later)).size();
            }
            decoded.put(names.get(index), propertyValues.get(rest / step % propertyValues.size()));
        }
        return decoded;
    }

    /**
     * What a state of this block shows.
     *
     * @param state number of the state, clamped into the range of the table
     * @return the variant, never {@code null}
     */
    public Variant variant(int state) {
        Variant variant = variants.get(Math.max(0, Math.min(state, variants.size() - 1)));
        return variant == null ? Variant.NONE : variant;
    }

    @Override
    public String toString() {
        return "BlockStateTable(" + blockName + ", " + values.keySet() + ", " + stateCount()
                + " states)";
    }
}
