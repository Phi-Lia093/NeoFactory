package com.philia093.neofactory.support;

import java.util.List;

/**
 * Every face the art pack draws of a block that is more than one picture.
 * <p>
 * Most blocks of the pack are one picture, seen from above: a stone, a plank, an ore. A block the
 * player stands next to is not - the grass is a bright top over a side that carries its own shade
 * and a second layer the colour of the biome is painted through, a log is bark around rings, a
 * furnace is a plain top over a front with a mouth. The pack draws those with one file per face,
 * and the flat engine of this project threw every face away that a view from above never showed,
 * which is why the block folder held 271 pictures and not 356.
 * <p>
 * A world of cubes shows six faces, so the missing ones were fetched back by
 * {@code build/verify/extract_3d_assets.ps1}. This class is the list of them - the faces of the
 * blocks that are more than one picture, plus the sheets whose animation is described by a file
 * next to them - and {@link TextureAuditTest#everyFaceOfAMultiFaceBlockExists()} is the net under
 * the list: deleting one of these pictures fails the build instead of showing up as a missing face
 * in the world.
 * <p>
 * The names are the file names of {@code assets/blocks} without the extension, exactly the way
 * {@link com.philia093.neofactory.block.Block} names a picture.
 */
public final class MultiFaceTextures {

    /**
     * Faces of the blocks the pack draws with more than one picture.
     * <p>
     * The list is sorted and grouped the way the pack groups the blocks, so a face that is missing
     * is found by looking at its neighbours.
     */
    public static final List<String> FACES = List.of(
            // The anvil: the body of the block, and its top in the three stages of wear.
            "anvil_base",

            // The two halves of a bed, each with the board, the side and the end of the frame.
            "bed_feet_end", "bed_feet_side", "bed_head_end", "bed_head_side",

            // The brewing stand, the pod of the cactus, the cake and the cauldron with its inside.
            "brewing_stand_base",
            "cactus_bottom", "cactus_side",
            "cake_bottom", "cake_inner", "cake_side",
            "cauldron_bottom", "cauldron_inner", "cauldron_side",

            // The workbench, drawn from the front and from the side.
            "crafting_table_front", "crafting_table_side",

            // The daylight detector, the podzol, the moss and the hay with their own side.
            "daylight_detector_side", "dirt_podzol_side", "mycelium_side", "hay_block_side",

            // The two mouths of a dispenser and of a dropper, one for each way they can point.
            "dispenser_front_horizontal", "dispenser_front_vertical",
            "dropper_front_horizontal", "dropper_front_vertical",

            // The two halves of a wooden and of an iron door.
            "door_iron_lower", "door_iron_upper", "door_wood_lower", "door_wood_upper",

            // The tall plants, whose lower half is drawn apart from the upper one.
            "double_plant_fern_bottom", "double_plant_grass_bottom", "double_plant_paeonia_bottom",
            "double_plant_rose_bottom", "double_plant_syringa_bottom",
            "double_plant_sunflower_back", "double_plant_sunflower_bottom",
            "double_plant_sunflower_front",

            // The table of enchantment, the frame of a portal and the box of a jukebox.
            "enchanting_table_bottom", "enchanting_table_side", "endframe_side", "jukebox_side",

            // The furnace, whose front is a mouth that tells whether it is burning.
            "furnace_front_off", "furnace_front_on", "furnace_side",

            // The grass, whose side carries its own shade, a layer for the colour of the biome and
            // another one under snow.
            "grass_side", "grass_side_overlay", "grass_side_snowed",

            // The hopper, from the outside and from the inside.
            "hopper_inside", "hopper_outside",

            // The bark of the six logs, the side of the melon and the skin of a huge mushroom.
            "log_acacia", "log_big_oak", "log_birch", "log_jungle", "log_oak", "log_spruce",
            "melon_side", "mushroom_block_inside", "mushroom_block_skin_stem",

            // The piston, with the rod that comes out of it and the plate on its top.
            "piston_bottom", "piston_inner", "piston_side",

            // The pumpkin and its carved face, lit and unlit.
            "pumpkin_face_off", "pumpkin_face_on", "pumpkin_side",

            // The quartz and the sandstone, which are cut rather than found.
            "quartz_block_bottom", "quartz_block_chiseled", "quartz_block_lines",
            "quartz_block_side",
            "sandstone_bottom", "sandstone_carved", "sandstone_normal", "sandstone_smooth",

            // The rim of a slab and the sides of a block of TNT.
            "stone_slab_side", "tnt_bottom", "tnt_side");

    /**
     * Sheets whose animation is described by a metadata file next to them.
     * <p>
     * The pack keeps the length of an animation in a small file beside the sheet, not inside it.
     * The game reads its own animations from {@link com.philia093.neofactory.block.Block.Animation}
     * and does not need that file yet, but it travels with the art: the sheet of water is a strip
     * of frames and loses its meaning without it, so the pair is kept together.
     */
    public static final List<String> ANIMATED_SHEETS = List.of("fire_layer_0", "fire_layer_1",
            "portal");

    private MultiFaceTextures() {
        // Utility class: never instantiated.
    }
}
