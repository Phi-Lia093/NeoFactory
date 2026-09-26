package com.philia093.neofactory.block;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.model.BlockModel;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.block.state.BlockStateRegistry;
import com.philia093.neofactory.block.state.BlockStateTable;
import com.philia093.neofactory.item.ToolType;
import com.philia093.neofactory.util.Aabb;

import java.util.Objects;

/**
 * Immutable description of a single block type.
 * <p>
 * A block owns exactly one texture name because the game looks at the world from
 * above: every block is drawn as a single, whole tile. The name is relative to
 * the {@code blocks/} asset folder and must not include the {@code .png}
 * extension.
 */
public final class Block {

    /** Texture name placeholder used by blocks that are never drawn. */
    public static final String NO_TEXTURE = "";

    /** Id of the empty block, see {@code Blocks.AIR_ID}. */
    public static final int AIR_ID = 0;

    private final int id;
    private final String name;
    private final String texture;
    private final boolean solid;
    private final boolean ground;
    private final boolean transparent;
    private final boolean climbable;
    private final boolean hangsOnASide;
    private final Color tint;
    private final float hardness;
    private final int harvestLevel;
    private final ToolType toolType;
    private final String blockEntityTypeName;
    private final boolean carriesFluid;
    private final Animation animation;
    private final int itemState;

    private Block(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.texture = builder.texture;
        this.solid = builder.solid;
        this.ground = builder.ground;
        this.transparent = builder.transparent;
        this.climbable = builder.climbable;
        this.hangsOnASide = builder.hangsOnASide;
        this.tint = builder.tint;
        this.hardness = builder.hardness;
        this.harvestLevel = builder.harvestLevel;
        this.toolType = builder.toolType;
        this.blockEntityTypeName = builder.blockEntityTypeName;
        this.carriesFluid = builder.carriesFluid;
        this.animation = builder.animation;
        this.itemState = builder.itemState;
    }

    /** Unique numeric id, also used as the palette index inside chunks. */
    public int id() {
        return id;
    }

    /** Human readable identifier, used for logging and debugging. */
    public String name() {
        return name;
    }

    /**
     * Texture name of this block, seen from above.
     * <p>
     * Returns {@link #NO_TEXTURE} for blocks that are never drawn, such as air.
     */
    public String texture() {
        return texture;
    }

    /**
     * {@code true} when the block stops player movement.
     * <p>
     * The flag describes a block that stands in a cell: a wall of planks or a tree
     * trunk is solid, tall grass and water are not. A hard material such as stone is
     * solid as well, so a block the player built is always an obstacle. Whether the
     * same block may be walked over as the ground of a cell is a separate question,
     * see {@link #isGround()}.
     */
    public boolean isSolid() {
        return solid;
    }

    /**
     * {@code true} when the block can be the ground of a cell.
     * <p>
     * A ground surface fills the floor layer - grass, sand, stone, the ores -
     * and the player walks over it, so it never stops movement no matter how hard
     * the material is. Air counts as ground as well, which is what keeps a dug out
     * hole passable.
     * <p>
     * Everything else, bedrock and every object such as a trunk or a wall, is not
     * ground: it blocks the cell as soon as it is either the layer the player stands
     * in or the ground below it, see {@code BlockAccess.isSolid(int, int)}.
     */
    public boolean isGround() {
        return ground;
    }


    /** {@code true} when blocks below this one stay visible. */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * The state this block is drawn in while it stands for an item.
     * <p>
     * A block that is a cube looks the same in every state and answers {@code 0}. A block whose shape is the
     * state itself - a pipe, whose six properties are its six sides - would be drawn as a bare stub in state
     * zero, which is not what a player holds in mind: the state named here is the one whose model shows the
     * block as a piece of itself, and it is what a hand, a drop and the icon of a slot are meshed from, see
     * {@code ItemCubeMeshes} and {@code BlockIconRenderer}.
     *
     * @return the state the item of this block is drawn in
     */
    public int itemState() {
        return itemState;
    }

