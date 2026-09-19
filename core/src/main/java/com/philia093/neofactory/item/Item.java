package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Block;

import java.util.Objects;

/**
 * Immutable description of a single item type.
 * <p>
 * An item is what the player carries in the inventory: a picture, a readable
 * name and a maximum stack size. Items that place a block - stone, planks and
 * every other block the player may build with - are represented by a
 * {@link BlockItem}, which keeps the link to the {@link Block} and borrows its
 * picture and colour.
 * <p>
 * The texture is a path relative to the asset root, without the {@code .png}
 * extension, for example {@code items/apple} or {@code blocks/grass_top}. A name
 * without a slash is resolved inside the block folder by
 * {@link com.philia093.neofactory.render.BlockTextureCache}, which is why the
 * plain items of {@link Items} spell their folder out.
 * <p>
 * Ids are stable: an item keeps the same id forever so that a stored inventory
 * stays readable. New items must be appended at the end of {@link Items} and the
 * value of {@link Items#NEXT_FREE_ID} has to be bumped accordingly.
 */
public class Item {

    /** Folder holding every item icon, relative to the asset root. */
    public static final String ITEM_FOLDER = "items/";

    /** Texture name placeholder used by items that are never drawn. */
    public static final String NO_TEXTURE = "";

    /** Amount a normal item, such as a block, a material or a piece of food, stacks to. */
    public static final int DEFAULT_MAX_STACK = 64;

    /** Amount a unique item, such as a tool or a piece of armour, stacks to. */
    public static final int SINGLE_ITEM_STACK = 1;

    private final int id;
    private final String name;
    private final String displayName;
    private final String texture;
    private final int iconFrame;
    private final Color tint;
    private final int maxStackSize;

    /**
     * Creates an item that is not linked to a block.
     *
     * @param builder filled builder, see {@link #builder(int, String)}
     */
    protected Item(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.displayName = builder.displayName != null ? builder.displayName : prettify(builder.name);
        this.texture = builder.texture != null ? builder.texture : NO_TEXTURE;
        this.iconFrame = builder.iconFrame;
        this.tint = builder.tint != null ? new Color(builder.tint) : new Color(Color.WHITE);
        this.maxStackSize = builder.maxStackSize;
    }

    /**
     * Creates an item that represents a block.
     * <p>
     * The picture and the colour of the block are reused, so a block item looks
     * exactly like the tile the player sees in the world. A definition may still
     * ask for its own picture by setting one on the builder.
     *
     * @param builder filled builder, see {@link #builder(int, String)}
     * @param block block this item places
     */
    protected Item(Builder builder, Block block) {
        Objects.requireNonNull(block, "block");
        this.id = builder.id;
        this.name = builder.name;
        this.displayName = builder.displayName != null ? builder.displayName : prettify(block.name());
        this.texture = builder.texture != null ? builder.texture : block.texture();
        this.iconFrame = builder.iconFrame;
        this.tint = builder.tint != null ? new Color(builder.tint) : new Color(block.tint());
        this.maxStackSize = builder.maxStackSize;
    }

    /** Unique numeric id of this item type. */
    public int id() {
        return id;
    }

    /** Technical name, used for logging and as a stable lookup key. */
    public String name() {
        return name;
    }

    /** Name shown to the player, for example inside an inventory tooltip. */
    public String displayName() {
        return displayName;
    }

    /** Texture path of the icon, relative to the asset root and without extension. */
    public String texture() {
        return texture;
    }

    /** Frame of the icon sheet to draw, {@code 0} for a static 16 by 16 picture. */
    public int iconFrame() {
        return iconFrame;
    }

    /** Color multiplied with the icon while drawing. */
    public Color tint() {
        return tint;
    }

    /** Amount of this item that fits into a single inventory slot. */
    public int maxStackSize() {
        return maxStackSize;
    }

    /** {@code true} when the item stacks, which every item but a tool or armour piece does. */
    public boolean isStackable() {
        return maxStackSize > 1;
    }

    /** {@code true} when this item places a block, see {@link #block()}. */
    public boolean isBlockItem() {
        return false;
    }

    /**
     * Block this item places.
     *
     * @return the linked block, or {@code null} for an item that is not a block
     */
    public Block block() {
        return null;
    }

    /** {@code true} when this item has an icon that can be drawn. */
    public boolean hasTexture() {
        return !texture.isEmpty();
    }

    /**
     * Derives a readable name from a technical one.
     * <p>
     * Underscores become spaces and every word starts with an upper case letter,
     * so {@code "log_oak"} turns into {@code "Log Oak"}. Definitions that want a
     * nicer name simply set their own {@code displayName}.
     *
     * @param name technical item name
     * @return the name in a form that can be shown to the player
     */
    public static String prettify(String name) {
        StringBuilder readable = new StringBuilder(name.length());
        boolean startOfWord = true;
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (character == '_') {
                readable.append(' ');
                startOfWord = true;
                continue;
            }
            readable.append(startOfWord ? Character.toUpperCase(character) : character);
            startOfWord = false;
        }
        return readable.toString();
    }

    /** Starts building a new item definition. */
    public static Builder builder(int id, String name) {
        return new Builder(id, name);
    }

    @Override
    public String toString() {
        return "Item(" + id + ", " + name + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item)) {
            return false;
        }
        return id == ((Item) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** Fluent builder for {@link Item} and {@link BlockItem} instances. */
    public static final class Builder {

        private final int id;
        private final String name;
        private String displayName;
        private String texture;
        private int iconFrame;
        private Color tint;
        private int maxStackSize = DEFAULT_MAX_STACK;

        private Builder(int id, String name) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
        }

        /**
         * Sets the name shown to the player.
         *
         * @param displayName readable name, derived from the technical name when omitted
         */
        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        /** Sets the icon path, relative to the asset root and without extension. */
        public Builder texture(String texture) {
            this.texture = Objects.requireNonNull(texture, "texture");
            return this;
        }

        /** Selects the frame of an animated icon sheet, {@code 0} by default. */
        public Builder iconFrame(int iconFrame) {
            if (iconFrame < 0) {
                throw new IllegalArgumentException("Icon frame must not be negative: " + iconFrame);
            }
            this.iconFrame = iconFrame;
            return this;
        }

        /** Overrides the colour multiplied with the icon while drawing. */
        public Builder tint(Color tint) {
            this.tint = new Color(Objects.requireNonNull(tint, "tint"));
            return this;
        }

        /** Sets how many of this item fit into a single slot. */
        public Builder maxStackSize(int maxStackSize) {
            if (maxStackSize < 1) {
                throw new IllegalArgumentException("Stack size must be positive: " + maxStackSize);
            }
            this.maxStackSize = maxStackSize;
            return this;
        }

        /** Builds a plain item that is not linked to a block. */
        public Item build() {
            return new Item(this);
        }

        /**
         * Builds an item that places the given block.
         *
         * @param block block the item represents
         * @return the block item, its picture and colour taken from the block
         */
        public BlockItem buildBlock(Block block) {
            return new BlockItem(this, block);
        }
    }
}
