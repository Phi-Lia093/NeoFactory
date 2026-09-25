package com.philia093.neofactory.render;

import com.philia093.neofactory.util.Constants;

/**
 * The sizes of an arm of a first-person view, and the card a tool is held as.
 * <p>
 * The arm is one box of the size a skin draws it in: four pixels across, four pixels thick and twelve
 * pixels long, see {@link #WIDTH}, {@link #THICKNESS} and {@link #LENGTH}. It reaches along the negative
 * Z axis, which is the direction the eye looks, and the mesh it is drawn from is the unit cube of a block,
 * meshed by {@link SectionMesher} - the shape of an arm is what the model matrix does to that cube, see
 * {@code FirstPersonHand}.
 */
public final class HandMeshes {

    /** Width of the arm, in blocks: four pixels of a skin. */
    public static final float WIDTH = 4.0f / Constants.TILE_SIZE;

    /** Thickness of the arm, in blocks: four pixels of a skin. */
    public static final float THICKNESS = 4.0f / Constants.TILE_SIZE;

    /** Length of the arm into the view, in blocks: twelve pixels of a skin. */
    public static final float LENGTH = 12.0f / Constants.TILE_SIZE;

    private HandMeshes() {
        // Utility class: never instantiated.
    }

    /**
     * Meshes a flat item, the card a tool or a material is held as.
     * <p>
     * A block that is held is a small cube, see {@link ItemCubeMeshes}, but a stick, a pickaxe or an
     * ingot has one picture and no shape: it is held as a card that faces the eye, which is what the
     * original game shows as well. The card stands across the view at the end of the arm and covers the
     * whole picture of the item, so the shape of it is the silhouette of its own art.
     * <p>
     * <b>The card is drawn the way the world draws a face that stands upright</b>, see
     * {@link SectionMesher#build}: the picture of an item starts at the top left, so the upper corners of
     * the card take the smaller coordinate. A card written the other way round stands on its head, which
     * is what a player sees at once in a hand.
     *
     * @param layer layer the picture of the item lives in, see {@link BlockPictures#layer(String)}
     * @param red red the picture is multiplied with, the tint of the item
     * @param green green the picture is multiplied with
     * @param blue blue the picture is multiplied with
     * @return the two triangles of the card
     */
    public static MeshData flatItem(int layer, float red, float green, float blue) {
        MeshData mesh = new MeshData();
        float half = LENGTH * 0.5f;
        int[] corners = new int[4];
        // The card lies in the plane the eye looks along, so its four corners differ only in X and Y and
        // it faces the eye with the whole picture. The picture of a material holds brightness only, so the
        // tint of the item is what makes a plate of iron and a plate of copper two different things in a
        // hand.
        corners[0] = mesh.addVertex(-half, -half, -LENGTH, 0.0f, 1.0f, layer, red, green, blue);
        corners[1] = mesh.addVertex(half, -half, -LENGTH, 1.0f, 1.0f, layer, red, green, blue);
        corners[2] = mesh.addVertex(half, half, -LENGTH, 1.0f, 0.0f, layer, red, green, blue);
        corners[3] = mesh.addVertex(-half, half, -LENGTH, 0.0f, 0.0f, layer, red, green, blue);
        mesh.addTriangle(corners[0], corners[1], corners[2]);
        mesh.addTriangle(corners[0], corners[2], corners[3]);
        return mesh;
    }
}
