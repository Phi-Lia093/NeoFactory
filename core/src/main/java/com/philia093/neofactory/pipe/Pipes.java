package com.philia093.neofactory.pipe;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The pipes of the game: four materials in seven sizes, and what they connect to.
 * <p>
 * A pipe is the simplest block of the industry and the one the most of it is built from: a tube that
 * carries a fluid from the tank of one machine to the tank of another. The game holds one block per
 * {@link PipeMaterial material} and {@link PipeSize size} - twenty eight of them, from the tiny wooden
 * pipe to the nonuple steel one - and every one of those blocks carries the same six properties, one per
 * direction, which is the whole of what a pipe decides: <b>which of its six sides it joins.</b>
 * <p>
 * <b>The state of a pipe is its mask.</b> The six properties are declared in the order {@code north},
 * {@code east}, {@code south}, {@code west}, {@code up}, {@code down}, so the number of a state is the
 * connection mask itself - the bit of the first property counts thirty two and the one of the last counts
 * one, see {@link #stateOf(int)} - and a lookup of "is this pipe joined to its north" is a single
 * {@code and} instead of a map of names. Sixty four states of a block are no more than the sixty four ways
 * a pipe can be joined, which is why the whole appearance of the fluid system is data and no code.
 * <p>
 * <b>Who looks after the mask.</b> Nobody but the player. A pipe is placed with no side joined and stays
 * that way: a wrench opens a side and a wrench closes it again, see {@link #toggled(int, BlockFace)} and
 * {@code PipeBlockEntity}. A line is therefore joined where it was built and nowhere else - two lines that
 * meet at a wall stay apart, and a pipe that runs past a machine does not take from it by accident. A side
 * that was joined stays joined when the block behind it is taken away, and what is left is the ending of
 * the tube with the plate of its size on it, which is a shape the models draw.
 * <p>
 * <b>A pipe joins a pipe, and one day a machine.</b> {@link #connects(Block)} is the whole rule: another
 * pipe today. It is the question of the transport - may a pipe take fluid from what stands next to it -
 * and not a question the wrench asks: a player may open any side, whatever it faces, see
 * {@code PipeBlockEntity#operateFace}. A machine joins on the faces that
 * carry a tank of fluid - the mouth of its front takes fluid in and every other face gives it - which
 * arrives with the transport of fluids, see {@code FluidNode}.
 * <p>
 * <b>Ids.</b> The blocks take the numbers {@value #FIRST_BLOCK_ID} to
 * {@value #FIRST_BLOCK_ID}+{@value #COUNT}-1 and the items {@value #FIRST_ITEM_ID} to
 * {@value #FIRST_ITEM_ID}+{@value #COUNT}-1, in the order the materials and the sizes are declared, so a
 * stored chunk and a stored inventory keep naming the right pipe. The items of the materials of the game
 * stand behind that run, which is why {@code Items#NEXT_FREE_ID} moved and why a save game of an older
 * version is refused, see {@code SaveFormat}.
 */
public final class Pipes {

    /** Id of the first pipe block, in the order of {@link PipeMaterials#all()}. */
    public static final int FIRST_BLOCK_ID = 29;

    /** Id of the first pipe item, in the order of {@link PipeMaterials#all()}. */
    public static final int FIRST_ITEM_ID = 100;

    /** Amount of pipes: one per material and size. */
    public static final int COUNT = PipeMaterials.all().size() * PipeSize.values().length;

    /** Seconds of work a pipe takes to break. */
    public static final float HARDNESS = 0.4f;

    /** Name of the block entity every pipe carries, see {@code BlockEntityTypes}. */
    public static final String BLOCK_ENTITY = "pipe";

    /**
     * Directions a pipe may join, in the order of the properties of its block state.
     * <p>
     * The order is what the bits of a mask weigh: the first direction counts thirty two and the last one
     * counts one, so the mask of a pipe reads like the six properties of its state written as a binary
     * number. {@link BlockFace} declares its own faces in the order of the mesh, which is a different
     * thing and not what a state is numbered by.
     */
    public static final List<BlockFace> DIRECTIONS = List.of(BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST, BlockFace.TOP, BlockFace.BOTTOM);

    /** Mask that joins the given directions. */
    public static int mask(BlockFace... faces) {
        int mask = 0;
        for (BlockFace face : faces) {
            mask |= bit(face);
        }
        return mask;
    }

    /**
     * Connections the picture of a pipe item shows.
     * <p>
     * The item of a pipe is drawn from the model of a straight length of it, which is the shape a player
     * has in mind while holding one, see {@code ModelRegistry}. The mask is the one of a run along the
     * north-south axis.
     */
    public static final int STRAIGHT_MASK = mask(BlockFace.NORTH, BlockFace.SOUTH);

    private static final List<Pipe> PIPES = new ArrayList<>();

    private static boolean blocksRegistered;
    private static boolean itemsRegistered;

    private Pipes() {
        // Utility class: never instantiated.
    }

    /**
     * Bit a direction weighs inside the mask of a pipe.
     *
     * @param face direction to ask for
     * @return the bit, {@code 32} for north and {@code 1} for down
     * @throws IllegalArgumentException when the direction is no side of a pipe
     */
    public static int bit(BlockFace face) {
        int index = DIRECTIONS.indexOf(face);
        if (index < 0) {
            throw new IllegalArgumentException("A pipe has no side towards " + face);
        }
        return 1 << (DIRECTIONS.size() - 1 - index);
    }

    /** Mask of every direction, the state of a pipe that joins on all six sides. */
    public static final int ALL_MASK = (1 << 6) - 1;

    /**
     * Number of the state a mask is stored as.
     * <p>
     * The two are the very same number, because the six directions are the six properties of the block and
     * they are declared in the order the bits of a mask are counted in: the first property counts the most,
     * see {@code BlockStateTable}. The pair of methods exists so that the coupling is written down in one
     * place instead of being scattered over the code.
     *
     * @param mask connections of a pipe
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

    /** {@code true} when a mask joins a direction. */
    public static boolean isConnected(int mask, BlockFace face) {
        return (mask & bit(face)) != 0;
    }

    /**
     * The state a pipe has after one of its sides was turned.
     * <p>
     * This is what the wrench does to a pipe: the side that was joined is let go of and the side that was
     * open is joined. The side is turned whatever stands next to the pipe, so a player may build an open
     * ending and may open a side that faces a wall - what a pipe may take fluid <i>from</i> is the question
     * {@link #connects(Block)} answers, and the transport asks it.
     *
     * @param state state of the pipe before the turn
     * @param face side of the pipe to turn
     * @return the number of the state after the turn
     */
    public static int toggled(int state, BlockFace face) {
        return stateOf(maskOf(state) ^ bit(face));
    }

    /**
     * The direction a quarter turn of the world carries a direction to.
     * <p>
     * The turn is the one a state is drawn with, see {@code SectionMesher}: a model that faces north is
     * seen facing east at ninety degrees, so the cycle runs north, west, south, east. The two vertical
     * directions stay where they are, which is why a mask has four turns and not twenty four.
     *
     * @param face direction to turn
     * @return the direction it lands on
     */
    public static BlockFace turned(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.NORTH;
            default:
                return face;
        }
    }

    /**
     * The mask a quarter turn carries a mask to.
     *
     * @param mask connections of a pipe
     * @param quarters quarter turns, the sign is ignored
     * @return the turned mask
     */
    public static int turned(int mask, int quarters) {
        int steps = ((quarters % 4) + 4) % 4;
        int turned = 0;
        for (BlockFace face : DIRECTIONS) {
            if (!isConnected(mask, face)) {
                continue;
            }
            BlockFace moved = face;
            for (int step = 0; step < steps; step++) {
                moved = turned(moved);
            }
            turned |= bit(moved);
        }
        return turned;
    }

    /**
     * The smallest of the four turns of a mask.
     * <p>
     * The models of a size are generated for these sixteen masks and not for all sixty four of them: a
     * turn of a pipe is the same pipe, so a state names a canonical model and the quarter turn that shows
     * it, see {@link #turnsToDraw(int)}. The generator and the blockstate files of the game are built on
     * this number, see {@code tools/gen_pipe_models.ps1}.
     *
     * @param mask connections of a pipe
     * @return the smallest mask of its four turns
     */
    public static int canonical(int mask) {
        int best = stateOf(mask);
        for (int quarters = 1; quarters < 4; quarters++) {
            best = Math.min(best, turned(mask, quarters));
        }
        return best;
    }

    /**
     * Quarter turns a state draws the canonical model of its mask with.
     *
     * @param mask connections of a pipe
     * @return {@code 0} to {@code 3} quarter turns
     */
    public static int turnsToDraw(int mask) {
        for (int quarters = 0; quarters < 4; quarters++) {
            if (turned(mask, quarters) == canonical(mask)) {
                // The model stands for the turned mask, so it is drawn with the opposite turn to show the
                // mask itself.
                return (4 - quarters) % 4;
            }
        }
        return 0;
    }

    /**
     * Name of the model one family of art draws one size with, joined in one way.
     * <p>
     * The name carries the canonical mask, so a caller can ask for any of the sixty four ways a pipe may
     * be joined and is handed the model that really exists, see {@link #canonical(int)}.
     *
     * @param texture family of art
     * @param size size of the pipe
     * @param mask connections of the pipe
     * @return the name of the model, relative to {@code models/block}
     */
    public static String modelName(PipeTexture texture, PipeSize size, int mask) {
        return "pipe_" + texture.name().toLowerCase(Locale.ROOT) + "_" + size.fileName() + "_"
                + String.format(Locale.ROOT, "%02d", canonical(mask));
    }

    /**
     * One pipe of the game: a material and a size, with the block and the item that carry it.
     */
    public static final class Pipe {

        private final PipeMaterial material;
        private final PipeSize size;
        private final Block block;
        private Item item;

        private Pipe(PipeMaterial material, PipeSize size, Block block) {
            this.material = material;
            this.size = size;
            this.block = block;
        }

        /** Material this pipe is made of. */
        public PipeMaterial material() {
            return material;
        }

        /** Size of this pipe. */
        public PipeSize size() {
            return size;
        }

        /** Block of this pipe. */
        public Block block() {
            return block;
        }

        /**
         * Item that places this pipe.
         *
         * @return the item
         * @throws IllegalStateException when the items are not registered yet, see {@link #registerItems()}
         */
        public Item item() {
            if (item == null) {
                throw new IllegalStateException("The items of the pipes are registered after the blocks");
            }
            return item;
        }

        /** Name of the block, of the item and of the files of this pipe, such as {@code bronze_pipe_medium}. */
        public String name() {
            return material.name() + "_pipe_" + size.fileName();
        }

        /** Name a player reads, such as {@code Bronze Medium Pipe}. */
        public String displayName() {
            return material.displayName() + " " + size.displayName() + " Pipe";
        }

        /** Name of the model this pipe is drawn with while it is joined in one way. */
        public String modelName(int mask) {
            return Pipes.modelName(material.texture(), size, mask);
        }

        @Override
        public String toString() {
            return "Pipe(" + name() + ")";
        }
    }

    /**
     * Builds and registers the block of every pipe.
     * <p>
     * Called while the blocks of the game are registered, so the pipes exist before the items that place
     * them do, see {@link #registerItems()}.
     */
    public static void registerBlocks() {
        if (blocksRegistered) {
            return;
        }
        int id = FIRST_BLOCK_ID;
        for (PipeMaterial material : PipeMaterials.all()) {
            for (PipeSize size : PipeSize.values()) {
                Block.Builder builder = Block.builder(id++, material.name() + "_pipe_" + size.fileName())
                        // The picture of a pipe is a fallback: a pipe of the world is drawn from the model of
                        // its state, see Pipes. It is named all the same, because a block without a picture is
                        // never drawn at all.
                        .texture(material.texture().end(size))
                        .solid(false)
                        .ground(true)
                        // A pipe is drawn as a straight length of itself wherever it stands for an item: in
                        // the hand, on the ground and in a slot. A slot would otherwise mesh the stub of
                        // state zero, which is a bare piece of tube, and the icon of the size would say
                        // nothing about the size, see Block#itemState and BlockIconRenderer.
                        .itemState(STRAIGHT_MASK)
                        .hardness(HARDNESS)
                        .blockEntity(BLOCK_ENTITY);
                if (material.texture().isTinted()) {
                    builder.tint(material.color());
                }
                Block block = builder.build();
                BlockRegistry.register(block);
                PIPES.add(new Pipe(material, size, block));
            }
        }
        blocksRegistered = true;
    }

    /**
     * Builds and registers the item that places every pipe.
     * <p>
     * Called while the items of the game are registered, behind the machines and in front of the items of
     * the materials, which is what the item ids of the pipes are measured against, see the class comment.
     */
    public static void registerItems() {
        if (itemsRegistered) {
            return;
        }
        int id = FIRST_ITEM_ID;
        for (Pipe pipe : PIPES) {
            Item item = Item.builder(id++, pipe.name())
                    .displayName(pipe.displayName())
                    .buildBlock(pipe.block());
            ItemRegistry.register(item);
            pipe.item = item;
        }
        itemsRegistered = true;
    }

    /** Every pipe of the game, in the order the materials and the sizes are declared. */
    public static List<Pipe> all() {
        return List.copyOf(PIPES);
    }

    /**
     * The pipe a block is.
     *
     * @param block block to ask about, may be {@code null}
     * @return the pipe, or {@code null} for a block that is no pipe
     */
    public static Pipe of(Block block) {
        if (block == null) {
            return null;
        }
        int index = block.id() - FIRST_BLOCK_ID;
        if (index < 0 || index >= PIPES.size()) {
            return null;
        }
        Pipe pipe = PIPES.get(index);
        return pipe.block() == block ? pipe : null;
    }

    /**
     * The pipe of a material and a size.
     *
     * @param material material of the pipe
     * @param size size of the pipe
     * @return the pipe, or {@code null} when the game has none
     */
    public static Pipe of(PipeMaterial material, PipeSize size) {
        int index = PipeMaterials.all().indexOf(material) * PipeSize.values().length + size.ordinal();
        return index < 0 || index >= PIPES.size() ? null : PIPES.get(index);
    }

    /**
     * {@code true} when a pipe joins the block that stands next to it.
     * <p>
     * Two pipes always join, whatever their material and their size: a line may run from a wooden pipe into
     * a steel one, and which of them limits the flow is a question of the transport and not of the shape. A
     * machine will join on the faces that carry a tank of fluid as well - the mouth of its front takes
     * fluid in and every other face gives it - which arrives with the transport, see {@code FluidNode}.
     * Until then only pipes join.
     * <p>
     * <b>This is the question of the transport</b> and not one a wrench asks: the wrench turns any side a
     * player puts it to, see {@code PipeBlockEntity#operateFace}. A test pins the rule down so that the
     * transport finds it as it was written, see {@code PipeConnectionTest}.
     *
     * @param block block to ask about, may be {@code null}
     * @return {@code true} when a pipe reaches it
     */
    public static boolean connects(Block block) {
        return of(block) != null;
    }
}
