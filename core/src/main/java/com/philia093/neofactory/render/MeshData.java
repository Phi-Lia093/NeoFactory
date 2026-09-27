package com.philia093.neofactory.render;

import com.philia093.neofactory.block.Block;

/**
 * The triangles of one mesh, ready to be handed to a {@link com.badlogic.gdx.graphics.Mesh}.
 * <p>
 * A vertex holds the position of a corner of a block face, the texture coordinate inside the picture
 * of that face, the layer the picture lives in inside the texture array and a colour that carries what
 * the fixed shading of the face and the shadow its neighbours drop on it have left of the tint of the
 * block. The colour is packed into four bytes, so a vertex is twenty eight bytes long.
 * <p>
 * <b>The data is written as it grows.</b> A section holds four thousand and ninety six cells and can
 * show more corners than a mesh can address with sixteen bit indices, so a build that runs out of room
 * starts a further {@code MeshData} instead of failing: the mesher hands out a list of them, see
 * {@link SectionMesher#build(com.philia093.neofactory.world.Section, int, SectionMesher.Blocks,
 * SectionMesher.Pictures)}.
 * <p>
 * <b>An index addresses a vertex of this mesh</b>, which is what keeps a shared corner between two
 * faces from being written twice.
 * <p>
 * <b>A corner also carries the frames of its picture.</b> A picture that is a strip of frames lives in
 * as many layers of the array as it has frames, one below the other, see
 * {@link com.philia093.neofactory.render.BlockPictures}; the layer of a corner is the first frame of
 * that run and the count beside it is how many layers it may run to. A still picture names one frame
 * and never moves, and the shader picks the frame of the run out of the tick the world is in, see
 * {@link BlockShader#animationFrame(int)}. That is what keeps the casing of a machine still while the
 * gear on its top turns.
 */
public final class MeshData {

    /** Highest amount of vertices one mesh may hold, the limit of a sixteen bit index. */
    public static final int MAX_VERTICES = 65_535;

    /**
     * Amount of floats of one vertex: position, texture coordinate, picture layer, colour, frames and the
     * light of the cell the corner is seen from.
     */
    public static final int FLOATS_PER_VERTEX = 13;

    /** Amount of floats one triangle covers. */
    public static final int FLOATS_PER_TRIANGLE = FLOATS_PER_VERTEX * 3;

    /** Offset of the red of a vertex inside its floats. */
    public static final int RED = 6;

    /** Offset of the amount of frames of the picture a corner shows inside its floats. */
    public static final int FRAMES = 10;

    /**
     * Offset of the light a corner is drawn with inside its floats: the light of the sky first, the light of
     * the sources of the industry second, both a level of {@code 0} to {@link Block#MAX_LIGHT}.
     * <p>
     * The <b>level</b> is stored and not a brightness, because how bright a level is drawn is a question of
     * the moment - noon is brighter than midnight - and that decision is made in the shader, once per frame,
     * see {@link BlockShader#skyBrightness(float)}. A mesh does not have to be rebuilt when the sun moves.
     */
    public static final int LIGHT = 11;

    private final float[] vertices = new float[MAX_VERTICES * FLOATS_PER_VERTEX];

    private final short[] indices = new short[MAX_VERTICES * 3 / 2];

    private int vertexCount;
    private int indexCount;

    /** Amount of corners written so far. */
    public int vertexCount() {
        return vertexCount;
    }

    /** Amount of indices written so far, three per triangle. */
    public int indexCount() {
        return indexCount;
    }

    /** {@code true} while nothing was written, which is how an empty mesh is skipped. */
    public boolean isEmpty() {
        return indexCount == 0;
    }

    /** {@code true} when another vertex would not fit, so a new mesh has to be started. */
    public boolean isFull() {
        return vertexCount + 4 > MAX_VERTICES || indexCount + 6 > indices.length;
    }

    /**
     * Writes one corner of a face.
     *
     * @param x world X coordinate of the corner
     * @param y world Y coordinate of the corner, the height
     * @param z world Z coordinate of the corner
     * @param u texture coordinate across the picture
     * @param v texture coordinate up the picture
     * @param layer layer the picture lives in inside the texture array
     * @param red red of the colour the corner is drawn with
     * @param green green of the colour the corner is drawn with
     * @param blue blue of the colour the corner is drawn with
     * @return the index of the corner inside this mesh
     */
    public int addVertex(float x, float y, float z, float u, float v, float layer, float red,
            float green, float blue) {
        return addVertex(x, y, z, u, v, layer, red, green, blue, 1);
    }

