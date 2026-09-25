package com.philia093.neofactory.block.model;

import com.philia093.neofactory.block.BlockFace;

import java.util.EnumMap;
import java.util.Map;

/**
 * The models a block is drawn from that need no file of their own.
 * <p>
 * A block of the art pack that is one picture from every side is a whole cube of that picture, and
 * so is a block no model file was written for: {@link com.philia093.neofactory.block.model.ModelRegistry}
 * falls back to the picture {@link com.philia093.neofactory.block.Block#texture()} names, which is
 * why a world still draws every block of an older build even when a model file is lost.
 */
public final class ModelTemplates {

    /** Amount of sixteenths one block is measured in. */
    public static final float UNITS = ModelBox.UNITS;

    private ModelTemplates() {
        // Utility class: never instantiated.
    }

    /**
     * A whole cube of one picture, the shape a plain block of the art pack is.
     *
     * @param picture picture of every face, a name relative to {@code blocks/}
     * @return the model, one box of six faces, each naming the direction it lies in
     */
    public static BlockModel wholeCube(String picture) {
        return wholeCube(picture, false);
    }

    /**
     * A whole cube of one picture.
     *
     * @param picture picture of every face, a name relative to {@code blocks/}
     * @param tinted {@code true} when the tint of the block is multiplied over every face
     * @return the model, one box of six faces, each naming the direction it lies in
     */
    public static BlockModel wholeCube(String picture, boolean tinted) {
        if (picture == null || picture.isEmpty()) {
            return BlockModel.EMPTY;
        }
        ModelBox box = new ModelBox(0.0f, 0.0f, 0.0f, UNITS, UNITS, UNITS, null, true);
        for (BlockFace face : BlockFace.ALL) {
            box.setFace(face, new ModelFace(picture, "", 0.0f, 0.0f, 1.0f, 1.0f, face, 0, tinted));
        }
        return new BlockModel(picture, java.util.List.of(box));
    }

    /**
     * A cube of several pictures, the shape the grass, the sandstone and the log have.
     *
     * @param pictures picture of every face that differs, the others are taken from
     *                 {@link BlockFace#ALL}
     * @param tinted {@code true} when the tint of the block is multiplied over every face
     * @return the model
     */
    public static BlockModel cube(Map<BlockFace, String> pictures, boolean tinted) {
        ModelBox box = new ModelBox(0.0f, 0.0f, 0.0f, UNITS, UNITS, UNITS, null, true);
        Map<BlockFace, String> faces = new EnumMap<>(BlockFace.class);
        faces.putAll(pictures);
        for (BlockFace face : BlockFace.ALL) {
            String picture = faces.get(face);
            if (picture == null) {
                continue;
            }
            box.setFace(face, new ModelFace(picture, "", 0.0f, 0.0f, 1.0f, 1.0f, face, 0, tinted));
        }
        return new BlockModel("cube", java.util.List.of(box));
    }
}