    /**
     * {@code true} when a body climbs on this block.
     * <p>
     * A ladder is the block of the game that is climbed: a body that stands in one of its cells holds on to
     * it, sinks slowly instead of falling and climbs while the player asks for it, see
     * {@link com.philia093.neofactory.entity.Player#update(com.philia093.neofactory.world.World, float)}.
     * Everything else is walked through and fallen through as usual.
     */
    public boolean isClimbable() {
        return climbable;
    }

    /**
     * {@code true} when this block hangs on the side of another one.
     * <p>
     * A ladder is built against a side and never onto the ground: the cell below a ladder carries it just as
     * little as it carries the ladder of a wall that is not there, so a build that only found a floor is
     * refused, see {@link com.philia093.neofactory.world.interaction.BlockPlacer}. The direction such a
     * block is turned is the side it hangs on as well, so its rungs look away from it.
     */
    public boolean hangsOnASide() {
        return hangsOnASide;
    }

    /** {@code true} when the block has no texture and is never drawn. */
    public boolean isAir() {
        return id == AIR_ID;
    }

    /** Color multiplied with the block texture while drawing. */
    public Color tint() {
        return tint;
    }

    /**
     * Time in seconds a player needs to break this block.
     * <p>
     * A negative value marks a block that can never be broken, which is how
     * bedrock and water are declared. The value is read by
     * {@link com.philia093.neofactory.world.interaction.MiningRule}, the rule that
     * is active right now breaks instantly and only respects that sign.
     */
    public float hardness() {
        return hardness;
    }

    /**
     * Mining level a tool needs to harvest this block.
     * <p>
     * Level {@code 0} is a bare hand or any tool. The value is compared with
     * {@link com.philia093.neofactory.item.Item#toolLevel()} of the held item by
     * {@link com.philia093.neofactory.world.interaction.HardnessMining}, which also asks for the
     * kind of tool the block names, see {@link #toolType()}.
     */
    public int harvestLevel() {
        return harvestLevel;
    }

    /**
     * Kind of tool this block is worked with, {@code null} for a block every tool mines alike.
     * <p>
     * The kind decides two things, both asked by
     * {@link com.philia093.neofactory.world.interaction.HardnessMining}. A tool of that kind mines
     * this block with its own speed, every other one only as fast as a bare hand; and a block that
     * asks for a mining level at all hands its item over only to the kind it names here, so a stone
     * of level {@code 1} wants a pickaxe and stays behind an axe.
     * <p>
     * A block that names no kind - soil, a sapling, a torch - is mined by hand and by every tool at
     * the same speed and hands its item to all of them.
     *
     * @return the kind of tool that fits this block, or {@code null} for a block every tool fits
     */
    public ToolType toolType() {
        return toolType;
    }

    /**
     * Name of the block entity this block owns, empty for a block without one.
     * <p>
     * A wall, a floor and every decorative block hold nothing but their own texture and
     * need no block entity. A machine does: the name is the one its type was registered
     * with, see {@link com.philia093.neofactory.blockentity.BlockEntityRegistry}, and it
     * is what puts the entity behind the block while the block is built and what brings
     * it back while its chunk is read.
     *
     * @return the name of the block entity type, empty when this block has none
     */
    public String blockEntityTypeName() {
        return blockEntityTypeName;
    }

    /** {@code true} when this block carries a block entity. */
    public boolean hasBlockEntity() {
        return !blockEntityTypeName.isEmpty();
    }

    /**
     * {@code true} when this block carries a tank of fluid, see {@link com.philia093.neofactory.fluid.FluidNode}.
     * <p>
     * A pipe joins what a block says about itself and not what the world happens to hold in a cell: a
     * machine names the fluid in its picture while it is built, which is how {@code Pipes#connects} knows
     * that a line may be built towards it without a world to ask.
     *
     * @return {@code true} when a pipe may reach this block
     */
    public boolean carriesFluid() {
        return carriesFluid;
    }