    /**
     * Writes one corner of a face of an animated picture.
     * <p>
     * The layer is the first frame of the run the shader walks through, see {@link #FRAMES}. The corner is
     * written in the light of the sky at its brightest, which is what a mesh of the industry - an item, a
     * body - is drawn with, see {@link #addVertex(float, float, float, float, float, float, float, float,
     * float, int, int, int)}.
     *
     * @param x world X coordinate of the corner
     * @param y world Y coordinate of the corner, the height
     * @param z world Z coordinate of the corner
     * @param u texture coordinate across the picture
     * @param v texture coordinate up the picture
     * @param layer layer the first frame of the picture lives in inside the texture array
     * @param red red of the colour the corner is drawn with
     * @param green green of the colour the corner is drawn with
     * @param blue blue of the colour the corner is drawn with
     * @param frames amount of frames the picture holds, at least one
     * @return the index of the corner inside this mesh
     */
    public int addVertex(float x, float y, float z, float u, float v, float layer, float red,
            float green, float blue, int frames) {
        return addVertex(x, y, z, u, v, layer, red, green, blue, frames, Block.MAX_LIGHT, 0);
    }

    /**
     * Writes one corner of a face in the light of the cell it is seen from.
     *
     * @param x world X coordinate of the corner
     * @param y world Y coordinate of the corner, the height
     * @param z world Z coordinate of the corner
     * @param u texture coordinate across the picture
     * @param v texture coordinate up the picture
     * @param layer layer the first frame of the picture lives in inside the texture array
     * @param red red of the colour the corner is drawn with
     * @param green green of the colour the corner is drawn with
     * @param blue blue of the colour the corner is drawn with
     * @param frames amount of frames the picture holds, at least one
     * @param skyLight light of the sky of the cell the face looks into, {@code 0} to {@link Block#MAX_LIGHT}
     * @param blockLight light of the sources of that cell, {@code 0} to {@link Block#MAX_LIGHT}
     * @return the index of the corner inside this mesh
     */
    public int addVertex(float x, float y, float z, float u, float v, float layer, float red,
            float green, float blue, int frames, int skyLight, int blockLight) {
        int index = vertexCount;
        int base = index * FLOATS_PER_VERTEX;
        vertices[base] = x;
        vertices[base + 1] = y;
        vertices[base + 2] = z;
        vertices[base + 3] = u;
        vertices[base + 4] = v;
        vertices[base + 5] = layer;
        vertices[base + 6] = clamp(red);
        vertices[base + 7] = clamp(green);
        vertices[base + 8] = clamp(blue);
        vertices[base + 9] = 1.0f;
        vertices[base + FRAMES] = frames >= 1 ? frames : 1;
        vertices[base + LIGHT] = level(skyLight);
        vertices[base + LIGHT + 1] = level(blockLight);

        vertexCount++;
        return index;
    }

    /** One level of a light map as the number the shader reads, never outside the scale of the map. */
    private static float level(int light) {
        if (light < 0) {
            return 0;
        }
        return light > Block.MAX_LIGHT ? Block.MAX_LIGHT : light;
    }

    /**
     * Writes one triangle from three corners of this mesh.
     *
     * @param first index of the first corner
     * @param second index of the second corner
     * @param third index of the third corner
     */
    public void addTriangle(int first, int second, int third) {
        indices[indexCount] = (short) first;
        indices[indexCount + 1] = (short) second;
        indices[indexCount + 2] = (short) third;
        indexCount += 3;
    }

    /** The written corners, in the layout a vertex buffer expects. */
    public float[] vertexFloats() {
        return vertices;
    }

    /** The written indices, three per triangle. */
    public short[] indexShorts() {
        return indices;
    }

    /** A colour component, kept inside the range a colour has. */
    private static float clamp(float component) {
        return Math.min(1.0f, Math.max(0.0f, component));
    }

    @Override
    public String toString() {
        return "MeshData(" + vertexCount + " corners, " + (indexCount / 3) + " triangles)";
    }
}
