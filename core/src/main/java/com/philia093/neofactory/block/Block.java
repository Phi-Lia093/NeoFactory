package com.philia093.neofactory.block;

import com.badlogic.gdx.graphics.Color;

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
    private final boolean liquid;
    private final boolean transparent;
    private final Color tint;
    private final float hardness;
    private final int harvestLevel;

    private Block(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.texture = builder.texture;
        this.solid = builder.solid;
        this.ground = builder.ground;
        this.liquid = builder.liquid;
        this.transparent = builder.transparent;
        this.tint = builder.tint;
        this.hardness = builder.hardness;
        this.harvestLevel = builder.harvestLevel;
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
     * A ground surface fills the floor layer - grass, sand, stone, the ores, snow -
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

    /** {@code true} when the block is a fluid such as water. */
    public boolean isLiquid() {
        return liquid;
    }

    /** {@code true} when blocks below this one stay visible. */
    public boolean isTransparent() {
        return transparent;
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
     * {@link com.philia093.neofactory.world.interaction.HardnessMining}.
     */
    public int harvestLevel() {
        return harvestLevel;
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

    /** Fluent builder for {@link Block} instances. */
    public static final class Builder {

        private final int id;
        private final String name;
        private String texture = NO_TEXTURE;
        private boolean solid = true;
        private boolean ground = false;
        private boolean liquid = false;
        private boolean transparent = false;
        private Color tint = new Color(Color.WHITE);
        private float hardness = 1.0f;
        private int harvestLevel;

        private Builder(int id, String name) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
        }

        /** Sets the single texture used for this block. */
        public Builder texture(String texture) {
            this.texture = Objects.requireNonNull(texture, "texture");
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

        public Builder liquid(boolean liquid) {
            this.liquid = liquid;
            return this;
        }

        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
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

        public Block build() {
            return new Block(this);
        }
    }
}