    /**
     * How this block is animated, {@code null} for a block that stands still.
     * <p>
     * The picture of an animated block is a sheet: {@code frames} cells of one tile, one
     * below the other, and the renderer shows the cell that belongs to the current tick,
     * see {@link com.philia093.neofactory.render.BlockAnimation}. The first user is the
     * water of a lake, the same ability serves a machine that moves while it works.
     * <p>
     * <b>The renderer reads the frames of a picture out of its file, not out of this record.</b> A
     * picture that is a strip of tiles takes one layer per frame of the texture array, and the
     * shader of the world walks that run with the tick of the world, which is what turns the gear
     * on the top of a machine while it works and keeps the casing around it still, see
     * {@link com.philia093.neofactory.render.BlockPictures#frameCountOf(int, int)} and
     * {@link com.philia093.neofactory.render.BlockShader#ANIMATION}. A block therefore moves as soon
     * as its picture is a strip; the length of a frame is the round of the world and no round of the
     * block's own.
     *
     * @return the animation, or {@code null} when the block has none
     */
    public Animation animation() {
        return animation;
    }

    /** {@code true} when this block is drawn from a sheet of more than one frame. */
    public boolean isAnimated() {
        return animation != null && animation.isAnimated();
    }

    /**
     * The model of this block, which is the shape it is drawn with.
     * <p>
     * The flat engine drew a block as one picture seen from above, which is what {@link #texture()}
     * still names. A world of cubes shows a shape: the grass its bright top over a side the colour of
     * its biome is painted through, the log its bark around rings, the furnace the mouth of its
     * front. The shape is read from {@code assets/models/block/<name>.json}, named after this block,
     * and a block without such a file is the whole cube of its picture, see
     * {@link com.philia093.neofactory.block.model.ModelRegistry#of(Block)}.
     *
     * @return the model, never {@code null}, the empty model for a block that is never drawn
     */
    public BlockModel model() {
        return ModelRegistry.of(this);
    }

    /**
     * The states this block may take.
     * <p>
     * A state is a number a cell carries beside the id of its block, see
     * {@link com.philia093.neofactory.world.Section#state(int, int, int)}: the direction a furnace
     * looks in, the shape a pipe is drawn with. The table is read from
     * {@code assets/blockstates/<name>.json} and knows what each number means; a block without such
     * a file carries {@link BlockStateTable#NONE}, which answers with the state every property at
     * its first value.
     *
     * @return the table, never {@code null}
     */
    public BlockStateTable states() {
        return BlockStateRegistry.of(this);
    }

    /**
     * What one state of this block shows.
     *
     * @param state number of the state, {@code 0} for a block that carries none
     * @return the model and the quarter turns it is drawn with, see
     *         {@link BlockStateRegistry#shown(Block, int)}
     */
    public BlockStateRegistry.Shown shown(int state) {
        return BlockStateRegistry.shown(this, state);
    }

    /**
     * The part of its cell a state of this block fills as an obstacle.
     * <p>
     * A block is not always a whole cube. A slab fills the lower or the upper half of its cell, an anvil a
     * body of its own, so what a body runs into is the shape of the model the state is
     * drawn with, see {@link BlockModel#shape()}, turned the way the state turns that model. The box is
     * written in the coordinates of one cell - {@code (0, 0, 0)} is one corner of the block and
     * {@code (1, 1, 1)} the other one; a caller that asks about a place in the world moves it there, see
     * {@link com.philia093.neofactory.world.BlockAccess#shape(int, int, int,
     * com.philia093.neofactory.util.Aabb)}.
     * <p>
     * <b>Whether the cell is walked through at all is a separate question.</b> Only a
     * {@link #isSolid() solid} block holds anything back; a plant, a torch, a ladder and the air are
     * entered by a body whatever shape they are drawn with.
     *
     * @param state number of the state, {@code 0} for a block that carries none
     * @param into box to write the shape into, because the box of a model is shared by every cell that
     *             shows it and must not be written to
     * @return the box, empty when the model of that state draws nothing
     */
    public Aabb shape(int state, Aabb into) {
        BlockStateRegistry.Shown shown = shown(state);
        into.set(shown.model().shape());
        return turn(shown.rotateY(), into);
    }

