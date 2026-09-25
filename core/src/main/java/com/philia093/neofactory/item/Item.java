package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Block;

import java.util.List;
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

    /** Amount a unique item, such as a tool, stacks to. */
    public static final int SINGLE_ITEM_STACK = 1;

    /** Mining speed of a bare hand, also the default of every item. */
    public static final float HAND_MINING_SPEED = 1.0f;

    private final int id;
    private final String name;
    private final String displayName;
    private final String texture;
    private final int iconFrame;
    private final Color tint;
    private final int maxStackSize;
    private final int toolLevel;
    private final float miningSpeed;
    private final ToolType toolType;
    private final int maxDamage;
    private final FluidContainer container;
    private final String chemicalFormula;
    private final String overlayTexture;
    private final FaceTool faceTool;

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
        this.toolLevel = builder.toolLevel;
        this.miningSpeed = builder.miningSpeed;
        this.toolType = builder.toolType;
        this.maxDamage = builder.maxDamage;
        this.container = builder.container;
        this.chemicalFormula = builder.chemicalFormula != null ? builder.chemicalFormula : "";
        this.overlayTexture = builder.overlayTexture != null ? builder.overlayTexture : NO_TEXTURE;
        this.faceTool = builder.faceTool != null ? builder.faceTool : FaceTool.NONE;
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
        this.toolLevel = builder.toolLevel;
        this.miningSpeed = builder.miningSpeed;
        this.toolType = builder.toolType;
        this.maxDamage = builder.maxDamage;
        this.container = builder.container;
        this.chemicalFormula = builder.chemicalFormula != null ? builder.chemicalFormula : "";
        this.overlayTexture = builder.overlayTexture != null ? builder.overlayTexture : NO_TEXTURE;
        this.faceTool = builder.faceTool != null ? builder.faceTool : FaceTool.NONE;
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

    /**
     * Mining level of this item as a tool.
     * <p>
     * A bare hand counts as level {@code 0}, so an item that is not a tool keeps
     * that value. The level is compared with
     * {@link com.philia093.neofactory.block.Block#harvestLevel()} to decide whether
     * a block drops its item, see
     * {@link com.philia093.neofactory.world.interaction.HardnessMining}.
     * <p>
     * The level says how good a tool is, the kind of it says what it is good for: a block of level
     * {@code 1} hands its item to a pickaxe of that level and to nothing else, see
     * {@link #toolType()}. A hoe and a sword carry level {@code 0}, because they open no block the
     * game has.
     */
    public int toolLevel() {
        return toolLevel;
    }

    /**
     * Speed this item breaks blocks with.
     * <p>
     * {@code 1} is the speed of a bare hand, a diamond pickaxe reaches {@code 8}. The speed counts
     * for the blocks the kind of this item fits, see {@link #toolType()}, and only a rule with
     * break times reads it.
     */
    public float miningSpeed() {
        return miningSpeed;
    }

    /**
     * Kind of tool this item is, {@code null} for an item that is not a tool.
     * <p>
     * A block names the kind it is worked with as well, and the two are compared as objects: a
     * block that wants a pickaxe is mined quickly by this item and slowly by every other one, and
     * a block that asks for a mining level hands its item over only to the right kind of tool, see
     * {@link com.philia093.neofactory.world.interaction.HardnessMining}.
     *
     * @return the kind of this tool, or {@code null} for a material, a block or a piece of food
     */
    public ToolType toolType() {
        return toolType;
    }

    /** {@code true} when this item is a tool and not a material, a block or a piece of food. */
    public boolean isTool() {
        return toolType != null;
    }

    /**
     * Amount of damage a fresh piece of this item takes before it is used up.
     * <p>
     * Zero is an item that never wears out, which is every material and every block; a tool of iron
     * reaches {@code 250} and one of diamond {@code 1561}. The number belongs to the kind of item,
     * the damage taken belongs to the stack that is used, see
     * {@link Damageable} and {@link ItemStack#applyDamage(int)}.
     *
     * @return the life of a new piece, {@code 0} for an item that never wears out
     */
    public int maxDamage() {
        return maxDamage;
    }

    /** {@code true} when this item wears out with use, which a tool and every future worn part does. */
    public boolean isDamageable() {
        return maxDamage > 0;
    }

    /**
     * What this item does with a fluid, {@code null} for an item that carries none.
     * <p>
     * A cell is the item that carries fluid, and it is an ordinary item with a
     * container, see {@link FluidContainer}: that keeps the world, the inventory and the save
     * format free of a second kind of stack, and it is why a full cell is its own item.
     *
     * @return the container, or {@code null} when this item never carries a fluid
     */
    public FluidContainer container() {
        return container;
    }

    /** {@code true} when this item carries a fluid, see {@link #container()}. */
    public boolean isFluidContainer() {
        return container != null;
    }

    /** {@code true} when the item stacks, which every item but a tool does. */
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
     * Picture drawn over the icon in its own colours.
     * <p>
     * A shape of a material that covers something - a metal drawn around a darker core - carries
     * two layers, see
     * {@link com.philia093.neofactory.material.MaterialForm#overlayTexture()}. The icon is the
     * shape and takes the tint of the item, this overlay is drawn over it with a white tint, so
     * the part the metal covers keeps the colour it was drawn in.
     *
     * @return the path of the overlay, relative to the asset root and without extension, or an
     *         empty string for an item that is drawn from a single picture
     */
    public String overlayTexture() {
        return overlayTexture;
    }

    /** {@code true} when this item is drawn from two layers, see {@link #overlayTexture()}. */
    public boolean hasOverlay() {
        return !overlayTexture.isEmpty();
    }

    /**
     * The tool this item is for a face of a block, {@link FaceTool#NONE} for everything else.
     * <p>
     * A wrench and a screwdriver do nothing while they are swung at a block; they are held at a face of
     * one, where the grid of nine cells appears and a cell of it carries out what the tool is good for,
     * see {@link com.philia093.neofactory.world.interaction.FaceOperable}. What a tool is good for is
     * part of the definition of the item, so the interaction never has to compare names or pictures.
     *
     * @return the tool, never {@code null}
     */
    public FaceTool faceTool() {
        return faceTool;
    }

    /**
     * Chemical formula of the material this item is made of.
     *
     * @return a formula such as {@code "Fe"}, or an empty string for an item that has none
     */
    public String chemicalFormula() {
        return chemicalFormula;
    }

    /** {@code true} when this item carries a chemical formula worth showing. */
    public boolean hasChemicalFormula() {
        return !chemicalFormula.isEmpty();
    }

    /**
     * The lines the tooltip of this item is drawn from.
     * <p>
     * The first line is always the name, a formula follows when the item has one: a plate of iron
     * reads as {@code "Iron Plate"} over {@code "Fe"}. A caller that has more to say - a machine
     * that names the energy it holds, a tool that names the block it breaks - copies this list and
     * appends what it knows, see {@code ItemTooltip}.
     *
     * @return the lines, at least the name of the item
     */
    public List<String> tooltipLines() {
        if (!hasChemicalFormula()) {
            return List.of(displayName);
        }
        return List.of(displayName, chemicalFormula);
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
        private int toolLevel;
        private float miningSpeed = HAND_MINING_SPEED;
        private ToolType toolType;
        private int maxDamage;
        private FluidContainer container;
        private String chemicalFormula;
        private String overlayTexture;
        private FaceTool faceTool;

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

        /**
         * Makes this item a tool of a mining level.
         * <p>
         * The level is what
         * {@link com.philia093.neofactory.world.interaction.HardnessMining} compares
         * with the level a block needs. Only tools whose material is known carry a
         * level, see {@link #toolLevel()}.
         *
         * @param toolLevel mining level, {@code 0} for an item that is not a tool
         */
        public Builder toolLevel(int toolLevel) {
            if (toolLevel < 0) {
                throw new IllegalArgumentException("Tool level must not be negative: " + toolLevel);
            }
            this.toolLevel = toolLevel;
            return this;
        }

        /**
         * Sets the speed this item breaks blocks with.
         *
         * @param miningSpeed speed, {@code 1} for a bare hand
         */
        public Builder miningSpeed(float miningSpeed) {
            if (miningSpeed <= 0.0f) {
                throw new IllegalArgumentException("Mining speed must be positive: " + miningSpeed);
            }
            this.miningSpeed = miningSpeed;
            return this;
        }

        /**
         * Makes this item a tool of a kind.
         * <p>
         * The kind is what a block is worked with: a block that names a pickaxe is mined quickly by
         * this item and slowly by every other one, and a block that asks for a mining level hands
         * its item over only to the kind it names, see {@link ToolType} and
         * {@link com.philia093.neofactory.world.interaction.HardnessMining}.
         *
         * @param toolType kind of tool, for example {@link ToolType#PICKAXE}
         */
        public Builder toolType(ToolType toolType) {
            this.toolType = Objects.requireNonNull(toolType, "toolType");
            return this;
        }

        /**
         * Makes this item wear out with use.
         * <p>
         * The amount is what a fresh piece takes and it is not a property of a tool alone: a mortar,
         * a screwdriver or any other piece of the workshop declares its own life the same way, see
         * {@link Damageable}. The damage taken belongs to the stack that is used and starts at
         * zero, so every piece that is handed out is new, see {@link ItemStack#applyDamage(int)}.
         *
         * @param maxDamage amount of use a fresh piece takes, must be positive
         */
        public Builder maxDamage(int maxDamage) {
            if (maxDamage <= 0) {
                throw new IllegalArgumentException("Durability must be positive: " + maxDamage);
            }
            this.maxDamage = maxDamage;
            return this;
        }

        /**
         * Makes this item a container of fluid, a cell.
         * <p>
         * The container is part of the definition of the item and not of a stack: the empty
         * cell and the water cell are two items, each with its own picture, which is what
         * lets a full cell be an icon of its own.
         *
         * @param container what the item carries and what it may take
         */
        public Builder container(FluidContainer container) {
            this.container = Objects.requireNonNull(container, "container");
            return this;
        }

        /**
         * Sets the chemical formula shown under the name of the item.
         * <p>
         * The formula is the second line of the tooltip, see {@link Item#tooltipLines()}. It is
         * written by the material the item is made of, so every shape of iron carries {@code
         * "Fe"} by itself.
         *
         * @param chemicalFormula formula such as {@code "Fe"}, empty for no second line
         */
        public Builder formula(String chemicalFormula) {
            this.chemicalFormula = Objects.requireNonNull(chemicalFormula, "chemicalFormula");
            return this;
        }

        /**
         * Sets a picture drawn over the icon in its own colours.
         *
         * @param overlayTexture path of the overlay, relative to the asset root and without
         *                       extension, for example {@code "items/generic_fine_wire_overlay"}
         */
        public Builder overlayTexture(String overlayTexture) {
            this.overlayTexture = Objects.requireNonNull(overlayTexture, "overlayTexture");
            return this;
        }

        /**
         * Names the tool this item is at a face of a block.
         *
         * @param faceTool tool the item stands for, never {@code null}
         * @return this builder
         */
        public Builder faceTool(FaceTool faceTool) {
            this.faceTool = Objects.requireNonNull(faceTool, "faceTool");
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
