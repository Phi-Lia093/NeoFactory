package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Every fluid of the game, together with the block each of them stands in the world with.
 * <p>
 * A fluid and its block are two views of one thing, so they share the numbers that describe
 * the look of it: the colour, the sheet and the amount of frames are constants of this class,
 * {@link Blocks#registerAll()} builds the block from them and {@link #registerAll()} builds
 * the fluid from the very same values. Neither can be changed without the other.
 * <p>
 * <b>Grey scale:</b> the sheet of a fluid holds brightness only and never a colour, the colour
 * travels in {@link Fluid#color()} and is multiplied with the picture while it is drawn. All
 * fluids of the game share the one sheet of {@link #SHEET} and differ in that colour alone, so a
 * new fluid is a colour plus a line in {@link #registerAll()}: the world, a tank and the cell of
 * the fluid all receive their colour from the fluid and none of them knows its name. The sheet is
 * built by {@code build/verify/grayscale_fluid.ps1}, which keeps the brightest channel of every
 * pixel of a sheet of the art pack and drops its colour, and the colours below are that brightest
 * channel mapped back to the hue of the original art.
 */
public final class Fluids {

    /**
     * Colour the water of the game is painted in.
     * <p>
     * The sheet keeps the brightness of the original picture, this colour brings the hue
     * back: drawn as {@code sheet * colour} the water of the world looks the way it did
     * before the sheet lost its blue, and the same colour serves the cell of the fluid.
     */
    public static final Color WATER_COLOR = new Color(0.19f, 0.28f, 1.0f, 0.70f);

    /** Colour lava is painted in, the hue of the orange sheet it is cut from. */
    public static final Color LAVA_COLOR = new Color(1.0f, 0.38f, 0.06f, 1.0f);

    /**
     * The grey scale sheet every fluid is painted from, relative to the {@code blocks/} folder.
     * <p>
     * All fluids of the game share one sheet and differ in their colour alone: water is the blue
     * one, lava the orange one, and a fluid of the industry that arrives later is another colour
     * of the same picture. A fluid that wants a sheet of its own is built with one - the renderer
     * asks {@link Fluid#stillTexture()} and never this constant.
     * <p>
     * The name follows the rule of every block picture of the game: it is spelled without the
     * folder, which {@link com.philia093.neofactory.render.BlockTextureCache#resolvePath(String)}
     * adds, and without the extension.
     */
    public static final String SHEET = "generic_fluid";

    /**
     * Cells of that sheet.
     * <p>
     * The sheet was built from the still water of the art pack, see
     * {@code build/verify/grayscale_fluid.ps1}. {@code FluidTextureTest} reads the number back out
     * of the file, so a sheet that changes fails the build instead of running short.
     */
    public static final int FRAMES = 32;

    /** Ticks one cell of a fluid sheet is shown, the game runs twenty ticks a second. */
    public static final int FRAME_TICKS = 4;

    /** Cells water reaches away from its source before it stops. */
    private static final int WATER_RANGE = 7;

    /** Cells lava reaches away from its source, it is thicker than water. */
    private static final int LAVA_RANGE = 3;

    /** Ticks between two rings of water, so a spill is seen running. */
    private static final int WATER_TICK_INTERVAL = 5;

    /** Ticks between two rings of lava, it crawls. */
    private static final int LAVA_TICK_INTERVAL = 30;

    /** Still water, the fluid of every lake and ocean. */
    public static Fluid WATER;

    /** Lava, the fluid of the deep layers of the world. */
    public static Fluid LAVA;

    private static final Map<String, Fluid> BY_NAME = new LinkedHashMap<>();
    private static final Map<Block, Fluid> BY_BLOCK = new HashMap<>();

    private Fluids() {
        // Utility class: never instantiated.
    }

    /**
     * Creates the fluid of every block that belongs to one.
     * <p>
     * Called once during startup, after {@link Blocks#registerAll()}: the block of a fluid is
     * registered there, because the table of the blocks is the one place that holds every
     * block of the game, and the numbers a fluid and its block share - the colour, the sheet,
     * the frames - are constants of this class, so the two can never drift apart.
     * <p>
     * The fluid of a block is handed out to a tank, a bucket and the world, so this table has
     * to be written before the first of them is used. It touches no registry of its own and
     * may therefore be filled at any point after the blocks exist.
     */
    public static void registerAll() {
        if (!BY_NAME.isEmpty()) {
            return;
        }
        WATER = register("water", WATER_COLOR, WATER_RANGE, WATER_TICK_INTERVAL, true,
                Blocks.WATER);
        LAVA = register("lava", LAVA_COLOR, LAVA_RANGE, LAVA_TICK_INTERVAL, true, Blocks.LAVA);
    }

    /**
     * Keeps a fluid by name and by block.
     *
     * @param name name of the fluid, also the name of its block
     * @param color colour the fluid is painted in
     * @param range cells the fluid spreads away from its source
     * @param tickInterval ticks between two rings of the spread
     * @param bucketable {@code true} when a bucket may carry the fluid
     * @param block block the fluid stands in the world with
     * @return the fluid, ready to be used by an item or a machine
     */
    private static Fluid register(String name, Color color, int range, int tickInterval,
            boolean bucketable, Block block) {
        Objects.requireNonNull(block, "The block of " + name + " is not registered yet");
        Fluid fluid = new Fluid(name, color, SHEET, FRAMES, FRAME_TICKS, range, tickInterval,
                bucketable, block);
        BY_NAME.put(name, fluid);
        BY_BLOCK.put(block, fluid);
        return fluid;
    }

    /**
     * Looks a fluid up by its name.
     *
     * @param name name such as {@code "water"}
     * @return the fluid, or {@code null} when no fluid uses that name
     */
    public static Fluid byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    /**
     * Looks a fluid up by the block it stands in the world with.
     *
     * @param block block of a cell, may be {@code null}
     * @return the fluid of that block, or {@code null} when the block is no fluid
     */
    public static Fluid byBlock(Block block) {
        return block == null ? null : BY_BLOCK.get(block);
    }

    /**
     * {@code true} when a block belongs to a fluid.
     *
     * @param block block of a cell, may be {@code null}
     * @return {@code true} for water, lava and every later fluid
     */
    public static boolean isFluidBlock(Block block) {
        return BY_BLOCK.containsKey(block);
    }

    /** Every fluid the game knows, water first. */
    public static Collection<Fluid> all() {
        return BY_NAME.values();
    }
}