    /**
     * Turns a box the way a state turns the model it belongs to.
     * <p>
     * The same quarter turn the mesher applies to the corners it draws: around the middle of the cell,
     * so a model that faces north faces east at ninety degrees, see
     * {@link com.philia093.neofactory.render.SectionMesher}. A turn of a quarter is what maps one
     * axis-aligned box onto another one, which is why turning the shape of a model is exact.
     *
     * @param rotateY quarter turns, a multiple of ninety degrees
     * @param box box to write the turned shape into, in the coordinates of one cell
     * @return the box
     */
    private static Aabb turn(int rotateY, Aabb box) {
        switch (rotateY) {
            case 90:
                return box.set(box.minZ(), box.minY(), 1.0f - box.maxX(), box.maxZ(), box.maxY(),
                        1.0f - box.minX());
            case 180:
                return box.set(1.0f - box.maxX(), box.minY(), 1.0f - box.maxZ(), 1.0f - box.minX(),
                        box.maxY(), 1.0f - box.minZ());
            case 270:
                return box.set(1.0f - box.maxZ(), box.minY(), box.minX(), 1.0f - box.minZ(),
                        box.maxY(), box.maxX());
            default:
                return box;
        }
    }

    /** {@code true} when this block has a texture that can be drawn. */
    public boolean isDrawable() {
        return !texture.isEmpty() && !isAir();
    }

    /** Starts building a new block definition. */
    public static Builder builder(int id, String name) {
        return new Builder(id, name);
    }

