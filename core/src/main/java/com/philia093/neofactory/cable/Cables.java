package com.philia093.neofactory.cable;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ToolType;
import com.philia093.neofactory.pipe.PipeTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The cables of the game: one block per material and size of the two tables, and what they connect to.
 * <p>
 * A cable is the line of the age of power and it is built the way a pipe is, {@code Pipes}: the game holds
 * one block per {@link CableMaterial material} and per {@link CableSize size} the tables name together -
 * forty five of them today, one for every line of the table - and every one of those blocks carries the
 * same six properties, one per direction, which is the whole of what a cable decides: <b>which of its six
 * sides it joins.</b>
 * <p>
 * <b>The state of a cable is its mask.</b> The six properties are declared in the order {@code north},
 * {@code east}, {@code south}, {@code west}, {@code up}, {@code down}, so the number of a state is the
 * connection mask itself - the bit of the first property counts thirty two and the one of the last counts
 * one, see {@link #stateOf(int)} - and a lookup of "is this cable joined to its north" is a single
 * {@code and} instead of a map of names.
 * <p>
 * <b>Who looks after the mask.</b> Nobody but the player, the same way a pipe is looked after: a cable is
 * placed with no side joined, a wrench opens a side and a wrench closes it again, see
 * {@link #toggled(int, BlockFace)}. A line is therefore joined where it was built and nowhere else, so two
 * lines that meet at a wall stay apart and a line that runs past a machine does not feed it by accident.
 * <p>
 * <b>The art of a cable is the art of a thin tube.</b> The pack draws the tubes of the fluid system in one
 * grey scale family and every metal is painted with its own colour, see {@code PipeTexture}: a cable is
 * drawn from the <b>thinnest</b> of those tubes until a family of its own arrives, so a line of copper
 * reads as a thin copper tube and no picture has to be drawn for a cable yet. The models of a cable are
 * therefore the models of the tiny metal pipe, see {@link #modelName(int)}, and the blockstates of the
 * cables name them.
 * <p>
 * <b>Ids.</b> The blocks take the numbers {@value #FIRST_BLOCK_ID} to
 * {@value #FIRST_BLOCK_ID}+{@value #COUNT}-1 and the items {@value #FIRST_ITEM_ID} to
 * {@value #FIRST_ITEM_ID}+{@value #COUNT}-1, in the order the materials and the sizes are declared. The
 * items of the materials of the game stand behind that run, so {@code Items#NEXT_FREE_ID} moved by the
 * amount of the cables and a save game of an older version is refused, see {@code SaveFormat} and
 * {@code Items#CABLE_ID_FROM}.
 */
public final class Cables {

    /** Id of the first cable block, in the order of {@link CableMaterials#all()}. */
    public static final int FIRST_BLOCK_ID = 188;

    /** Id of the first cable item, in the order of the tables of the cables. */
    public static final int FIRST_ITEM_ID = 304;

    /** Amount of cables: one per material, size and kind the tables name together. */
    public static final int COUNT = countOfCables();

    /** Seconds of work a cable takes to break. */
    public static final float HARDNESS = 0.4f;

    /**
     * Directions a cable may join, in the order of the properties of its block state.
     * <p>
     * The order is what the bits of a mask weigh: the first direction counts thirty two and the last one
     * counts one, so the mask of a cable reads like the six properties of its state written as a binary
     * number.
     */
    public static final List<BlockFace> DIRECTIONS = List.of(BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST, BlockFace.TOP, BlockFace.BOTTOM);

    /** Mask of every direction, the state of a cable that joins on all six sides. */
    public static final int ALL_MASK = (1 << 6) - 1;

    /**
     * Connections the picture of a cable item shows.
     * <p>
     * The item of a cable is drawn from the model of a straight length of it, the shape a player has in
     * mind while holding one. The mask is the one of a run along the north-south axis.
     */
    public static final int STRAIGHT_MASK = mask(BlockFace.NORTH, BlockFace.SOUTH);

    private static final List<Cable> CABLES = new ArrayList<>();

    private static boolean blocksRegistered;
    private static boolean itemsRegistered;

    private Cables() {
        // Utility class: never instantiated.
    }

    /** Counts the cables the tables name together: a material, a size and a kind of every one. */
    private static int countOfCables() {
        return CableMaterials.all().size() * CableSize.values().length * CableKind.values().length;
    }

    /**
     * Mask that joins the given directions.
     *
     * @param faces directions to join
     * @return the mask
     */
    public static int mask(BlockFace... faces) {
        int mask = 0;
        for (BlockFace face : faces) {
            mask |= bit(face);
        }
        return mask;
    }

    /**
     * Bit a direction weighs inside the mask of a cable.
     *
     * @param face direction to ask for
     * @return the bit, {@code 32} for north and {@code 1} for down
     * @throws IllegalArgumentException when the direction is no side of a cable
     */
    public static int bit(BlockFace face) {
        int index = DIRECTIONS.indexOf(face);
        if (index < 0) {
            throw new IllegalArgumentException("A cable has no side towards " + face);
        }
        return 1 << (DIRECTIONS.size() - 1 - index);
    }

    /**
     * Number of the state a mask is stored as.
     *
     * @param mask connections of a cable
     * @return the number of its state
     */
    public static int stateOf(int mask) {
        return mask & ALL_MASK;
    }

    /**
     * Connections a state stands for.
     *
     * @param state number of a state
     * @return the mask
     */
    public static int maskOf(int state) {
        return state & ALL_MASK;
    }

    /**
     * The state a cable has after one of its sides was turned.
     *
     * @param state state of the cable before the turn
     * @param face side of the block the wrench was put to
     * @return the number of the state after the turn
     */
    public static int toggled(int state, BlockFace face) {
        return stateOf(maskOf(state) ^ bit(face));
    }

    /**
     * The state a cable has after one of its sides was joined.
     *
     * @param state state of the cable before the join
     * @param face side that is joined
     * @return the number of the state after the join
     */
    public static int joined(int state, BlockFace face) {
        return stateOf(maskOf(state) | bit(face));
    }

    /**
     * {@code true} when a state joins a direction.
     *
     * @param state number of a state
     * @param face direction to ask about
     * @return {@code true} when the cable joins that side
     */
    public static boolean isConnected(int state, BlockFace face) {
        return (maskOf(state) & bit(face)) != 0;
    }

    /**
     * The mask of a state after it was turned about the vertical axis.
     * <p>
     * A turn is the one a state is drawn with, see {@code SectionMesher}: a model that faces north is
     * drawn facing east after one turn, so four turns come back to the mask they started at. The two
     * vertical sides are left where they are, because nothing about them turns.
     *
     * @param mask connections of a cable
     * @param turns amount of quarter turns, any whole number
     * @return the mask of the turned cable
     */
    public static int turned(int mask, int turns) {
        int turned = stateOf(mask);
        int steps = Math.floorMod(turns, 4);
        for (int i = 0; i < steps; i++) {
            int next = turned & (bit(BlockFace.TOP) | bit(BlockFace.BOTTOM));
            if ((turned & bit(BlockFace.NORTH)) != 0) {
                next |= bit(BlockFace.WEST);
            }
            if ((turned & bit(BlockFace.EAST)) != 0) {
                next |= bit(BlockFace.NORTH);
            }
            if ((turned & bit(BlockFace.SOUTH)) != 0) {
                next |= bit(BlockFace.EAST);
            }
            if ((turned & bit(BlockFace.WEST)) != 0) {
                next |= bit(BlockFace.SOUTH);
            }
            turned = next;
        }
        return turned;
    }

    /**
     * The turn of a state that the mask of the model is drawn with.
     *
     * @param mask connections of a cable
     * @return the smallest of the four turns that stands for the same line
     */
    public static int canonical(int mask) {
        int best = stateOf(mask);
        for (int i = 1; i < 4; i++) {
            best = Math.min(best, turned(mask, i));
        }
        return best;
    }

    /**
     * Quarter turns a state draws the canonical model of its mask with.
     *
     * @param mask connections of a cable
     * @return the amount of quarter turns, {@code 0} to {@code 3}
     */
    public static int turnsToDraw(int mask) {
        int wanted = stateOf(mask);
        for (int i = 0; i < 4; i++) {
            if (turned(canonical(mask), i) == wanted) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Name of the model a line of a kind, a width and a mask is drawn with.
     * <p>
     * A bare line is the tube of its width in the metal family of the pipes and a wrapped one is the skin of
     * its width, see {@link CableKind}: both name the canonical model of their mask and the quarter turn that
     * shows it, exactly like a pipe does.
     *
     * @param kind kind of the line
     * @param size width of the line
     * @param mask connections of the line
     * @return the name of the model, such as {@code pipe_metal_tiny_00} or {@code cable_insulation_1x_00}
     */
    public static String modelName(CableKind kind, CableSize size, int mask) {
        String family = kind == CableKind.WIRE
                ? PipeTexture.METAL.folder() + "_" + size.tube().fileName()
                : kind.folder() + "_" + size.fileName();
        return family + "_" + String.format(Locale.ROOT, "%02d", canonical(mask));
    }

    /**
     * One cable of the game: a material and a size, with the block and the item that carry it.
     */
    public static final class Cable {

        private final CableMaterial material;
        private final CableSize size;
        private final CableKind kind;
        private final Block block;
        private Item item;

        private Cable(CableMaterial material, CableSize size, CableKind kind, Block block) {
            this.material = material;
            this.size = size;
            this.kind = kind;
            this.block = block;
        }

        /** Material this cable is made of. */
        public CableMaterial material() {
            return material;
        }

        /** Size of this cable. */
        public CableSize size() {
            return size;
        }

        /** What this line is wrapped in, a bare line or one with a skin. */
        public CableKind kind() {
            return kind;
        }

        /** Highest voltage a line of this cable may carry, see {@link CableMaterial#voltage()}. */
        public Voltage voltage() {
            return material.voltage();
        }

        /** Current one cable of this size takes, the amperage of the table times the width. */
        public int amperage() {
            return material.amperage() * size.factor();
        }

        /**
         * Energy one cable takes away per block an energy travels.
         * <p>
         * The loss of the material, halved and rounded down when the line wears a skin, see
         * {@link CableKind#loss(CableMaterial)}.
         */
        public int loss() {
            return kind.loss(material);
        }

        /** Energy a line of one cable of this size carries a tick. */
        public int throughput() {
            return voltage().euPerTick() * amperage();
        }

        /** Block of this cable. */
        public Block block() {
            return block;
        }

        /**
         * Item that places this cable.
         *
         * @return the item
         * @throws IllegalStateException when the items are not registered yet, see {@link #registerItems()}
         */
        public Item item() {
            if (item == null) {
                throw new IllegalStateException("The items of the cables are registered after the blocks");
            }
            return item;
        }

        /** Name of the block, of the item and of the files, such as {@code copper_cable_1x}. */
        public String name() {
            return material.name() + "_" + kind.fileName() + "_" + size.fileName();
        }

        /** Name a player reads, such as {@code Copper 4x Cable}. */
        public String displayName() {
            return material.displayName() + " " + size.displayName() + " " + kind.displayName();
        }

        /** Name of the model this cable is drawn with while it is joined in one way. */
        public String modelName(int mask) {
            return Cables.modelName(kind, size, mask);
        }

        @Override
        public String toString() {
            return "Cable(" + name() + ")";
        }
    }

    /**
     * Builds and registers the block of every cable.
     * <p>
     * Called while the blocks of the game are registered, behind the pipes and in front of everything that
     * was built after them, which is what the block ids of the cables are measured against, see the class
     * comment.
     */
    public static void registerBlocks() {
        if (blocksRegistered) {
            return;
        }
        int id = FIRST_BLOCK_ID;
        for (CableMaterial material : CableMaterials.all()) {
            for (CableSize size : CableSize.values()) {
                for (CableKind kind : CableKind.values()) {
                    Block.Builder builder = Block.builder(id++,
                            material.name() + "_" + kind.fileName() + "_" + size.fileName())
                            // The picture of a cable is a fallback, exactly like the one of a pipe: a cable
                            // of the world is drawn from the model of its state. It is named all the same,
                            // because a block without a picture is never drawn at all.
                            .texture(kind.picture(size))
                            .solid(false)
                            // A cable fills no cell: its tube leaves the corners of the block empty, so a
                            // block that stands next to one keeps its faces. Without this the mesher would
                            // take the face of a neighbour away as soon as a cable touched it, because the
                            // question it asks is whether a block fills the whole cell, see SectionMesher.
                            .transparent(true)
                            .ground(true)
                            .itemState(STRAIGHT_MASK)
                            .hardness(HARDNESS)
                            // A cable is taken apart with the wrench, the tool the line of the industry is
                            // built with.
                            .toolType(ToolType.WRENCH)
                            .tint(material.color());
                    Block block = builder.build();
                    BlockRegistry.register(block);
                    CABLES.add(new Cable(material, size, kind, block));
                }
            }
        }
        blocksRegistered = true;
    }

    /**
     * Builds and registers the item that places every cable.
     * <p>
     * Called while the items of the game are registered, behind the pipes and in front of the items of the
     * materials, which is what the item ids of the cables are measured against, see the class comment.
     */
    public static void registerItems() {
        if (itemsRegistered) {
            return;
        }
        int id = FIRST_ITEM_ID;
        for (Cable cable : CABLES) {
            Item item = Item.builder(id++, cable.name())
                    .displayName(cable.displayName())
                    .buildBlock(cable.block());
            ItemRegistry.register(item);
            cable.item = item;
        }
        itemsRegistered = true;
    }

    /** Every cable of the game, in the order the materials and the sizes are declared. */
    public static List<Cable> all() {
        return List.copyOf(CABLES);
    }

    /**
     * The cable a block is.
     *
     * @param block block to ask about, may be {@code null}
     * @return the cable, or {@code null} for a block that is no cable
     */
    public static Cable of(Block block) {
        if (block == null) {
            return null;
        }
        int index = block.id() - FIRST_BLOCK_ID;
        if (index < 0 || index >= CABLES.size()) {
            return null;
        }
        Cable cable = CABLES.get(index);
        return cable.block() == block ? cable : null;
    }

    /**
     * The cable of a material, a size and a kind.
     *
     * @param material material of the cable
     * @param size size of the cable
     * @param kind what the line is wrapped in
     * @return the cable, or {@code null} when the game has none
     */
    public static Cable of(CableMaterial material, CableSize size, CableKind kind) {
        for (Cable cable : CABLES) {
            if (cable.material() == material && cable.size() == size && cable.kind() == kind) {
                return cable;
            }
        }
        return null;
    }

    /**
     * {@code true} when a cable joins the block that stands next to it.
     * <p>
     * Two cables always join, whatever their material: a line may run from a copper cable into a
     * superconductor, and which of the two limits the line is a question of the net and not of the shape.
     * <b>A machine joins as well, one day:</b> a block that carries a buffer of energy says so of itself
     * while it is built, the way a machine that carries a fluid does, and the net behind it is the step
     * after this one, see {@code EnergyStorage}.
     *
     * @param block block to ask about, may be {@code null}
     * @return {@code true} when a cable reaches it
     */
    public static boolean connects(Block block) {
        return of(block) != null;
    }
}
