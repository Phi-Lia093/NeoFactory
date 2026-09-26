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
            "grass_side", "log_oak", "sandstone_bottom", "stone_slab_side", "stone_slab_top",

            // The casing of the machines of the industry, which the mod draws as one picture per side of
            // a block, and the mouth of a boiler, which is drawn over the front of the casing like the
            // second layer of the grass: it is the door of the boiler and it glows while the boiler burns.
            "bronze_casing/bronze_casing_bottom", "bronze_casing/bronze_casing_side",
            "bronze_casing/bronze_casing_top", "bronze_boiler/bronze_boiler_front",
            "bronze_boiler/bronze_boiler_front_active",

            // The front and the top of every machine of the age of steam, drawn over the casing the way the
            // mouth of the boiler is: the picture of the machine, and the one of a machine that works, which
            // glows while it runs. The machines of steel carry the same pictures over the casing of their own
            // age, so both of them are listed.
            "alloy_furnace/alloy_furnace_front", "alloy_furnace/alloy_furnace_front_active",
            "alloy_furnace/alloy_furnace_top", "alloy_furnace/alloy_furnace_top_active",
            "compressor/compressor_front", "compressor/compressor_front_active",
            "compressor/compressor_top", "compressor/compressor_top_active",
            "extractor/extractor_front", "extractor/extractor_front_active",
            "extractor/extractor_top", "extractor/extractor_top_active",
            "forge_hammer/forge_hammer_front", "forge_hammer/forge_hammer_front_active",
            "forge_hammer/forge_hammer_top", "forge_hammer/forge_hammer_top_active",
            "grinder/grinder_front", "grinder/grinder_front_active",
            "grinder/grinder_top", "grinder/grinder_top_active",
            "steam_furnace/steam_furnace_front", "steam_furnace/steam_furnace_front_active",
            "steam_furnace/steam_furnace_top", "steam_furnace/steam_furnace_top_active",
            "steel_alloy_furnace/steel_alloy_furnace_front",
            "steel_alloy_furnace/steel_alloy_furnace_front_active",
            "steel_alloy_furnace/steel_alloy_furnace_top",
            "steel_alloy_furnace/steel_alloy_furnace_top_active",
            "steel_boiler/steel_boiler_front", "steel_boiler/steel_boiler_front_active",
            "steel_compressor/steel_compressor_front", "steel_compressor/steel_compressor_front_active",
            "steel_compressor/steel_compressor_top", "steel_compressor/steel_compressor_top_active",
            "steel_extractor/steel_extractor_front", "steel_extractor/steel_extractor_front_active",
            "steel_extractor/steel_extractor_top", "steel_extractor/steel_extractor_top_active",
            "steel_forge_hammer/steel_forge_hammer_front",
            "steel_forge_hammer/steel_forge_hammer_front_active",
            "steel_forge_hammer/steel_forge_hammer_top",
            "steel_forge_hammer/steel_forge_hammer_top_active",
            "steel_grinder/steel_grinder_front", "steel_grinder/steel_grinder_front_active",
            "steel_grinder/steel_grinder_top", "steel_grinder/steel_grinder_top_active",
            "steel_steam_furnace/steel_steam_furnace_front",
            "steel_steam_furnace/steel_steam_furnace_front_active",
            "steel_steam_furnace/steel_steam_furnace_top",
            "steel_steam_furnace/steel_steam_furnace_top_active",
            "steel_bricks_casing/steel_bricks_casing_bottom", "steel_bricks_casing/steel_bricks_casing_side",
            "steel_bricks_casing/steel_bricks_casing_top",
            "steel_casing/steel_casing_bottom", "steel_casing/steel_casing_side",
            "steel_casing/steel_casing_top",
            "bronze_bricks_casing/bronze_bricks_casing_bottom",
            "bronze_bricks_casing/bronze_bricks_casing_side",
            "bronze_bricks_casing/bronze_bricks_casing_top");

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