    @Override
    public String toString() {
        return "Block(" + id + ", " + name + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Block)) {
            return false;
        }
        return id == ((Block) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * A sheet of frames that is played where the block stands.
     * <p>
     * The game holds one picture per block and draws it as a whole tile, so an animation is
     * nothing but a taller picture: {@code frames} cells of the usual tile size, stacked
     * upwards from the top of the image, and the renderer cuts the cell out that belongs to
     * the tick that is running.
     * <p>
     * The cell size is the cell size of the game, so a sheet of thirty two frames of water
     * is sixteen pixels wide and five hundred and twelve pixels tall.
     *
     * @param frames amount of cells in the sheet, at least one
     * @param frameTicks amount of ticks one cell is shown, at least one
     */
    public record Animation(int frames, int frameTicks) {

        /** Checks the numbers, so a broken sheet fails while the block is defined. */
        public Animation {
            if (frames < 1) {
                throw new IllegalArgumentException("An animation needs at least one frame: "
                        + frames);
            }
            if (frameTicks < 1) {
                throw new IllegalArgumentException("A frame lasts at least one tick: "
                        + frameTicks);
            }
        }

        /** {@code true} when the sheet holds more than a single frame. */
        public boolean isAnimated() {
            return frames > 1;
        }

        /**
         * Amount of ticks a whole round through the sheet takes.
         *
         * @return {@code frames * frameTicks}, never zero
         */
        public int cycleTicks() {
            return frames * frameTicks;
        }
    }

    /** Fluent builder for {@link Block} instances. */
    public static final class Builder {

        private final int id;
        private final String name;
        private String texture = NO_TEXTURE;
        private boolean solid = true;
        private boolean ground = false;
        private boolean transparent = false;
        private boolean climbable = false;
        private boolean hangsOnASide = false;
        private Color tint = new Color(Color.WHITE);
        private float hardness = 1.0f;
        private int harvestLevel;
        private ToolType toolType;
        private String blockEntityTypeName = "";

        /** {@code true} for a block that carries a tank of fluid, see {@link Block#carriesFluid()}. */
        private boolean carriesFluid = false;
        private Animation animation;
        private int itemState;

        private Builder(int id, String name) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
        }

        /**
         * Sets the picture of this block.
         * <p>
         * The picture is drawn on every face of a block that names no model of its own, and it is
         * the picture a slot and a hand show, see
         * {@link com.philia093.neofactory.block.model.ModelRegistry#of(Block)}. The shape of a block
         * that is more than one picture is written down in {@code assets/models/block}, not here.
         *
         * @param texture name relative to {@code blocks/} without extension
         */
        public Builder texture(String texture) {
            this.texture = Objects.requireNonNull(texture, "texture");
            return this;
        }

        /**
         * Sets the state this block is drawn in while it stands for an item.
         * <p>
         * Only a block whose shape is its state needs this, see {@link Block#itemState()}: a pipe is drawn
         * as a straight length of itself in the hand, on the ground and in a slot.
         *
         * @param itemState state the item of this block is drawn in, {@code 0} for a block that is a cube
         */
        public Builder itemState(int itemState) {
            this.itemState = itemState;
            return this;
        }

        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }

        /**
         * Marks the block as a ground surface the player walks over.
         * <p>
         * A block that is not ground blocks the cell it occupies, see
         * {@link Block#isGround()}.
         *
         * @param ground {@code true} for a block that fills the floor layer
         */
        public Builder ground(boolean ground) {
            this.ground = ground;
            return this;
        }

        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }

        /**
         * Marks the block as one a body climbs on.
         *
         * @param climbable {@code true} for a ladder
         */
        public Builder climbable(boolean climbable) {
            this.climbable = climbable;
            return this;
        }

        /**
         * Declares that this block hangs on the side of another one.
         *
         * @param hangsOnASide {@code true} for a ladder
         */
        public Builder hangsOnASide(boolean hangsOnASide) {
            this.hangsOnASide = hangsOnASide;
            return this;
        }

        public Builder tint(Color tint) {
            this.tint = new Color(tint);
            return this;
        }

        public Builder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }

        /**
         * Sets the mining level a tool needs to harvest this block.
         *
         * @param harvestLevel required level, {@code 0} for hand and any tool
         */
        public Builder harvestLevel(int harvestLevel) {
            if (harvestLevel < 0) {
                throw new IllegalArgumentException("Harvest level must not be negative: " + harvestLevel);
            }
            this.harvestLevel = harvestLevel;
            return this;
        }

        /**
         * Sets the kind of tool this block is worked with.
         * <p>
         * A block that names a kind is mined with the speed of that tool and with the speed of a
         * bare hand by every other one; a block that asks for a mining level hands its item over
         * only to the kind it names, see {@link Block#toolType()} and
         * {@link com.philia093.neofactory.world.interaction.HardnessMining}. A block that names no
         * kind - soil, a sapling - is worked by hand and by every tool alike.
         *
         * @param toolType kind of tool that fits this block, for example {@link ToolType#PICKAXE}
         */
        public Builder toolType(ToolType toolType) {
            this.toolType = Objects.requireNonNull(toolType, "toolType");
            return this;
        }

        /**
         * Declares that this block owns a block entity.
         * <p>
         * The name is looked up in {@link com.philia093.neofactory.blockentity.BlockEntityRegistry}
         * while the block is built and while a chunk is read, which is why it is a name
         * and not a type: the table of the block types is written first and knows nothing
         * about the systems that use it.
         *
         * @param typeName name the block entity type was registered with
         */
        public Builder blockEntity(String typeName) {
            this.blockEntityTypeName = Objects.requireNonNull(typeName, "typeName");
            return this;
        }

        /**
         * Names the fluid this block carries, so a pipe may be built towards it.
         * <p>
         * The flag is what {@code Pipes#connects} reads, and it is the block that says it: the machine
         * behind the block answers with a tank of its own on every side, see
         * {@link com.philia093.neofactory.fluid.FluidNode}.
         *
         * @return this builder, so the calls chain
         */
        public Builder carriesFluid() {
            this.carriesFluid = true;
            return this;
        }

        /**
         * Makes the picture of this block a sheet that is played in place.
         *
         * @param animation frames of the sheet and how long one is shown
         */
        public Builder animation(Animation animation) {
            this.animation = Objects.requireNonNull(animation, "animation");
            return this;
        }

        /**
         * Makes the picture of this block a sheet that is played in place.
         *
         * @param frames amount of cells in the sheet, at least one
         * @param frameTicks amount of ticks one cell is shown, at least one
         */
        public Builder animation(int frames, int frameTicks) {
            return animation(new Animation(frames, frameTicks));
        }

        public Block build() {
            return new Block(this);
        }
    }
}
