package com.philia093.neofactory.support;

import java.util.List;

/**
 * Every face the art pack draws of the blocks this project keeps.
 * <p>
 * Most blocks of the pack are one picture, seen from above: a stone, a plank, an ore. A block the
 * player stands next to is not - the grass is a bright top over a side that carries its own shade, a
 * log is bark around rings, a furnace is a plain top over a front with a mouth. The pack draws those
 * with one file per face, and the flat engine of this project threw every face away that a view from
 * above never showed, which is why the block folder held 271 pictures and not 356.
 * <p>
 * A world of cubes shows six faces, so the missing ones came home through
 * {@code build/verify/extract_3d_assets.ps1}. <b>This class is the list of the ones that are
 * kept</b>: the faces of the blocks this project holds art for. The art of the blocks the project
 * does not use was let go instead - it is one call of git away, and the script above fetches it from
 * the original pack again.
 * <p>
 * <b>A face inside a folder of its own names that folder.</b> The pack groups the art of a block that
 * comes in several parts - the anvil, the cauldron, the crafting table, the furnace, the
 * stages of a broken block - in a folder next to the single pictures, so a face such as
 * {@code furnace/furnace_side} is read from {@code assets/blocks/furnace/furnace_side.png}, see
 * {@code BlockPictures#path(String)}.
 * <p>
 * {@link TextureAuditTest#everyFaceOfAMultiFaceBlockExists()} is the net under that list: deleting
 * one of these pictures fails the build instead of showing up as a missing face in the world.
 */
public final class MultiFaceTextures {

    /**
     * Faces of the blocks the pack draws with more than one picture.
     * <p>
     * The list is sorted and grouped the way the pack groups the blocks, so a face that is missing
     * is found by looking at its neighbours.
     */
    public static final List<String> FACES = List.of(
            // The anvil, whose body is one picture while its top is a plate of its own.
            "anvil/anvil_base", "anvil/anvil_top",

            // The cauldron, whose inside is a picture of its own.
            "cauldron/cauldron_bottom", "cauldron/cauldron_inner", "cauldron/cauldron_side",

            // The workbench, drawn from the front and from the side, and the furnace, whose front is a
            // mouth that tells whether it is burning.
            "crafting_table/crafting_table_front", "crafting_table/crafting_table_side",
            "furnace/furnace_front_off", "furnace/furnace_front_on", "furnace/furnace_side",

            // The grass, whose side carries its own shade, the bark of the oak trunk, whose rings live
            // in a picture of their own, the rim and the top of a slab, and the sandstone, whose bottom
            // is a plate of its own below the smooth top.
            "grass_side", "log_oak", "sandstone_bottom", "stone_slab_side", "stone_slab_top");

    /**
     * Sheets whose animation is described by a metadata file next to them.
     * <p>
     * The pack keeps the length of an animation in a small file beside the sheet, not inside it. The
     * game reads its own animations from {@link com.philia093.neofactory.block.Block.Animation} and
     * does not need that file yet, so the list holds no sheet today: the water, the lava and the
     * portal of the pack were let go with the art of the blocks that are not part of this project.
     * A sheet that comes back is watched here as well, one metadata file per name.
     */
    public static final List<String> ANIMATED_SHEETS = List.of();

    private MultiFaceTextures() {
        // Utility class: never instantiated.
    }
}
