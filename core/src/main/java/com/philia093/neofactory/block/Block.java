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
    private final boolean liquid;
    private final boolean transparent;
    private final Color tint;
    private final float hardness;

    private Block(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.texture = builder.texture;
        this.solid = builder.solid;
        this.liquid = builder.liquid;
        this.transparent = builder.transparent;
        this.tint = builder.tint;
        this.hardness = builder.hardness;
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

    /** {@code true} when the block stops player movement. */
    public boolean isSolid() {
        return solid;
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
     * Only relevant once block breaking is implemented.
     */
    public float hardness() {
        return hardness;
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
        private boolean liquid = false;
        private boolean transparent = false;
        private Color tint = new Color(Color.WHITE);
        private float hardness = 1.0f;

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

        public Block build() {
            return new Block(this);
        }
    }
}