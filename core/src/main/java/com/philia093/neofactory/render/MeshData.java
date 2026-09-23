package com.philia093.neofactory.render;

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
 */
public final class MeshData {

    /** Highest amount of vertices one mesh may hold, the limit of a sixteen bit index. */
    public static final int MAX_VERTICES = 65_535;

    /** Amount of floats of one vertex: position, texture coordinate, picture layer and colour. */
    public static final int FLOATS_PER_VERTEX = 10;

    /** Amount of floats one triangle covers. */
    public static final int FLOATS_PER_TRIANGLE = FLOATS_PER_VERTEX * 3;

    /** Offset of the red of a vertex inside its floats. */
    public static final int RED = 6;

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

        vertexCount++;
        return index;
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
