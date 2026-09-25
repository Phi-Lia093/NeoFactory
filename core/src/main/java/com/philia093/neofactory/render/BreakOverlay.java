package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.block.BlockFace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The pictures of a block coming apart, drawn over the cell that is being broken.
 * <p>
 * A player has to see what they are breaking and how far along they are, so the cell collects cracks while
 * the button is held: the picture of the stage the break has reached is drawn over every face of that cell,
 * one hair outside it, and the block underneath shows through the cracks. The ten stages are the ten
 * pictures the art pack draws, from the first chip to a block that is held together by nothing, see
 * {@link BlockPictures#DESTROY_STAGES}.
 * <p>
 * A stage is drawn the way a block reads its own pictures, see {@link FaceLayout}, and every one of its six
 * faces is wound counter clockwise seen from outside, so the faces a turning eye cannot see are culled like
 * the faces of the block itself. A stage is meshed once and kept: the same six quads serve every block of
 * the world and every frame of a break, and the place of the overlay is a model matrix.
 * <p>
 * <b>Nothing is drawn while the art is missing.</b> A stage whose picture the array of the world does not
 * hold draws nothing and is named in the log once, instead of failing a break.
 */
public class BreakOverlay implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Amount of stages a break is shown with, one picture per stage. */
    public static final int STAGES = BlockPictures.DESTROY_STAGES;

    /**
     * Distance the overlay stands outside the cell, in blocks.
     * <p>
     * A hair is enough and it is what the frame around a cell uses for the same reason: two cards in the
     * very same place are one card to a graphics card, so an overlay drawn exactly on the face of a block
     * would be drawn together with it and win or lose by chance.
     */
    private static final float OUTSIDE = 0.002f;

    /** Brightness the cracks are drawn with: white, so the art keeps the colours it was drawn in. */
    private static final float LIGHT = 1.0f;

    private final BlockPictures pictures;

    /** Mesh of every stage, built on first use, {@code null} while it is not built. */
    private final Mesh[] stages = new Mesh[STAGES];

    /** Stages whose picture the array does not hold, so each of them is reported once. */
    private final boolean[] withoutPicture = new boolean[STAGES];

    /**
     * Creates the cache.
     *
     * @param pictures pictures of the world, asked for the layer of a stage
     */
    public BreakOverlay(BlockPictures pictures) {
        this.pictures = pictures;
    }

    /**
     * Stage of a break that is this far along.
     *
     * @param progress progress of the break, {@code 0} to {@code 1}
     * @return the stage, {@code 0} for a break that just started and {@code -1} while nothing is broken
     */
    public static int stageOf(float progress) {
        if (progress <= 0.0f) {
            return -1;
        }
        return Math.min(STAGES - 1, (int) (progress * STAGES));
    }

    /**
     * Mesh of the stage a break has reached, meshed on first use.
     *
     * @param progress progress of the break, {@code 0} to {@code 1}
     * @return the mesh of that stage, {@code null} while nothing is broken or the art is missing
     */
    public Mesh meshFor(float progress) {
        int stage = stageOf(progress);
        if (stage < 0) {
            return null;
        }
        Mesh mesh = stages[stage];
        if (mesh != null) {
            return mesh;
        }
        int layer = pictures.layer(BlockPictures.destroyStagePicture(stage));
        if (layer < 0) {
            if (!withoutPicture[stage]) {
                withoutPicture[stage] = true;
                LOGGER.warn("The picture of stage {} of a break is not part of the world, so a breaking "
                        + "block shows no cracks", stage);
            }
            return null;
        }
        Mesh built = upload(overlay(layer));
        stages[stage] = built;
        return built;
    }

    /** Amount of stages whose mesh is held right now. */
    public int stageCount() {
        int built = 0;
        for (Mesh mesh : stages) {
            if (mesh != null) {
                built++;
            }
        }
        return built;
    }

    /**
     * The six faces of one cell, one hair outside it, carrying the picture of one stage of a break.
     *
     * @param layer layer the picture of that stage lives in
     * @return the triangles of the overlay, spanning the cell from the origin
     */
    static MeshData overlay(int layer) {
        MeshData mesh = new MeshData();
        for (BlockFace face : BlockFace.ALL) {
            int[] corners = new int[4];
            for (int corner = 0; corner < 4; corner++) {
                float x = face.cornerX(corner);
                float y = face.cornerY(corner);
                float z = face.cornerZ(corner);
                // The picture is read across the face and down from its first row, the way a block reads
                // its own pictures, see FaceLayout.
                float across = FaceLayout.across(face, x, y, z);
                float down = 1.0f - FaceLayout.up(face, x, y, z);
                corners[corner] = mesh.addVertex(pushed(x), pushed(y), pushed(z), across, down, layer,
                        LIGHT, LIGHT, LIGHT);
            }
            mesh.addTriangle(corners[0], corners[1], corners[2]);
            mesh.addTriangle(corners[0], corners[2], corners[3]);
        }
        return mesh;
    }

    /** One coordinate of the cell, pushed a hair outside it so the overlay shares no place with a block. */
    private static float pushed(float corner) {
        return corner <= 0.0f ? -OUTSIDE : 1.0f + OUTSIDE;
    }

    /** Uploads the triangles of one mesh, where they stay until the game closes. */
    private static Mesh upload(MeshData data) {
        Mesh mesh = new Mesh(true, data.vertexCount(), data.indexCount(), BlockShader.ATTRIBUTES);
        mesh.setVertices(data.vertexFloats(), 0, data.vertexCount() * MeshData.FLOATS_PER_VERTEX);
        mesh.setIndices(data.indexShorts(), 0, data.indexCount());
        return mesh;
    }

    @Override
    public void dispose() {
        for (int stage = 0; stage < stages.length; stage++) {
            if (stages[stage] != null) {
                stages[stage].dispose();
                stages[stage] = null;
            }
        }
    }

    @Override
    public String toString() {
        return "BreakOverlay(" + stageCount() + " of " + STAGES + " stages)";
    }
}
